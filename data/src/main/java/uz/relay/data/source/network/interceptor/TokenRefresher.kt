package uz.relay.data.source.network.interceptor

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import uz.relay.data.model.request.RefreshTokenRequest
import uz.relay.data.source.local.SessionStorage
import uz.relay.data.source.network.api.AuthApi
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Token yangilash natijasi — chaqiruvchi (REST yoki WebSocket) keyin nima qilishni shunga qarab hal qiladi. */
sealed interface RefreshOutcome {
    /** Yangi (yoki boshqa oqim allaqachon olgan) access token. */
    data class Refreshed(val accessToken: String) : RefreshOutcome

    /** Refresh token rad etildi (TOKEN_REUSED, muddati o'tgan, logout qilingan): sessiya tozalanadi. */
    data object SessionEnded : RefreshOutcome

    /** Internet yo'q yoki 5xx: sessiya hali yaroqli, keyinroq qayta urinish kerak. */
    data object TemporaryFailure : RefreshOutcome
}

/**
 * Access token'ni yangilaydigan YAGONA joy: REST'da [TokenAuthenticator] (401), WebSocket'da
 * RealtimeClient (4001 yopilish kodi) shu yerni chaqiradi.
 *
 * Nega Mutex: refresh token bir martalik (rotatsiya). Ikki so'rov bir vaqtda o'sha token bilan yangilasa,
 * ikkinchisiga TOKEN_REUSED keladi va server qurilmaning butun sessiyasini bekor qiladi. Mutex faqat
 * bittasiga yangilashga ruxsat beradi; qolganlari navbat kutib, yangi token'ni ko'radi va o'shani ishlatadi.
 */
@Singleton
class TokenRefresher @Inject constructor(
    private val authApi: AuthApi,
    private val sessionStorage: SessionStorage
) {
    private val mutex = Mutex()

    /**
     * @param failedAccessToken 401 olgan token; omborda allaqachon yangirog'i bo'lsa, tarmoqqa chiqmasdan
     * o'sha qaytariladi.
     */
    suspend fun refresh(failedAccessToken: String?): RefreshOutcome = mutex.withLock {
        val session = sessionStorage.current() ?: return@withLock RefreshOutcome.SessionEnded

        // Mutex'ni kutayotgan paytda boshqa oqim token'ni allaqachon yangilagan.
        if (failedAccessToken != null && session.accessToken != failedAccessToken) {
            return@withLock RefreshOutcome.Refreshed(session.accessToken)
        }

        try {
            val tokens = authApi.refresh(RefreshTokenRequest(session.refreshToken))
            sessionStorage.updateTokens(tokens.accessToken, tokens.refreshToken)
            RefreshOutcome.Refreshed(tokens.accessToken)
        } catch (e: CancellationException) {
            // Korutina bekor qilinishini yutib yubormaymiz.
            throw e
        } catch (e: HttpException) {
            if (e.code() in 400..499) {
                // TOKEN_REUSED / muddati o'tgan / yaroqsiz: qayta urinish befoyda, foydalanuvchi qaytadan kirishi kerak.
                sessionStorage.clear()
                RefreshOutcome.SessionEnded
            } else {
                RefreshOutcome.TemporaryFailure
            }
        } catch (e: IOException) {
            RefreshOutcome.TemporaryFailure
        }
    }
}
