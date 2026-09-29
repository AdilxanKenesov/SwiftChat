package uz.relay.data.realtime

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import uz.relay.core.common.dispatcher.ApplicationScope
import uz.relay.data.connection.NetworkMonitor
import uz.relay.data.outbox.OutboxScheduler
import uz.relay.data.source.local.database.dao.UserDao
import uz.relay.data.source.network.realtime.RealtimeClient
import uz.relay.data.source.network.realtime.ServerFrame
import uz.relay.data.sync.SyncEngine
import uz.relay.domain.model.AuthState
import uz.relay.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebSocket'ni ilova holatiga bog'laydi va kelgan frame'larni kerakli joyga yo'naltiradi.
 *
 * - Socket faqat login qilingan VA ilova old planda bo'lganda ochiq. Fonda yopiladi: server socket'i yo'q
 *   qurilmaga push yuboradi (Guide, 8-bo'lim), socket'ni ochiq ushlab batareyani yeyish shart emas.
 * - `auth_ok` → catch-up (socket ochilguncha o'tkazib yuborilgan hodisalar) + outbox'ni yuborish.
 * - `update` → SyncEngine (teshik tekshiruvi bilan), `presence` → profil keshi, `typing` → TypingTracker.
 * - `ack` / `nack` ni OutboxSender o'zi kutadi.
 */
@Singleton
class RealtimeCoordinator @Inject constructor(
    private val authRepository: AuthRepository,
    private val realtimeClient: RealtimeClient,
    private val syncEngine: SyncEngine,
    private val userDao: UserDao,
    private val networkMonitor: NetworkMonitor,
    private val typingTracker: TypingTracker,
    private val outboxScheduler: OutboxScheduler,
    @ApplicationScope private val scope: CoroutineScope
) {
    private var started = false

    /** Application.onCreate'dan bir marta chaqiriladi. */
    @OptIn(ExperimentalCoroutinesApi::class) // transformLatest
    @Synchronized
    fun start() {
        if (started) return
        started = true

        scope.launch {
            combine(
                authRepository.authState.map { it == AuthState.LOGGED_IN },
                appInForeground()
            ) { loggedIn, foreground -> loggedIn && foreground }
                .distinctUntilChanged()
                // Ilovadan bir lahzaga chiqib qaytish (masalan, galereyani ochish) socket'ni uzib-ulamasin:
                // "fonga o'tdi" holati faqat 5 s davom etsagina qo'llanadi.
                .transformLatest { active ->
                    if (!active) delay(BACKGROUND_GRACE_MS)
                    emit(active)
                }
                .distinctUntilChanged()
                .collect { active ->
                    if (active) realtimeClient.start { syncEngine.cursor() ?: 0 } else realtimeClient.stop()
                }
        }

        scope.launch {
            // Bitta kolektor, ketma-ket: frame'lar kelgan tartibda qo'llanadi.
            realtimeClient.frames.collect { frame -> handle(frame) }
        }

        scope.launch {
            // Internet qaytdi — backoff tugashini kutmasdan darhol ulanamiz.
            networkMonitor.isOnline.drop(1).filter { it }.collect { realtimeClient.reconnectNow() }
        }
    }

    private suspend fun handle(frame: ServerFrame) {
        when (frame) {
            is ServerFrame.AuthOk -> {
                // Socket ochildi — kutib qolgan xabarlar endi tezroq yo'l (socket) bilan ketadi.
                outboxScheduler.schedule()
                // Server `updateSeq` ni socket ro'yxatga olingandan KEYIN o'qiydi: undan kattalari jonli keladi,
                // unga qadar bo'lganlari REST'da. Biz orqada bo'lsak — catch-up (ustma-ust tushish zararsiz).
                if (frame.updateSeq > (syncEngine.cursor() ?: -1)) syncEngine.catchUp()
            }

            is ServerFrame.Update -> syncEngine.onLiveUpdate(frame.toUpdateResponse())
            is ServerFrame.Presence -> userDao.updatePresence(frame.userId, frame.online, frame.lastSeenAt)
            is ServerFrame.Typing -> typingTracker.onTyping(frame.chatId, frame.userId)
            else -> Unit
        }
    }

    /** ProcessLifecycleOwner — butun ilova (bitta Activity emas) foydalanuvchiga ko'rinib turibdimi. */
    private fun appInForeground(): Flow<Boolean> =
        ProcessLifecycleOwner.get().lifecycle.currentStateFlow
            .map { it.isAtLeast(Lifecycle.State.STARTED) }
            .distinctUntilChanged()
            // Lifecycle holati main thread'da o'qilishi kerak.
            .flowOn(Dispatchers.Main)

    private companion object {
        const val BACKGROUND_GRACE_MS = 5_000L
    }
}
