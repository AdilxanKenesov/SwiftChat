package uz.relay.data.source.network.interceptor

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import javax.inject.Inject

/** On 401 OkHttp asks here: refresh the token and replay the request once. */
class TokenAuthenticator @Inject constructor(
    private val tokenRefresher: TokenRefresher
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val failedToken = response.request.header(TokenInterceptor.HEADER)
            ?.removePrefix(TokenInterceptor.BEARER_PREFIX)
            ?: return null // the request had no token, refreshing would not help

        // Still 401 with the fresh token: stop instead of looping.
        if (response.priorResponse != null) return null

        return when (val outcome = runBlocking { tokenRefresher.refresh(failedToken) }) {
            is RefreshOutcome.Refreshed -> response.request.newBuilder()
                .header(TokenInterceptor.HEADER, TokenInterceptor.BEARER_PREFIX + outcome.accessToken)
                .build()

            RefreshOutcome.SessionEnded -> null
            // The session is still valid: surface a retryable network error instead of a final 401.
            RefreshOutcome.TemporaryFailure -> throw IOException("Access token refresh failed temporarily")
        }
    }
}
