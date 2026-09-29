package uz.relay.feature.auth.phone

import org.orbitmvi.orbit.OrbitContainerHost
import uz.relay.core.common.result.AppError

interface PhoneContract {

    interface ViewModel : OrbitContainerHost<UiState, UiState, SideEffect> {
        fun onEventDispatcher(intent: Intent)
    }

    sealed interface Intent {
        data class OnPhoneChange(val digits: String) : Intent
        object OnGetCode : Intent
        object OnOpenBot : Intent
        object OnResendCode : Intent
        object OnDismissTelegramSheet : Intent
    }

    sealed interface SideEffect {
        data class ShowError(val error: AppError) : SideEffect
        data class OpenUrl(val url: String) : SideEffect
    }

    data class UiState(
        /** Local part only, up to 9 digits. */
        val digits: String = "",
        val loading: Boolean = false,
        /** Non-null while the "link Telegram" sheet is shown (409 TELEGRAM_NOT_LINKED). */
        val botUrl: String? = null
    ) {
        val continueEnabled: Boolean get() = digits.length == 9
    }

    interface Directions {
        suspend fun navigateToOtp(phone: String)
    }
}
