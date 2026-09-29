package uz.relay.core.common.dispatcher

import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Qualifier

/** Injected instead of using `Dispatchers.*` directly, so tests can swap in a TestDispatcher. */
data class AppDispatchers(
    val io: CoroutineDispatcher,
    val default: CoroutineDispatcher,
    val main: CoroutineDispatcher
)

/** App-lifetime scope for work that must outlive a screen (sync, WebSocket, outbox). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
