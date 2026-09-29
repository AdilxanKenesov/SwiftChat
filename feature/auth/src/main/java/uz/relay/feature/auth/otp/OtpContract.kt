package uz.relay.feature.auth.otp

import org.orbitmvi.orbit.OrbitContainerHost
import uz.relay.core.common.result.AppError

interface OtpContract {

    interface ViewModel : OrbitContainerHost<UiState, UiState, SideEffect> {
        fun onEventDispatcher(intent: Intent)
    }

    sealed interface Intent {
        data class OnCodeChange(val code: String) : Intent
        object OnResendCode : Intent
        object OnBack : Intent
    }

    sealed interface SideEffect {
        data class ShowError(val error: AppError) : SideEffect
        /** Wrong code: shake the boxes. */
        object Shake : SideEffect
    }

    sealed interface Status {
        object Input : Status
        data class Wrong(val attemptsLeft: Int) : Status
        object Expired : Status
        object Locked : Status
    }

    data class UiState(
        val phone: String,
        /** Number of code digits (6). */
        val codeLength: Int,
        val code: String = "",
        val status: Status = Status.Input,
        val verifying: Boolean = false,
        val resending: Boolean = false,
        val secondsLeft: Int = RESEND_SECONDS
    ) {
        val inputEnabled: Boolean
            get() = !verifying && status != Status.Expired && status != Status.Locked
    }

    interface Directions {
        suspend fun back()
        suspend fun navigateToProfileSetup()
        suspend fun navigateToChats()
    }

    companion object {
        const val MAX_ATTEMPTS = 5
        const val RESEND_SECONDS = 60
    }
}
