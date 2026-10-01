package uz.relay.data.call

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import io.getstream.log.Priority
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.core.logging.LoggingLevel
import io.getstream.video.android.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.relay.core.common.dispatcher.ApplicationScope
import uz.relay.data.BuildConfig
import uz.relay.domain.model.AuthState
import uz.relay.domain.repository.AuthRepository
import uz.relay.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stream Video client'ining hayotiy sikli Relay sessiyasiga bog'langan:
 *  - login qilingan va profil ma'lum → client quriladi (Stream user id = Relay userId, ism = displayName);
 *  - logout / sessiya tugadi → `logOut()` + `removeClient()` (keyingi hisob oldingisining qo'ng'iroqlarini olmasin);
 *  - boshqa hisobga kirildi → eski client yopilib, yangisi quriladi.
 *
 * Nega Application darajasida (RealtimeCoordinator kabi): Stream client — process bo'yicha yagona singleton, uni
 * ekran ichida yaratib bo'lmaydi; kiruvchi qo'ng'iroq istalgan ekranda kelishi mumkin.
 *
 * Token: hozircha dev-token (`StreamVideo.devToken`) — Stream dashboard'da "Disable Auth Checks" yoqilgan. Bu faqat
 * development uchun: relizdan oldin Relay token'ini tekshirib Stream token beradigan kichik server qo'shiladi
 * (secret faqat serverda).
 *
 * API key bo'lmasa (local.properties'da STREAM_API_KEY yo'q) — qo'ng'iroqlar shunchaki o'chiq, ilova yiqilmaydi.
 */
@Singleton
class StreamVideoConnector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
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
        started = true
        scope.launch {
            combine(authRepository.authState, userRepository.observeMe()) { auth, me ->
                if (auth == AuthState.LOGGED_IN && me != null) me.id to me.displayName else null
            }
                .distinctUntilChanged()
                .collect { identity -> if (identity == null) disconnect() else connect(identity.first, identity.second) }
        }
    }

    /** Builder Stream singleton'ini ro'yxatdan o'tkazadi — main thread'da (SDK ichida lifecycle/audio obyektlari bor). */
    private suspend fun connect(userId: String, name: String) = withContext(Dispatchers.Main) {
        val existing = StreamVideo.instanceOrNull()
        if (existing != null && existing.userId == userId) return@withContext
        if (existing != null) {
            existing.logOut()
            StreamVideo.removeClient()
        }
        runCatching {
            StreamVideoBuilder(
                context = context,
                apiKey = BuildConfig.STREAM_API_KEY,
                user = User(id = userId, name = name),
                token = StreamVideo.devToken(userId),
                loggingLevel = LoggingLevel(priority = if (BuildConfig.DEBUG) Priority.DEBUG else Priority.ERROR),
                appName = APP_NAME
            ).build()
        }.onSuccess { _connectedUserId.value = userId }
            .onFailure { Log.e(TAG, "Stream Video client could not be built", it) }
    }

    private suspend fun disconnect() = withContext(Dispatchers.Main) {
        StreamVideo.instanceOrNull()?.let {
            it.logOut()
            StreamVideo.removeClient()
        }
        _connectedUserId.value = null
    }

    private companion object {
        const val TAG = "StreamVideoConnector"
        const val APP_NAME = "SwiftChat"
    }
}
