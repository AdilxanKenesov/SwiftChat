package uz.relay.data.source.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import uz.relay.data.source.local.SessionStorage
import javax.inject.Inject

/**
 * Har bir so'rovga `Authorization: Bearer <access>` sarlavhasini qo'shadi. Faqat authorized klientga
 * o'rnatiladi (public AuthApi klientiga emas). Token [SessionStorage.current] orqali xotiradagi keshdan
 * o'qiladi — har so'rovda diskka tegilmaydi. Sessiya bo'lmasa so'rov o'zgarishsiz ketadi.
 */
class TokenInterceptor @Inject constructor(
    private val sessionStorage: SessionStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val accessToken = sessionStorage.current()?.accessToken ?: return chain.proceed(chain.request())
        return chain.proceed(
            chain.request().newBuilder()
                .header(HEADER, BEARER_PREFIX + accessToken)
                .build()
        )
    }

    // TokenAuthenticator ham shu konstantalar bilan eski token'ni sarlavhadan ajratib oladi.
    internal companion object {
        const val HEADER = "Authorization"
        const val BEARER_PREFIX = "Bearer "
    }
}
