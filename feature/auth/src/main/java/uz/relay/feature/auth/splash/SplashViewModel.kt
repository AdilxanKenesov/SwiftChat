package uz.relay.feature.auth.splash

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.viewmodel.orbitContainer
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val directions: SplashContract.Directions
) : ViewModel(), SplashContract.ViewModel {

    override val container =
        orbitContainer<SplashContract.UiState, SplashContract.SideEffect>(SplashContract.UiState)
}
