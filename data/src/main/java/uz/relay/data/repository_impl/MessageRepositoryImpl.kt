package uz.relay.data.repository_impl

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CancellationException
import uz.relay.core.common.result.AppError
import uz.relay.core.common.result.AppResult
import uz.relay.core.common.result.ErrorCodes
import uz.relay.core.common.result.map
import uz.relay.data.mapper.PeerCursors
import uz.relay.data.mapper.systemEventUserIds
import uz.relay.data.mapper.toDomain
import uz.relay.data.mapper.toEntity
import uz.relay.data.model.request.EditMessageRequest
import uz.relay.data.model.response.MessagePageResponse
import uz.relay.data.media.MediaPreparer
import uz.relay.data.media.MediaTooLargeException
import uz.relay.data.outbox.OutboxScheduler
import uz.relay.data.realtime.ReceiptSender
import uz.relay.data.source.local.SessionStorage
import uz.relay.data.source.local.cache.UserCache
import uz.relay.data.source.local.database.RelayDatabase
import uz.relay.data.source.local.database.dao.ChatDao
import uz.relay.data.source.local.database.dao.MemberCursorDao
import uz.relay.data.source.local.database.dao.MessageDao
import uz.relay.data.source.local.database.dao.UploadDao
import uz.relay.data.source.local.database.dao.UserDao
import uz.relay.data.source.local.database.entity.MessageEntity
import uz.relay.data.source.local.database.entity.SendStatus
import uz.relay.data.source.local.database.entity.UploadEntity
import uz.relay.data.source.network.api.MessageApi
import uz.relay.data.source.network.realtime.ClientFrame
import uz.relay.data.source.network.realtime.RealtimeClient
import uz.relay.data.utils.safeApiCall
import uz.relay.domain.model.Attachment
import uz.relay.domain.model.Message
import uz.relay.domain.repository.MessageRepository
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

internal class MessageRepositoryImpl @Inject constructor(
    private val database: RelayDatabase,
    private val messageDao: MessageDao,
    private val chatDao: ChatDao,
    private val userDao: UserDao,
    private val memberCursorDao: MemberCursorDao,
    private val uploadDao: UploadDao,
    private val mediaPreparer: MediaPreparer,
    private val messageApi: MessageApi,
    private val sessionStorage: SessionStorage,
    private val userCache: UserCache,
    private val outboxScheduler: OutboxScheduler,
    private val receiptSender: ReceiptSender,
    private val realtimeClient: RealtimeClient,
    private val json: Json
) : MessageRepository {

    override fun observeMessages(chatId: String): Flow<List<Message>> = combine(
        messageDao.observeMessages(chatId),
        memberCursorDao.observe(chatId),
        uploadDao.observeByChat(chatId),
        sessionStorage.session.map { it?.userId }.distinctUntilChanged()
    ) { entities, cursors, uploads, myUserId ->
        // O'zim yuborgan fayllar: lokal nusxa va yuklash progressi (har bo'lakdan keyin yangilanadi).
        val uploadsById = uploads.associateBy { it.clientMessageId }
        // Boshqa a'zolarning eng katta kursorlari: guruhda kamida bittasi o'qigan bo'lsa ✓✓.
        val others = cursors.filter { it.userId != myUserId }
        val peers = PeerCursors(
            readUpToSeq = others.maxOfOrNull { it.readUpToSeq } ?: 0,
            deliveredUpToSeq = others.maxOfOrNull { it.deliveredUpToSeq } ?: 0
        )
        entities.map { it.toDomain(myUserId, peers, json, uploadsById[it.clientMessageId]) }
    }

    override suspend fun loadLatest(chatId: String): AppResult<Boolean> = loadPage(chatId, beforeSeq = null)

    override suspend fun loadOlder(chatId: String): AppResult<Boolean> {
        val oldest = messageDao.minSeq(chatId) ?: return loadLatest(chatId)
        return loadPage(chatId, beforeSeq = oldest)
    }

    private suspend fun loadPage(chatId: String, beforeSeq: Long?): AppResult<Boolean> {
        val result = safeApiCall { messageApi.getMessages(chatId, beforeSeq = beforeSeq) }
        if (result is AppResult.Error) return result
        val page = (result as AppResult.Success).data

        // Ismlar (yuboruvchi + SYSTEM xabardagi ishtirokchilar) — bubble'lar va tizim xabarlari uchun.
        val users = userCache.fetchMissing(
            page.messages.flatMap { message ->
                listOf(message.senderId) + systemEventUserIds(message.body.takeIf { message.type == "SYSTEM" }, json)
            }
        )

        database.withTransaction {
            if (beforeSeq == null && hasGapBefore(page, chatId)) messageDao.deleteSynced(chatId)
            userDao.upsertAll(users)
            messageDao.upsertAll(page.messages.map { it.toEntity() })
        }
        return AppResult.Success(page.hasMore)
    }

    /**
     * Eng yangi sahifa lokal tarix bilan ulanmasa (orada serverSeq "teshigi" bo'lsa), eski lokal xabarlarni
     * tashlaymiz. Aks holda ekranda 1..50 va 151..200 yonma-yon, orasidagi 100 ta xabar yo'qligi sezilmay
     * ko'rinardi, "eskilarini yuklash" esa noto'g'ri joydan boshlanardi. Tashlanganlar yuqoriga scroll
     * qilinganda serverdan qayta yuklanadi.
     */
    private suspend fun hasGapBefore(page: MessagePageResponse, chatId: String): Boolean {
        val localMax = messageDao.maxSeq(chatId) ?: return false
        val pageMin = page.messages.minOfOrNull { it.serverSeq } ?: return false
        return pageMin > localMax + 1
    }

    override suspend fun sendText(chatId: String, text: String, replyToClientMessageId: String?) {
        val myUserId = sessionStorage.current()?.userId ?: return
        messageDao.insert(
            MessageEntity(
                // Idempotentlik kaliti shu yerda, serverga yuborishdan OLDIN yaratiladi: qayta urinishlarda ham
                // aynan shu UUID yuboriladi va server dublikat yaratmaydi.
                clientMessageId = UUID.randomUUID().toString(),
                chatId = chatId,
                senderId = myUserId,
                serverId = null,
                serverSeq = null,
                type = "TEXT",
                body = text,
                replyToClientMessageId = replyToClientMessageId,
                createdAt = System.currentTimeMillis(),
                editedAt = null,
                editVersion = 0,
                deletedAt = null,
                status = SendStatus.PENDING,
                sendError = null
            )
        )
        outboxScheduler.schedule()
    }

    override suspend fun sendMedia(
        chatId: String,
        attachment: Attachment,
        caption: String?,
        replyToClientMessageId: String?
    ): AppResult<Unit> {
        val myUserId = sessionStorage.current()?.userId ?: return AppResult.Success(Unit)
        val clientMessageId = UUID.randomUUID().toString()

        // Katta videoni nusxalash va hash'lash bir necha soniya olishi mumkin — shuning uchun xabar bazaga
        // tayyorlash TUGAGANDAN keyin yoziladi: ekranda "yarim tayyor" xabar ko'rinmaydi.
        val prepared = try {
            mediaPreparer.prepare(attachment.uri, attachment.asFile, clientMessageId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: MediaTooLargeException) {
            return AppResult.Error(AppError.Api(413, ErrorCodes.PAYLOAD_TOO_LARGE, "File is larger than 100 MB", retryable = false))
        } catch (e: IOException) {
            return AppResult.Error(AppError.Unknown(e))
        } catch (e: SecurityException) {
            return AppResult.Error(AppError.Unknown(e))
        }

        database.withTransaction {
            messageDao.insert(
                MessageEntity(
                    clientMessageId = clientMessageId,
                    chatId = chatId,
                    senderId = myUserId,
                    serverId = null,
                    serverSeq = null,
                    type = prepared.kind,
                    // Server fayl nomini saqlamaydi — FILE xabarida nom `body`da boradi (boshqa klientlar ham shunday).
                    body = if (prepared.kind == "FILE") prepared.displayName else caption,
                    replyToClientMessageId = replyToClientMessageId,
                    createdAt = System.currentTimeMillis(),
                    editedAt = null,
                    editVersion = 0,
                    deletedAt = null,
                    status = SendStatus.PENDING,
                    sendError = null
                )
            )
            uploadDao.insert(
                UploadEntity(
                    clientMessageId = clientMessageId,
                    chatId = chatId,
                    localPath = prepared.localPath,
                    posterPath = prepared.posterPath,
                    kind = prepared.kind,
                    mimeType = prepared.mimeType,
                    sizeBytes = prepared.sizeBytes,
                    sha256 = prepared.sha256,
                    width = prepared.width,
                    height = prepared.height,
                    durationMs = prepared.durationMs,
                    thumbBase64 = prepared.thumbBase64,
                    uploadId = null,
                    mediaId = null,
                    chunkSize = DEFAULT_CHUNK_SIZE,
                    confirmedBytes = 0,
                    completed = false
                )
            )
        }
        outboxScheduler.schedule()
        return AppResult.Success(Unit)
    }

    /**
     * Faqat serverga hali yetmagan xabarni bekor qilsa bo'ladi. Yuklash ketayotgan bo'lsa, u keyingi bo'lakdan
     * oldin qator yo'qligini ko'radi va to'xtaydi. Serverdagi yarim sessiya o'z muddatida o'chadi.
     */
    override suspend fun cancelUpload(clientMessageId: String) {
        val message = messageDao.get(clientMessageId) ?: return
        if (message.status == SendStatus.SENT) return
        val upload = uploadDao.get(clientMessageId)
        database.withTransaction {
            uploadDao.delete(clientMessageId)
            messageDao.delete(clientMessageId)
        }
        upload?.let {
            File(it.localPath).delete()
            it.posterPath?.let { poster -> File(poster).delete() }
        }
    }

    override suspend fun retry(clientMessageId: String) {
        messageDao.markPendingAgain(clientMessageId)
        outboxScheduler.schedule()
    }

    /**
     * Tahrir va o'chirish outbox'ga qo'yilmaydi (to'g'ridan-to'g'ri so'rov): ular faqat serverdagi xabarga
     * tegishli va natijani darhol bilish kerak (masalan, 48 soat o'tib ketgan bo'lsa). Muvaffaqiyatda bazani
     * darhol yangilaymiz; keyin keladigan `message_edit` update'i `editVersion` max-wins tufayli hech narsani buzmaydi.
     */
    override suspend fun edit(serverId: Long, text: String): AppResult<Unit> =
        safeApiCall { messageApi.editMessage(serverId, EditMessageRequest(text)) }
            .map { edited ->
                messageDao.applyEdit(
                    serverId = serverId,
                    body = edited.body.orEmpty(),
                    editVersion = edited.editVersion,
                    editedAt = edited.editedAt ?: System.currentTimeMillis()
                )
            }

    override suspend fun delete(serverId: Long): AppResult<Unit> =
        safeApiCall { messageApi.deleteMessage(serverId) }
            // Server 204 qaytaradi (vaqtsiz); aniq `deletedAt` keyin `message_delete` update'ida keladi.
            .map { messageDao.applyDelete(serverId, System.currentTimeMillis()) }

    override fun sendTyping(chatId: String) {
        // Server bitta foydalanuvchi/chat uchun 3 s da bittadan ortig'ini o'zi tashlaydi — klientda qo'shimcha
        // cheklov kerak emas (spec). Socket bo'lmasa signal shunchaki yuborilmaydi: u saqlanmaydigan hodisa.
        realtimeClient.send(ClientFrame.Typing(chatId))
    }

    /**
     * Avval lokal (o'qilmaganlar belgisi darhol yo'qoladi), keyin server (socket yoki REST). Server so'rovi
     * muvaffaqiyatsiz bo'lsa ham xavfsiz: `read` max-wins, keyingi safar yana yuboriladi.
     */
    override suspend fun markRead(chatId: String): AppResult<Unit> {
        val upToSeq = messageDao.maxSeq(chatId) ?: return AppResult.Success(Unit)
        chatDao.markRead(chatId, upToSeq)
        return receiptSender.read(chatId, upToSeq)
    }

    override suspend fun search(chatId: String, query: String): List<Message> {
        val myUserId = sessionStorage.current()?.userId
        // Qidiruv natijasida ✓ belgilari kerak emas — kursorlar hisoblanmaydi.
        return messageDao.search(chatId, query).map { it.toDomain(myUserId, PeerCursors(), json) }
    }

    private companion object {
        /** Server javobidagi haqiqiy qiymat sessiya ochilganda yoziladi; bu faqat boshlang'ich. */
        const val DEFAULT_CHUNK_SIZE = 512 * 1024
    }
}
