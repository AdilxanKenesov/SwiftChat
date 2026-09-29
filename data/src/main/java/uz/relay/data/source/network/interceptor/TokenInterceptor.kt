package uz.relay.data.source.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import uz.relay.data.source.local.SessionStorage
import javax.inject.Inject

/** Adds `Authorization: Bearer <access>`. Installed only on the authorized client. */
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

    internal companion object {
        const val HEADER = "Authorization"
        const val BEARER_PREFIX = "Bearer "
    }
}
