package uz.relay.data.source.network.api

import retrofit2.http.GET
import retrofit2.http.Query
import uz.relay.data.model.response.UpdatesPageResponse
import uz.relay.data.model.response.UpdatesStateResponse

/**
 * Update oqimi (updateSeq kursori) endpoint'lari. Offline paytda o'tkazib yuborilgan hodisalar shu yerdan
 * tartib bilan olinadi — WebSocket faqat "jonli" qism, bu esa ishonchli zaxira yo'l.
 * Kim ishlatadi: SyncEngine.
 */
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
        /** Server ruxsat bergan eng katta sahifa — uzoq offline'dan keyin kamroq so'rov bilan yetib olish uchun. */
        const val MAX_PAGE_SIZE = 500
    }
}
