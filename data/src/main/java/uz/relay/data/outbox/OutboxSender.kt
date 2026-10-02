package uz.relay.data.outbox

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import uz.relay.core.common.result.AppError
import uz.relay.core.common.result.AppResult
import uz.relay.core.common.result.isRetryable
import uz.relay.data.mapper.toSendRequest
import uz.relay.data.media.MediaUploader
import uz.relay.data.model.response.SendMessageResultResponse
import uz.relay.data.source.local.database.dao.MessageDao
import uz.relay.data.source.local.database.dao.UploadDao
import uz.relay.data.source.local.database.entity.MessageEntity
import uz.relay.data.source.network.api.MessageApi
import uz.relay.data.source.network.realtime.ClientFrame
import uz.relay.data.source.network.realtime.RealtimeClient
import uz.relay.data.source.network.realtime.RealtimeState
import uz.relay.data.source.network.realtime.ServerFrame
import uz.relay.data.utils.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

/** [OutboxSender.flush] natijasi — [OutboxWorker] uni WorkManager'ning success/retry'iga aylantiradi. */
enum class FlushResult {
    /** Kutayotgan xabar qolmadi. */
    DONE,

    /** Vaqtinchalik xato (internet, 5xx, 429) — keyinroq qayta urinish kerak. */
    RETRY_LATER
}

/**
 * Outbox'dagi (PENDING) xabarlarni yuboradi.
 *
 * Outbox g'oyasi: foydalanuvchi "Yuborish"ni bosganda xabar avval lokal bazaga PENDING holatida yoziladi
 * va darhol chatda ko'rinadi (optimistik UI). Tarmoqqa jo'natish esa alohida — shu klass orqali, fonda.
 * Shuning uchun internet yo'q bo'lsa ham xabar yo'qolmaydi, ilova qayta ochilganda ham navbatda turadi.
 * Chaqiruvchilar: [OutboxWorker] (WorkManager) — [OutboxScheduler] orqali ishga tushiriladi.
 *
 * Navbat qat'iy tartibda (eng eskisidan) yuboriladi: vaqtinchalik xatoda to'xtaymiz, doimiy xatoda esa
 * xabarni FAILED qilib keyingisiga o'tamiz (foydalanuvchi uni qo'lda qayta yuborishi mumkin).
 * Media xabarda avval fayl [MediaUploader] bilan yuklanadi, keyin xabar `mediaIds` bilan jo'natiladi.
 *
 * Yo'l tanlash: socket ulangan bo'lsa — `send` frame (tezroq, HTTP yuki yo'q), aks holda REST. Ikkalasi
 * serverda bitta servis va bitta idempotentlik kaliti (`clientMessageId`): shuning uchun bitta xabarni ikki
 * yo'l bilan yuborish ham xavfsiz — server ikkinchisiga asl natijani qaytaradi, dublikat yaratmaydi.
 */
@Singleton
class OutboxSender @Inject constructor(
    private val messageDao: MessageDao,
    private val uploadDao: UploadDao,
    private val mediaUploader: MediaUploader,
    private val messageApi: MessageApi,
    private val realtimeClient: RealtimeClient
) {
    /** Ikki flush parallel ketsa, bitta xabar ikki marta yuborilardi (zararsiz, lekin befoyda). */
    private val mutex = Mutex()

    /** Navbat bo'shaguncha yoki vaqtinchalik xatoga uchraguncha PENDING xabarlarni ketma-ket yuboradi. */
    suspend fun flush(): FlushResult = mutex.withLock { flushLocked() }

    /** [flush]ning asosiy tsikli; Mutex allaqachon olingan. */
    private suspend fun flushLocked(): FlushResult {
        while (true) {
            val message = messageDao.nextPending() ?: return FlushResult.DONE

            // Media xabar: avval fayl (to'xtagan joyidan), keyin xabarning o'zi `mediaIds` bilan.
            // Navbat tartibi saqlanadi — katta video yuklanayotganda undan keyingi matn ham kutadi.
            val mediaIds = uploadDao.get(message.clientMessageId)?.let { upload ->
                when (val uploaded = mediaUploader.upload(upload)) {
                    is AppResult.Success -> listOf(uploaded.data)
                    is AppResult.Error -> {
                        // Foydalanuvchi bekor qildi — xabar allaqachon o'chirilgan, keyingisiga o'tamiz.
                        if (messageDao.get(message.clientMessageId) == null) continue
                        if (uploaded.error.isRetryable) return FlushResult.RETRY_LATER
                        // Doimiy xato (masalan, fayl juda katta yoki taqiqlangan tur) — qayta urinish befoyda.
                        messageDao.markFailed(message.clientMessageId, uploaded.error.code())
                        continue
                    }
                }
            }

            // Avval socket; u natija bermasa (`null`) — xuddi shu clientMessageId bilan REST.
            when (val result = sendViaSocket(message, mediaIds) ?: sendViaRest(message, mediaIds)) {
                // PENDING qator server bergan id/seq bilan SENT bo'ladi (echo oldinroq kelgan bo'lsa ham zararsiz).
                is AppResult.Success -> messageDao.markSent(
                    clientMessageId = message.clientMessageId,
                    serverId = result.data.serverId,
                    serverSeq = result.data.serverSeq,
                    serverCreatedAt = result.data.serverCreatedAt
                )

                is AppResult.Error -> {
                    // Vaqtinchalik xatoda to'xtaymiz va keyingilarini ham yubormaymiz: aks holda keyingi xabar
                    // oldingisidan oldin yetib borib, chatdagi tartib buziladi.
                    if (result.error.isRetryable) return FlushResult.RETRY_LATER
                    messageDao.markFailed(message.clientMessageId, result.error.code())
                }
            }
        }
    }

    /**
     * `null` — socket orqali natija olinmadi (ulanmagan yoki ack kelmadi); chaqiruvchi REST'ga o'tadi.
     *
     * Ack yo'qolishi mumkin (uzilish, chaos mode) — shuning uchun kutish vaqti cheklangan. Keyin xuddi shu
     * clientMessageId REST'da yuboriladi: xabar allaqachon saqlangan bo'lsa server asl natijani qaytaradi.
     */
    private suspend fun sendViaSocket(message: MessageEntity, mediaIds: List<String>?): AppResult<SendMessageResultResponse>? {
        if (realtimeClient.state.value != RealtimeState.CONNECTED) return null

        return coroutineScope {
            // Javobga OLDIN obuna bo'lamiz, keyin yuboramiz: ack juda tez kelsa ham o'tkazib yubormaslik uchun.
            // UNDISPATCHED — async birinchi suspend nuqtasigacha (obunagacha) darhol bajariladi.
            val reply = async(start = CoroutineStart.UNDISPATCHED) {
                realtimeClient.frames.first { frame ->
                    (frame is ServerFrame.Ack && frame.clientMessageId == message.clientMessageId) ||
                        (frame is ServerFrame.Nack && frame.clientMessageId == message.clientMessageId)
                }
            }

            val sent = realtimeClient.send(
                ClientFrame.Send(
                    clientMessageId = message.clientMessageId,
                    chatId = message.chatId,
                    messageType = message.type,
                    body = message.body,
                    mediaIds = mediaIds,
                    replyTo = message.replyToClientMessageId
                )
            )
            // Frame socket'ga yozilmadi (shu lahzada uzilgan) — obunani bekor qilib REST'ga o'tamiz.
            if (!sent) {
                reply.cancel()
                return@coroutineScope null
            }

            when (val frame = withTimeoutOrNull(ACK_TIMEOUT_MS) { reply.await() }) {
                is ServerFrame.Ack -> AppResult.Success(
                    SendMessageResultResponse(frame.clientMessageId, frame.serverId, frame.serverSeq, frame.serverCreatedAt)
                )
                // nack REST xatosi bilan bir xil kodlar va `retryable` bayrog'i bilan keladi.
                is ServerFrame.Nack -> AppResult.Error(
                    AppError.Api(httpStatus = 0, code = frame.code, message = frame.message, retryable = frame.retryable)
                )
                // Timeout: ack kelmadi — REST'ga o'tamiz (idempotentlik dublikatdan saqlaydi).
                else -> {
                    reply.cancel()
                    null
                }
            }
        }
    }

    /** 201 — yangi, 200 — replay (javob avval yo'qolgan edi). Ikkalasida ham natija bir xil. */
    private suspend fun sendViaRest(message: MessageEntity, mediaIds: List<String>?): AppResult<SendMessageResultResponse> =
        safeApiCall { messageApi.sendMessage(message.chatId, message.toSendRequest(mediaIds)) }

    /** FAILED xabarga yoziladigan xato kodi (UI sababni ko'rsatishi uchun). */
    private fun AppError.code(): String = when (this) {
        is AppError.Api -> code
        AppError.Network -> "NETWORK"
        is AppError.Unknown -> "UNKNOWN"
    }

    private companion object {
        /** Socket orqali ack'ni kutish chegarasi; o'tsa REST'ga o'tiladi. */
        const val ACK_TIMEOUT_MS = 10_000L
    }
}
