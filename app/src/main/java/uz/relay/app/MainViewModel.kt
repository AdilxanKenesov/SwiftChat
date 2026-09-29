package uz.relay.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import uz.relay.core.navigation.AppNavigationHandler
import uz.relay.core.navigation.AppNavigationParam
import uz.relay.core.navigation.AppNavigator
import uz.relay.core.navigation.key.PhoneKey
import uz.relay.domain.model.AuthState
import uz.relay.domain.usecase.auth.ObserveAuthStateUseCase
import javax.inject.Inject

/**
 * Activity-level work. A session can end on any screen (TOKEN_REUSED, a failed refresh, later WS 4003):
 * then the app returns to login wherever it is. The first value is skipped — Splash picks the start screen.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    observeAuthState: ObserveAuthStateUseCase,
    navigator: AppNavigator,
    val navigationHandler: AppNavigationHandler
) : ViewModel() {

    init {
        viewModelScope.launch {
            observeAuthState()
                .drop(1)
                .filter { it == AuthState.LOGGED_OUT }
                .collect { navigator.navigate(AppNavigationParam.ResetTo(PhoneKey)) }
        }
    }
}
