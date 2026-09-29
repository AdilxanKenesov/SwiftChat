package uz.relay.data.source.network.api

import retrofit2.http.GET
import retrofit2.http.Query
import uz.relay.data.model.response.UpdatesPageResponse
import uz.relay.data.model.response.UpdatesStateResponse

interface SyncApi {

    /** Foydalanuvchining joriy `updateSeq`i. Bootstrap'da chatlar ro'yxatidan OLDIN chaqiriladi. */
    @GET("v1/updates/state")
    suspend fun getState(): UpdatesStateResponse

    /** `since` dan qat'iy keyingi hodisalar: o'sish tartibida va teshiksiz. */
    @GET("v1/updates")
    suspend fun getUpdates(
        @Query("since") since: Long,
        @Query("limit") limit: Int = MAX_PAGE_SIZE
    ): UpdatesPageResponse

    companion object {
        const val MAX_PAGE_SIZE = 500
    }
}
