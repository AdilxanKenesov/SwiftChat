package uz.relay.data.source.network.api

import retrofit2.http.POST
import retrofit2.http.Url
import uz.relay.data.model.response.StreamTokenResponse

/**
 * Stream Video token serveri (Cloudflare Worker). Relay serveri emas — manzil to'liq ([Url]) beriladi.
 * Authorized client orqali: Relay access token sarlavhaga o'zi qo'shiladi, 401 kelsa yangilanib qayta yuboriladi.
 */
interface StreamTokenApi {

    @POST
    suspend fun token(@Url url: String): StreamTokenResponse
}
