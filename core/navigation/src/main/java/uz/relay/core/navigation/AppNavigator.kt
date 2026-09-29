package uz.relay.core.navigation

import androidx.navigation3.runtime.NavKey

interface AppNavigator {

    fun navigateTo(route: NavKey)

    fun replaceTo(route: NavKey)

    fun replaceAll(route: NavKey)

    fun back()

    fun backTo(predicate: (NavKey) -> Boolean)
}
