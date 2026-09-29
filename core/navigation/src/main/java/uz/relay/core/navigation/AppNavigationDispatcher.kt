package uz.relay.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

object AppNavigationDispatcher : AppNavigator, AppNavigationHandler {

    private val commands = Channel<AppNavigationParam>(Channel.BUFFERED)

    override val backStack: Flow<AppNavigationParam> = commands.receiveAsFlow()

    private fun navigate(param: AppNavigationParam) {
        commands.trySend(param)
    }

    override fun navigateTo(route: NavKey) = navigate {
        add(route)
    }

    override fun replaceTo(route: NavKey) = navigate {
        if (isNotEmpty()) removeAt(lastIndex)
        add(route)
    }

    override fun replaceAll(route: NavKey) = navigate {
        clear()
        add(route)
    }

    override fun back() = navigate {
        if (size > 1) removeAt(lastIndex)
    }

    override fun backTo(predicate: (NavKey) -> Boolean) = navigate {
        while (size > 1 && !predicate(last())) removeAt(lastIndex)
    }
}
