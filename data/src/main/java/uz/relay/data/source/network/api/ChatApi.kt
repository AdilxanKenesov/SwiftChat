package uz.relay.data.source.network.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import uz.relay.data.model.response.ChatListPageResponse
import uz.relay.data.model.response.ChatResponse

interface ChatApi {

    /**
     * Oxirgi faollik bo'yicha, eng yangisi birinchi. `cursor` — oldingi javobning `nextCursor`i;
     * `null` bo'lsa Retrofit uni so'rovga umuman qo'shmaydi (birinchi sahifa).
     */
    @GET("v1/chats")
    suspend fun getChats(
        @Query("limit") limit: Int = MAX_PAGE_SIZE,
        @Query("cursor") cursor: String? = null
    ): ChatListPageResponse

    @GET("v1/chats/{id}")
    suspend fun getChat(@Path("id") id: String): ChatResponse

    companion object {
        /** Server ruxsat bergan eng katta sahifa: kamroq so'rov — 300 so'rov/daqiqa limitini tejaydi. */
        const val MAX_PAGE_SIZE = 100
    }
}
