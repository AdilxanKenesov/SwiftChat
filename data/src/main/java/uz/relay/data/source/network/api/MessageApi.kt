package uz.relay.data.source.network.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import uz.relay.data.model.request.EditMessageRequest
import uz.relay.data.model.request.SendMessageRequest
import uz.relay.data.model.request.UpToSeqRequest
import uz.relay.data.model.response.MessagePageResponse
import uz.relay.data.model.response.MessageResponse
import uz.relay.data.model.response.SendMessageResultResponse

/**
 * Xabarlar bo'yicha REST endpoint'lar: tarix sahifalari, yuborish, tahrir, o'chirish va kvitansiyalar.
 * Yuborish idempotent — `clientMessageId` kaliti bilan, shuning uchun outbox xavfsiz qayta urina oladi.
 * Kim ishlatadi: MessageRepositoryImpl, OutboxSender, ReceiptSender.
 */
interface MessageApi {

    /**
     * Chat tarixi, eng yangisi birinchi. `beforeSeq` — shu serverSeq'dan eskilarini ber
     * (`null` bo'lsa Retrofit uni so'rovga qo'shmaydi — eng yangi sahifa).
     */
    @GET("v1/chats/{id}/messages")
    suspend fun getMessages(
        @Path("id") chatId: String,
        @Query("beforeSeq") beforeSeq: Long? = null,
        @Query("limit") limit: Int = PAGE_SIZE
    ): MessagePageResponse

    /** 201 — yangi xabar, 200 — shu clientMessageId avval yuborilgan (asl natija qaytadi). Ikkalasi ham muvaffaqiyat. */
    @POST("v1/chats/{id}/messages")
    suspend fun sendMessage(@Path("id") chatId: String, @Body request: SendMessageRequest): SendMessageResultResponse

    /** Faqat yuboruvchi, createdAt'dan 48 soat ichida (keyin 400 EDIT_WINDOW_EXPIRED). */
    @PATCH("v1/messages/{serverId}")
    suspend fun editMessage(@Path("serverId") serverId: Long, @Body request: EditMessageRequest): MessageResponse

    /** Tombstone: qator o'chmaydi, faqat `deletedAt` qo'yiladi. Javob: 204. */
    @DELETE("v1/messages/{serverId}")
    suspend fun deleteMessage(@Path("serverId") serverId: Long)

    /** O'qish kursorimni ko'taradi (max-wins, chat oxiridan oshsa server o'zi kesadi). Javob: 204. */
    @POST("v1/chats/{id}/read")
    suspend fun markRead(@Path("id") chatId: String, @Body request: UpToSeqRequest)

    /** Yetkazilish kvitansiyasi — socket yo'q paytdagi zaxira yo'l. Javob: 204. */
    @POST("v1/chats/{id}/received")
    suspend fun markReceived(@Path("id") chatId: String, @Body request: UpToSeqRequest)

    companion object {
        /** Tarix sahifasi hajmi — bir ekranni to'ldirishga yetarli, lekin trafikni tejaydi. */
        const val PAGE_SIZE = 50
    }
}
