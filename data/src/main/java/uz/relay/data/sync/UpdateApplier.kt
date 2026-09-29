package uz.relay.data.sync

import androidx.room.withTransaction
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import uz.relay.core.common.result.AppResult
import uz.relay.core.common.result.isRetryable
import uz.relay.data.mapper.systemEventUserIds
import uz.relay.data.mapper.toEmbedded
import uz.relay.data.mapper.toEntity
import uz.relay.data.model.response.ChatPayload
import uz.relay.data.model.response.ChatResponse
import uz.relay.data.model.response.CursorPayload
import uz.relay.data.model.response.MemberPayload
import uz.relay.data.model.response.MessageDeletePayload
import uz.relay.data.model.response.MessageEditPayload
import uz.relay.data.model.response.MessageResponse
import uz.relay.data.model.response.UpdateKinds
import uz.relay.data.model.response.UpdateResponse
import uz.relay.data.realtime.ReceiptSender
import uz.relay.data.realtime.TypingTracker
import uz.relay.data.source.local.SessionStorage
import uz.relay.data.source.local.cache.UserCache
import uz.relay.data.source.local.database.RelayDatabase
import uz.relay.data.source.local.database.dao.ChatDao
import uz.relay.data.source.local.database.dao.MemberCursorDao
import uz.relay.data.source.local.database.dao.MessageDao
import uz.relay.data.source.local.database.dao.SyncStateDao
import uz.relay.data.source.local.database.dao.UserDao
import uz.relay.data.source.local.database.entity.LastMessageEmbedded
import uz.relay.data.source.local.database.entity.SendStatus
import uz.relay.data.source.network.api.ChatApi
import uz.relay.data.utils.safeApiCall
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** `kind` bo'yicha o'qilgan update. Noma'lum `kind` lar bu yerga umuman kirmaydi. */
private sealed interface Decoded {
    data class NewMessage(val message: MessageResponse) : Decoded
    data class Edit(val payload: MessageEditPayload) : Decoded
    data class Delete(val payload: MessageDeletePayload) : Decoded
    data class Read(val payload: CursorPayload) : Decoded
    data class Delivered(val payload: CursorPayload) : Decoded
    data class Member(val payload: MemberPayload) : Decoded
    data class ChatChanged(val payload: ChatPayload) : Decoded
}

/**
 * Update'larni lokal bazaga qo'llaydi. REST catch-up ham, keyinroq WebSocket'ning jonli frame'lari ham
 * shu yerdan o'tadi — mantiq bitta joyda.
 *
 * Asosiy qoida — IDEMPOTENTLIK: bitta update ikki marta kelsa ham natija bir xil bo'lishi kerak
 * (chaos mode dublikatlari, bootstrap bilan catch-up'ning ustma-ust tushishi):
 *  - xabarlar `clientMessageId` bo'yicha upsert qilinadi (dublikat bo'lmaydi);
 *  - tahrir faqat `editVersion` oshsa, o'chirish faqat hali o'chirilmagan bo'lsa qo'llanadi;
 *  - yangi xabar chatni faqat uning `topSeq`idan katta bo'lsa o'zgartiradi (o'qilmaganlar ikki marta sanalmaydi);
 *  - kursorlar max-wins (hech qachon orqaga ketmaydi).
 *
 * Ikki bosqich:
 *  1. Tarmoq (tranzaksiyadan TASHQARIDA): kerakli chat qatorlari va profillar yuklanadi. Tranzaksiya
 *     ichida tarmoqni kutish bazani uzoq qulflab, UI'ni qotirardi.
 *  2. Bitta tranzaksiya: hamma o'zgarishlar + kursor birga yoziladi. Ilova o'rtada o'ldirilsa, hech narsa
 *     yarim qolmaydi — yo hammasi qo'llangan, yo hech biri (va keyingi sync ularni qaytadan beradi).
 */
@Singleton
class UpdateApplier @Inject constructor(
    private val database: RelayDatabase,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val userDao: UserDao,
    private val memberCursorDao: MemberCursorDao,
    private val syncStateDao: SyncStateDao,
    private val chatApi: ChatApi,
    private val userCache: UserCache,
    private val sessionStorage: SessionStorage,
    private val json: Json,
    private val typingTracker: TypingTracker,
    private val receiptSender: ReceiptSender
) {
    /**
     * @param updates `updateSeq` bo'yicha o'sish tartibida va teshiksiz (buni [SyncEngine] kafolatlaydi).
     * @throws IOException kerakli chat qatorini vaqtincha yuklab bo'lmadi. Bunda kursor surilmaydi va
     * keyingi urinishda shu update'lar qaytadan olinadi — aks holda yangi chat butunlay yo'qolib qolardi.
     */
    suspend fun apply(updates: List<UpdateResponse>) {
        if (updates.isEmpty()) return
        val myUserId = sessionStorage.current()?.userId ?: return
        val decoded = updates.mapNotNull { decode(it) }

        // ---- 1. Tarmoq ----
        val chatsToFetch = buildSet {
            decoded.forEach { update ->
                when (update) {
                    // Yangi chat yoki nom/avatar o'zgardi — to'liq qator faqat GET /v1/chats/{id} da.
                    is Decoded.ChatChanged -> add(update.payload.chatId)
                    // Bazada yo'q chatga xabar keldi (masalan, kimdir menga birinchi marta yozdi).
                    is Decoded.NewMessage ->
                        if (chatDao.getChat(update.message.chatId) == null) add(update.message.chatId)
                    // Meni guruhga qo'shishdi.
                    is Decoded.Member -> {
                        val addedMe = update.payload.userId == myUserId && !update.payload.removed
                        if (addedMe && chatDao.getChat(update.payload.chatId) == null) add(update.payload.chatId)
                    }
                    else -> Unit
                }
            }
        }
        // 403/404 (chatdan chiqarilganmiz) — o'tkazib yuboramiz. Vaqtinchalik xato (internet, 5xx) esa
        // butun qo'llashni to'xtatadi: kursor oldinga ketib, chat qatori hech qachon kelmay qolmasin.
        val fetchedChats: List<ChatResponse> = chatsToFetch.mapNotNull { chatId ->
            when (val result = safeApiCall { chatApi.getChat(chatId) }) {
                is AppResult.Success -> result.data
                is AppResult.Error ->
                    if (result.error.isRetryable) throw IOException("Chat $chatId yuklanmadi") else null
            }
        }
        val users = userCache.fetchMissing(
            decoded.flatMap { it.referencedUserIds() } + fetchedChats.mapNotNull { it.peerUserId }
        )

        // ---- 2. Bitta tranzaksiya ----
        database.withTransaction {
            userDao.upsertAll(users)
            chatDao.upsertAll(fetchedChats.map { it.toEntity() })
            decoded.forEach { applyOne(it, myUserId) }
            // Kursor faqat oldinga: kechikkan chaqiruv uni orqaga surib yubormasin.
            val newCursor = maxOf(syncStateDao.getCursor() ?: 0, updates.last().updateSeq)
            syncStateDao.setCursor(newCursor)
        }

        // ---- 3. Tranzaksiyadan keyin: yon ta'sirlar (bazaga emas, tashqariga) ----
        val incoming = decoded.filterIsInstance<Decoded.NewMessage>()
            .map { it.message }
            .filter { it.senderId != myUserId }
        // Xabar yuborgan odam endi "yozmayapti" — 5 s kutmasdan belgini olib tashlaymiz.
        incoming.forEach { typingTracker.clear(it.chatId, it.senderId) }
        // Yetkazilish kvitansiyasi: yuboruvchi ✓✓ ko'radi. Har chat bo'yicha faqat eng katta seq yetarli (max-wins).
        incoming.groupBy { it.chatId }
            .mapValues { (_, messages) -> messages.maxOf { it.serverSeq } }
            .forEach { (chatId, upToSeq) -> receiptSender.received(chatId, upToSeq) }
    }

    private suspend fun applyOne(update: Decoded, myUserId: String) {
        when (update) {
            is Decoded.NewMessage -> applyNewMessage(update.message, myUserId)

            is Decoded.Edit -> with(update.payload) {
                messageDao.applyEdit(serverId, body, editVersion, editedAt)
                updateLastMessage(chatId, serverId) { it.copy(body = body) }
            }

            is Decoded.Delete -> with(update.payload) {
                messageDao.applyDelete(serverId, deletedAt)
                updateLastMessage(chatId, serverId) { it.copy(deletedAt = deletedAt) }
            }

            is Decoded.Read -> with(update.payload) {
                // O'zim boshqa qurilmamda o'qigan bo'lsam — bu yerda ham o'qilmaganlar belgisi yo'qolsin.
                if (userId == myUserId) chatDao.markRead(chatId, upToSeq)
                memberCursorDao.raise(chatId, userId, readUpToSeq = upToSeq)
            }

            is Decoded.Delivered -> with(update.payload) {
                memberCursorDao.raise(chatId, userId, deliveredUpToSeq = upToSeq)
            }

            is Decoded.Member -> with(update.payload) {
                // Meni chiqarishdi yoki o'zim chiqdim: server bu chat haqida boshqa update yubormaydi,
                // shuning uchun lokal nusxani ham o'chiramiz. A'zolar ro'yxati guruh bosqichida qo'shiladi.
                if (userId == myUserId && removed) {
                    chatDao.delete(chatId)
                    messageDao.deleteByChat(chatId)
                    memberCursorDao.deleteByChat(chatId)
                }
            }

            // Chat qatori 1-bosqichda yuklab olingan va yuqorida yozilgan.
            is Decoded.ChatChanged -> Unit
        }
    }

    private suspend fun applyNewMessage(message: MessageResponse, myUserId: String) {
        val existing = messageDao.get(message.clientMessageId)
        // Server bergan va keyin tahrirlangan/o'chirilgan xabarni eski nusxa bilan bosib ketmaymiz
        // (catch-up bootstrap'dan keyin eski message_new'larni qayta berishi mumkin).
        val keepExisting = existing != null &&
            existing.status == SendStatus.SENT &&
            (existing.editVersion > message.editVersion || (existing.deletedAt != null && message.deletedAt == null))
        if (!keepExisting) {
            // O'z xabarimning "echo"si ack'dan oldin kelsa ham to'g'ri: PENDING qator shu yerda SENT bo'ladi.
            messageDao.upsertAll(listOf(message.toEntity()))
        }

        val chat = chatDao.getChat(message.chatId) ?: return
        // topSeq'dan eski yoki teng xabar — dublikat yoki snapshot uni allaqachon hisobga olgan.
        if (message.serverSeq <= chat.topSeq) return

        val isIncomingUnread = message.senderId != myUserId && message.serverSeq > chat.readUpToSeq
        chatDao.upsert(
            chat.copy(
                topSeq = message.serverSeq,
                lastActivityAt = maxOf(chat.lastActivityAt, message.createdAt),
                lastMessage = message.toEmbedded(),
                unreadCount = if (isIncomingUnread) chat.unreadCount + 1 else chat.unreadCount
            )
        )
    }

    /** Ro'yxatdagi "oxirgi xabar" aynan shu xabar bo'lsa — preview'ni ham yangilaymiz. */
    private suspend fun updateLastMessage(
        chatId: String,
        serverId: Long,
        change: (LastMessageEmbedded) -> LastMessageEmbedded
    ) {
        val chat = chatDao.getChat(chatId) ?: return
        val last = chat.lastMessage ?: return
        if (last.serverId == serverId) chatDao.upsert(chat.copy(lastMessage = change(last)))
    }

    private fun decode(update: UpdateResponse): Decoded? = try {
        when (update.kind) {
            UpdateKinds.MESSAGE_NEW -> Decoded.NewMessage(json.decodeFromJsonElement(MessageResponse.serializer(), update.payload))
            UpdateKinds.MESSAGE_EDIT -> Decoded.Edit(json.decodeFromJsonElement(MessageEditPayload.serializer(), update.payload))
            UpdateKinds.MESSAGE_DELETE -> Decoded.Delete(json.decodeFromJsonElement(MessageDeletePayload.serializer(), update.payload))
            UpdateKinds.READ -> Decoded.Read(json.decodeFromJsonElement(CursorPayload.serializer(), update.payload))
            UpdateKinds.DELIVERED -> Decoded.Delivered(json.decodeFromJsonElement(CursorPayload.serializer(), update.payload))
            UpdateKinds.MEMBER -> Decoded.Member(json.decodeFromJsonElement(MemberPayload.serializer(), update.payload))
            UpdateKinds.CHAT -> Decoded.ChatChanged(json.decodeFromJsonElement(ChatPayload.serializer(), update.payload))
            // Kelajakdagi yangi `kind` — o'tkazib yuboramiz, lekin kursor baribir oshadi (teshik qolmaydi).
            else -> null
        }
    } catch (e: SerializationException) {
        // Bitta buzuq payload butun oqimni to'xtatib qo'ymasin.
        null
    } catch (e: IllegalArgumentException) {
        null
    }

    /** Ro'yxatda ism ko'rsatish uchun kimlarning profili kerak ("Malika: ...", SYSTEM matnlari). */
    private fun Decoded.referencedUserIds(): List<String> = when (this) {
        is Decoded.NewMessage -> listOf(message.senderId) + systemEventUserIds(message.body, json)
        is Decoded.Member -> listOf(payload.userId, payload.actorId)
        else -> emptyList()
    }
}
