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

sealed interface RefreshOutcome {
    data class Refreshed(val accessToken: String) : RefreshOutcome

    /** The refresh token was rejected (TOKEN_REUSED, expired, logged out): the session is cleared. */
    data object SessionEnded : RefreshOutcome

    /** No connection or 5xx: the session is still valid, try again later. */
    data object TemporaryFailure : RefreshOutcome
}

/**
 * The single place that refreshes the access token (REST authenticator now, WebSocket 4001 later).
 *
 * The refresh token is single-use: if two requests refreshed with it at once, the second would be
 * answered TOKEN_REUSED and the whole device session revoked. The Mutex lets one caller refresh;
 * the others see the new token and reuse it.
 */
@Singleton
class TokenRefresher @Inject constructor(
    private val authApi: AuthApi,
    private val sessionStorage: SessionStorage
) {
    private val mutex = Mutex()

    /** @param failedAccessToken the token that got 401; if storage already holds a newer one, it is returned. */
    suspend fun refresh(failedAccessToken: String?): RefreshOutcome = mutex.withLock {
        val session = sessionStorage.current() ?: return@withLock RefreshOutcome.SessionEnded

        if (failedAccessToken != null && session.accessToken != failedAccessToken) {
            return@withLock RefreshOutcome.Refreshed(session.accessToken)
        }

        try {
            val tokens = authApi.refresh(RefreshTokenRequest(session.refreshToken))
            sessionStorage.updateTokens(tokens.accessToken, tokens.refreshToken)
            RefreshOutcome.Refreshed(tokens.accessToken)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            if (e.code() in 400..499) {
                // TOKEN_REUSED / expired / invalid: retrying is pointless, the user must log in again.
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
