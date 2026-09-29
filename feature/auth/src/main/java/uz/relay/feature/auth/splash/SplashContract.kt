package uz.relay.feature.auth.splash

import org.orbitmvi.orbit.OrbitContainerHost

interface SplashContract {

    interface ViewModel : OrbitContainerHost<UiState, UiState, SideEffect>

    sealed interface SideEffect

    object UiState

    interface Directions {
        suspend fun navigateToPhone()
        suspend fun navigateToProfileSetup()
        suspend fun navigateToChats()
    }
}
