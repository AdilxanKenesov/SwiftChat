package uz.relay.feature.auth.splash

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.viewmodel.orbitContainer
import uz.relay.domain.model.AuthState
import uz.relay.domain.usecase.auth.ObserveAuthStateUseCase
import javax.inject.Inject

/**
 * Faqat birinchi ekranni tanlaydi. Keyingi o'zgarishlar (sessiya tugashi) MainViewModel'ning ishi:
 * sessiya istalgan ekranda, splash allaqachon yopilganidan ancha keyin tugashi mumkin.
 *
 * Mantiq Orbit container'ning `onCreate` blokida - u faqat birinchi obuna bo'lganda bir marta ishlaydi
 * va konfiguratsiya o'zgarishida (ekran aylanishi) qayta ishga tushmaydi.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val observeAuthState: ObserveAuthStateUseCase,
    private val directions: SplashContract.Directions
) : ViewModel(), SplashContract.ViewModel {

    override val container =
        orbitContainer<SplashContract.UiState, SplashContract.SideEffect>(SplashContract.UiState) {
            // Saqlangan sessiya o'qilmaguncha splash ko'rinib turadi.
            when (observeAuthState().first()) {
                AuthState.LOGGED_OUT -> directions.navigateToPhone()
                AuthState.NEEDS_PROFILE -> directions.navigateToProfileSetup()
                AuthState.LOGGED_IN -> directions.navigateToChats()
            }
        }
}
