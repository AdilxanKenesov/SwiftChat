package uz.relay.core.navigation

/** ViewModel side: asks for a navigation without knowing the UI. */
interface AppNavigator {

    suspend fun navigate(param: AppNavigationParam)
}
