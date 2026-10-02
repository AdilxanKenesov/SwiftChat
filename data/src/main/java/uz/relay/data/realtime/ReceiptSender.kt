package uz.relay.data.realtime

import uz.relay.core.common.result.AppResult
import uz.relay.data.model.request.UpToSeqRequest
import uz.relay.data.source.network.api.MessageApi
import uz.relay.data.source.network.realtime.ClientFrame
import uz.relay.data.source.network.realtime.RealtimeClient
import uz.relay.data.utils.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `read` va `received` kvitansiyalari: socket ulangan bo'lsa frame (tezroq, HTTP yuki yo'q), aks holda REST.
 *
 * Nega javob kutilmaydi: ikkalasi ham max-wins va "clamped" — qiymat yo'qolsa, keyingi (kattaroq yoki teng)
 * qiymat uni to'liq qoplaydi. Takroriy yoki eski qiymat esa serverda shunchaki no-op.
 *
 * Chaqiruvchilar: [uz.relay.data.sync.UpdateApplier] (yangi xabar kelganda `received`) va chat ekrani
 * repository orqali (xabarlar ko'rilganda `read`).
 */
@Singleton
class ReceiptSender @Inject constructor(
    private val realtimeClient: RealtimeClient,
    private val messageApi: MessageApi
) {
    /** "O'qildi": chatdagi [upToSeq] gacha bo'lgan xabarlar ko'rildi (yuboruvchida ko'k ✓✓). */
    suspend fun read(chatId: String, upToSeq: Long): AppResult<Unit> =
        if (realtimeClient.send(ClientFrame.Read(chatId, upToSeq))) {
            AppResult.Success(Unit)
        } else {
            safeApiCall { messageApi.markRead(chatId, UpToSeqRequest(upToSeq)) }
        }

    /** Yuboruvchi ✓✓ (yetkazildi) ko'rishi uchun: qurilmam shu serverSeq gacha xabarlarni oldi. */
    suspend fun received(chatId: String, upToSeq: Long): AppResult<Unit> =
        if (realtimeClient.send(ClientFrame.Received(chatId, upToSeq))) {
            AppResult.Success(Unit)
        } else {
            safeApiCall { messageApi.markReceived(chatId, UpToSeqRequest(upToSeq)) }
        }
}
