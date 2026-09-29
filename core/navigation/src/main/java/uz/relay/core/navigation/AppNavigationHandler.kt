package uz.relay.core.navigation

import kotlinx.coroutines.flow.Flow

/** UI side: the app module collects these and applies them to the back stack. */
interface AppNavigationHandler {

    val params: Flow<AppNavigationParam>
}
