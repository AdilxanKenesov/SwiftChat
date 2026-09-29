package uz.relay.data.source.network.api

import retrofit2.http.POST

/**
 * Joriy sessiyaga oid, lekin access token talab qiladigan endpoint'lar. [AuthApi] dan alohida, chunki u
 * token'siz (public) klient bilan ishlaydi, bular esa authorized klient bilan: token eskirgan bo'lsa
 * TokenAuthenticator uni avval yangilaydi.
 */
interface SessionApi {

    /**
     * Shu qurilmaning refresh token'ini bekor qiladi va uning WebSocket'ini 4003 bilan yopadi. Idempotent:
     * ikkinchi chaqiruv ham 204. Access token o'z muddati (≈15 daqiqa) tugaguncha ishlashda davom etadi.
     */
    @POST("v1/auth/logout")
    suspend fun logout()
}
