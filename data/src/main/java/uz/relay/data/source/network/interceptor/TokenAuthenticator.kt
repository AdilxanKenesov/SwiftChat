package uz.relay.data.source.network.interceptor

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import javax.inject.Inject

/**
 * OkHttp [Authenticator]: server 401 qaytarganda OkHttp shu yerga murojaat qiladi — token yangilanadi va
 * so'rov bir marta qayta yuboriladi.
 *
 * Nega Authenticator (interceptor emas): OkHttp 401'ni o'zi aniqlaydi va qayta yuborishni o'zi boshqaradi,
 * repository'lar esa token eskirganini umuman bilmaydi. Refresh'ning o'zi [TokenRefresher]da (Mutex bilan).
 * Faqat authorized klientga o'rnatiladi (NetworkModule).
 */
class TokenAuthenticator @Inject constructor(
    private val tokenRefresher: TokenRefresher
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val failedToken = response.request.header(TokenInterceptor.HEADER)
            ?.removePrefix(TokenInterceptor.BEARER_PREFIX)
            ?: return null // so'rovda token bo'lmagan — yangilash yordam bermaydi

        // Yangi token bilan ham 401: cheksiz aylanmaslik uchun to'xtaymiz.
        if (response.priorResponse != null) return null

        // runBlocking: Authenticator sinxron API, OkHttp uni allaqachon fon oqimida chaqiradi.
        return when (val outcome = runBlocking { tokenRefresher.refresh(failedToken) }) {
            is RefreshOutcome.Refreshed -> response.request.newBuilder()
                .header(TokenInterceptor.HEADER, TokenInterceptor.BEARER_PREFIX + outcome.accessToken)
                .build()

            // null — OkHttp 401 javobini qaytaradi; sessiya tozalangan, UI login'ga o'tadi.
            RefreshOutcome.SessionEnded -> null
            // Sessiya hali yaroqli: yakuniy 401 o'rniga qayta urinsa bo'ladigan tarmoq xatosini beramiz.
            RefreshOutcome.TemporaryFailure -> throw IOException("Access token refresh failed temporarily")
        }
    }
}
