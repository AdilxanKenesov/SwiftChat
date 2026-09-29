package uz.relay.core.navigation

import kotlinx.coroutines.flow.Flow

interface AppNavigationHandler {

    val backStack: Flow<AppNavigationParam>
}
