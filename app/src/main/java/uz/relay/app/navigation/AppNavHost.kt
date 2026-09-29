package uz.relay.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import uz.relay.core.navigation.AppNavigationHandler
import uz.relay.core.navigation.AppNavigationParam
import uz.relay.core.navigation.key.SplashKey
import uz.relay.feature.auth.authEntries
import uz.relay.feature.chats.chatsEntries
import uz.relay.feature.conversation.conversationEntries
import uz.relay.feature.group.groupEntries

/** The single back stack: every Directions command lands here through [AppNavigationHandler]. */
@Composable
fun AppNavHost(navigationHandler: AppNavigationHandler) {
    // @Serializable keys: the stack survives process death.
    val backStack = rememberNavBackStack(SplashKey)

    LaunchedEffect(navigationHandler) {
        navigationHandler.params.collect { param -> backStack.apply(param) }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.pop() },
        entryDecorators = listOf(
            // rememberSaveable state per screen.
            rememberSaveableStateHolderNavEntryDecorator(),
            // A ViewModelStore per screen: leaving the stack clears its ViewModel.
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            authEntries()
            chatsEntries()
            conversationEntries()
            groupEntries()
        }
    )
}

/** The last screen is never removed: an empty stack crashes NavDisplay. */
private fun MutableList<NavKey>.pop() {
    if (size > 1) removeAt(lastIndex)
}

internal fun MutableList<NavKey>.apply(param: AppNavigationParam) {
    when (param) {
        is AppNavigationParam.To -> if (!(param.singleTop && lastOrNull() == param.key)) add(param.key)
        is AppNavigationParam.Replace -> {
            if (isNotEmpty()) removeAt(lastIndex)
            add(param.key)
        }

        AppNavigationParam.Back -> pop()
        is AppNavigationParam.BackTo -> {
            // Not in the stack: do nothing rather than closing every screen.
            val index = lastIndexOf(param.key)
            if (index >= 0) {
                val keep = if (param.inclusive) index else index + 1
                while (size > keep.coerceAtLeast(1)) removeAt(lastIndex)
            }
        }

        is AppNavigationParam.ResetTo -> {
            if (size == 1 && first() == param.key) return
            clear()
            add(param.key)
        }
    }
}
