package uz.relay.core.navigation

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Navigation is an event, not state: a Channel delivers each command exactly once, so a rotation
 * does not replay it. The buffer keeps commands sent before the UI starts collecting.
 */
@Singleton
class AppNavigationDispatcher @Inject constructor() : AppNavigator, AppNavigationHandler {

    private val channel = Channel<AppNavigationParam>(capacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override val params: Flow<AppNavigationParam> = channel.receiveAsFlow()

    override suspend fun navigate(param: AppNavigationParam) {
        channel.send(param)
    }
}
