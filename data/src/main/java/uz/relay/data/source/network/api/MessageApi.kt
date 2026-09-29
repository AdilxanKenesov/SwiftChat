package uz.relay.data.source.network.api

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import uz.relay.data.model.request.UpToSeqRequest

interface MessageApi {

    /** O'qish kursorimni ko'taradi (max-wins, chat oxiridan oshsa server o'zi kesadi). Javob: 204. */
    @POST("v1/chats/{id}/read")
    suspend fun markRead(@Path("id") chatId: String, @Body request: UpToSeqRequest)

    /** Yetkazilish kvitansiyasi — socket yo'q paytdagi zaxira yo'l. Javob: 204. */
    @POST("v1/chats/{id}/received")
    suspend fun markReceived(@Path("id") chatId: String, @Body request: UpToSeqRequest)
}
