package uz.relay.feature.auth.splash

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.viewmodel.orbitContainer
import uz.relay.domain.model.AuthState
import uz.relay.domain.usecase.auth.ObserveAuthStateUseCase
import javax.inject.Inject

/**
 * Only picks the first screen. Later changes (session ended) are MainViewModel's job:
 * a session can end on any screen, long after the splash is gone.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val observeAuthState: ObserveAuthStateUseCase,
    private val directions: SplashContract.Directions
) : ViewModel(), SplashContract.ViewModel {

    override val container =
        orbitContainer<SplashContract.UiState, SplashContract.SideEffect>(SplashContract.UiState) {
            // The splash stays until the stored session is read.
            when (observeAuthState().first()) {
                AuthState.LOGGED_OUT -> directions.navigateToPhone()
                AuthState.NEEDS_PROFILE -> directions.navigateToProfileSetup()
                AuthState.LOGGED_IN -> directions.navigateToChats()
            }
        }
}
