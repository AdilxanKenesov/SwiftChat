package uz.relay.feature.auth.splash

import org.orbitmvi.orbit.OrbitContainerHost

interface SplashContract {

    interface ViewModel : OrbitContainerHost<UiState, UiState, SideEffect>

    sealed interface SideEffect

    object UiState

    // navigateToPhone / navigateToChats are added with the auth flow (token check).
    interface Directions
}
