package uz.relay.data.call

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import io.getstream.log.Priority
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.core.socket.common.token.TokenProvider
import io.getstream.video.android.model.User
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.relay.core.common.dispatcher.ApplicationScope
import uz.relay.data.BuildConfig
import uz.relay.data.source.network.api.StreamTokenApi
import uz.relay.domain.model.AuthState
import uz.relay.domain.repository.AuthRepository
import uz.relay.domain.repository.UserRepository

/**
 * Stream Video client'ining hayotiy sikli Relay sessiyasiga bog'langan:
 *  - login qilingan va profil ma'lum → client quriladi (Stream user id = Relay userId, ism = displayName);
 *  - logout / sessiya tugadi → `logOut()` + `removeClient()` (keyingi hisob oldingisining qo'ng'iroqlarini olmasin);
 *  - boshqa hisobga kirildi → eski client yopilib, yangisi quriladi.
 *
 * Nega Application darajasida (RealtimeCoordinator kabi): Stream client — process bo'yicha yagona singleton, uni
 * ekran ichida yaratib bo'lmaydi; kiruvchi qo'ng'iroq istalgan ekranda kelishi mumkin.
 *
 * Token: `server/stream-token` (Cloudflare Worker) beradi — u Relay access token'i orqali foydalanuvchini aniqlab, JWT'ni
 * API Secret bilan imzolaydi (secret faqat serverda). SDK token muddati tugaganda [TokenProvider]ni o'zi qayta chaqiradi.
 * STREAM_TOKEN_URL berilmagan bo'lsa: debug build'da dev-token (Stream'da "Disable Auth Checks" yoqiq bo'lishi kerak),
 * release'da qo'ng'iroqlar umuman ulanmaydi — dev-token bilan istalgan odam istalgan foydalanuvchi bo'lib kira olardi.
 *
 * API key bo'lmasa (local.properties'da STREAM_API_KEY yo'q) — qo'ng'iroqlar shunchaki o'chiq, ilova yiqilmaydi.
 */
@Singleton
class StreamVideoConnector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val streamTokenApi: StreamTokenApi,
    @ApplicationScope private val scope: CoroutineScope
) {
    private val _connectedUserId = MutableStateFlow<String?>(null)

    /** Stream'ga ulangan foydalanuvchi (`null` — client yo'q). CallRepository shunga qarab ishlaydi. */
    val connectedUserId: StateFlow<String?> = _connectedUserId.asStateFlow()

    private var started = false

    /** Application.onCreate'dan bir marta chaqiriladi. */
    @Synchronized
    fun start() {
        if (started || BuildConfig.STREAM_API_KEY.isBlank()) return
        if (BuildConfig.STREAM_TOKEN_URL.isBlank() && !BuildConfig.DEBUG) {
            Log.w(TAG, "STREAM_TOKEN_URL is not set — calls are disabled in release")
            return
        }
        started = true
        scope.launch {
            combine(authRepository.authState, userRepository.observeMe()) { auth, me ->
                if (auth == AuthState.LOGGED_IN && me != null) me.id to me.displayName else null
            }
                .distinctUntilChanged()
                .collectLatest { identity -> if (identity == null) disconnect() else connect(identity.first, identity.second) }
        }
    }

    /**
     * Builder Stream singleton'ini ro'yxatdan o'tkazadi — main thread'da (SDK ichida lifecycle/audio obyektlari bor).
     * Builder boshlang'ich token bo'sh bo'lishiga yo'l qo'ymaydi ("token cannot be blank"), shuning uchun birinchi
     * token oldindan olinadi; keyingi yangilashlarni SDK [tokenProvider] orqali o'zi qiladi.
     */
    private suspend fun connect(userId: String, name: String) {
        val existing = withContext(Dispatchers.Main) { StreamVideo.instanceOrNull() }
        if (existing != null && existing.userId == userId) return
        val provider = tokenProvider(userId)
        val token = initialToken(provider) ?: run {
            Log.w(TAG, "Stream token unavailable — calls stay disabled until next login/app start")
            return
        }
        withContext(Dispatchers.Main) {
            StreamVideo.instanceOrNull()?.let {
                it.logOut()
                StreamVideo.removeClient()
            }
            runCatching {
                StreamVideoBuilder(
                    context = context,
                    apiKey = BuildConfig.STREAM_API_KEY,
                    user = User(id = userId, name = name),
                    token = token,
                    tokenProvider = provider,
                    // DEBUG darajasida SDK har health-check, SFU paket va WebRTC hodisasini yozardi (daqiqasiga minglab
                    // qator) — logcat to'lib, ilova sezilarli sekinlashardi. WARN — faqat muammolar.
                    loggingLevel = LoggingLevel(priority = if (BuildConfig.DEBUG) Priority.WARN else Priority.ERROR),
                    appName = APP_NAME
                ).build()
            }.onSuccess {
                _connectedUserId.value = userId
                Log.i(TAG, "Stream Video connected as $userId")
            }
                .onFailure { Log.e(TAG, "Stream Video client could not be built", it) }
        }
    }

    /**
     * Birinchi token: internet yo'q yoki server vaqtincha javob bermasa bir necha marta, oralig'ini oshirib urinadi.
     * Foydalanuvchi bu orada chiqib ketsa yoki almashsa, `collectLatest` bu kutishni bekor qiladi.
     */
    private suspend fun initialToken(provider: TokenProvider): String? {
        var wait = 2_000L
        repeat(TOKEN_ATTEMPTS) { attempt ->
            val token = withContext(Dispatchers.IO) { provider.loadToken() }
            if (token.isNotBlank()) return token
            if (attempt < TOKEN_ATTEMPTS - 1) {
                delay(wait)
                wait *= 2
            }
        }
        return null
    }

    /**
     * Token serveridan Stream token oladi. Shartnoma bo'yicha xato bo'lsa bo'sh qator qaytariladi (SDK qayta urinadi).
     * Server boshqa foydalanuvchini qaytarsa (masalan, logout/login orasida eski token) — token ishlatilmaydi.
     */
    private fun tokenProvider(userId: String) = object : TokenProvider {
        override suspend fun loadToken(): String {
            if (BuildConfig.STREAM_TOKEN_URL.isBlank()) return StreamVideo.devToken(userId)
            return try {
                val response = streamTokenApi.token(BuildConfig.STREAM_TOKEN_URL)
                if (response.userId == userId) response.token
                else {
                    Log.w(TAG, "token server returned another user")
                    ""
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Stream token request failed", e)
                ""
            }
        }
    }

    private suspend fun disconnect() = withContext(Dispatchers.Main) {
        StreamVideo.instanceOrNull()?.let {
            it.logOut()
            StreamVideo.removeClient()
            Log.i(TAG, "Stream Video disconnected")
        }
        _connectedUserId.value = null
    }

    private companion object {
        /** Birinchi Stream token'ni olish urinishlari (2 s, 4 s, 8 s, 16 s oraliq bilan). */
        const val TOKEN_ATTEMPTS = 5
        const val TAG = "StreamVideoConnector"
        const val APP_NAME = "SwiftChat"
    }
}
