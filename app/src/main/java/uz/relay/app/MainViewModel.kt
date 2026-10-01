package uz.relay.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import androidx.navigation3.runtime.NavKey
import uz.relay.core.navigation.key.ChatsKey
import uz.relay.core.navigation.key.ProfileSetupKey
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.relay.core.navigation.AppNavigationHandler
import uz.relay.core.navigation.AppNavigationParam
import uz.relay.core.navigation.AppNavigator
import uz.relay.core.navigation.key.PhoneKey
import uz.relay.domain.model.AuthState
import uz.relay.domain.model.ThemeMode
import uz.relay.domain.usecase.auth.ObserveAuthStateUseCase
import uz.relay.domain.usecase.call.ObserveIncomingCallsUseCase
import uz.relay.core.navigation.key.CallKey
import uz.relay.domain.usecase.settings.ObserveThemeModeUseCase
import javax.inject.Inject

/**
 * Activity darajasidagi ishlar: butun ilova temasi va sessiya tugashini kuzatish.
 *
 * Sessiya istalgan ekranda tugashi mumkin (TOKEN_REUSED, refresh muvaffaqiyatsiz, WS 4003 yopilishi yoki
 * profildan logout): shunda ilova qaysi ekranda bo'lmasin, stek tozalanib telefon kiritish ekraniga
 * ([PhoneKey]) qaytadi — [AppNavigationParam.ResetTo], chunki orqaga bosib yopiq sessiya ekranlariga qaytib
 * bo'lmasligi kerak. Bu mantiq har bir feature'da takrorlanmasligi uchun bitta joyda — shu yerda.
 *
 * Birinchi qiymat o'tkazib yuboriladi (`drop(1)`): boshlang'ich ekranni [startKey] tanlaydi, aks holda
 * ilova ochilishida ikki marta navigatsiya bo'lardi.
 *
 * [navigationHandler] shu yerda ochiq, chunki MainActivity uni [uz.relay.app.navigation.AppNavHost] ga uzatadi.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    observeAuthState: ObserveAuthStateUseCase,
    observeThemeMode: ObserveThemeModeUseCase,
    observeIncomingCalls: ObserveIncomingCallsUseCase,
    navigator: AppNavigator,
    val navigationHandler: AppNavigationHandler
) : ViewModel() {

    /**
     * Butun ilova temasi shu yerdan olinadi: profilda "Tungi rejim" bosilishi bilan hamma ekran darhol
     * almashadi. `null` — DataStore hali o'qilmagan: shu payt splash ushlab turiladi, aks holda tungi rejimdagi
     * foydalanuvchi bir lahza yorug' ekranni ko'rardi.
     */
    val themeMode: StateFlow<ThemeMode?> = observeThemeMode()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * Boshlang'ich ekran — saqlangan sessiyaga qarab. Alohida Splash EKRANI yo'q: aks holda foydalanuvchi avval
     * tizim splash'ini (Android 12+ da uni o'chirib bo'lmaydi), keyin xuddi shunday ko'rinadigan Compose
     * splash'ni ko'rardi — "ikki marta splash". Endi tizim splash'i shu qiymat `null` bo'lguncha ushlab
     * turiladi (MainActivity), keyin darhol kerakli ekran ochiladi.
     */
    val startKey: StateFlow<NavKey?> = flow {
        emit(
            when (observeAuthState().first()) {
                AuthState.LOGGED_IN -> ChatsKey
                AuthState.NEEDS_PROFILE -> ProfileSetupKey
                AuthState.LOGGED_OUT -> PhoneKey
            }
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        // Kiruvchi qo'ng'iroq istalgan ekranda bo'lsa ham qo'ng'iroq ekrani ochiladi (singleTop — ikki marta emas).
        viewModelScope.launch {
            observeIncomingCalls().collect { callId -> navigator.navigate(AppNavigationParam.To(CallKey(callId))) }
        }
        viewModelScope.launch {
            observeAuthState()
                .drop(1)
                .filter { it == AuthState.LOGGED_OUT }
                .collect { navigator.navigate(AppNavigationParam.ResetTo(PhoneKey)) }
        }
    }
}
