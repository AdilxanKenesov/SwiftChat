package uz.relay.feature.auth.otp

import org.orbitmvi.orbit.OrbitContainerHost
import uz.relay.core.common.result.AppError

/**
 * SMS/Telegram orqali kelgan tasdiqlash kodini kiritish ekranining Orbit MVI kontrakti.
 *
 * Contract Intent, UiState, SideEffect va Directions'ni bitta joyda guruhlaydi.
 * Oqim: Phone ekrani -> shu ekran -> ProfileSetup (yangi foydalanuvchi) yoki Chats.
 */
interface OtpContract {

    interface ViewModel : OrbitContainerHost<UiState, UiState, SideEffect> {
        fun onEventDispatcher(intent: Intent)
    }

    /** Foydalanuvchi harakatlari. */
    sealed interface Intent {
        data class OnCodeChange(val code: String) : Intent
        object OnResendCode : Intent
        object OnBack : Intent
    }

    sealed interface SideEffect {
        data class ShowError(val error: AppError) : SideEffect
        /** Kod noto'g'ri: kataklarni silkitish (bir martalik animatsiya, shuning uchun state emas). */
        object Shake : SideEffect
    }

    /** Kod kiritish holati: oddiy kiritish, noto'g'ri (qolgan urinishlar bilan), muddati o'tgan yoki bloklangan. */
    sealed interface Status {
        object Input : Status
        data class Wrong(val attemptsLeft: Int) : Status
        object Expired : Status
        object Locked : Status
    }

    data class UiState(
        val phone: String,
        /** Kod raqamlari soni (6). */
        val codeLength: Int,
        val code: String = "",
        val status: Status = Status.Input,
        val verifying: Boolean = false,
        val resending: Boolean = false,
        val secondsLeft: Int = RESEND_SECONDS
    ) {
        // Tekshiruv paytida yoki kod yaroqsiz bo'lganda (muddati o'tgan/bloklangan) kiritish o'chiriladi.
        val inputEnabled: Boolean
            get() = !verifying && status != Status.Expired && status != Status.Locked
    }

    interface Directions {
        suspend fun back()
        suspend fun navigateToProfileSetup()
        suspend fun navigateToChats()
    }

    // Server bir kod uchun 5 ta urinish beradi; qayta yuborish 60 soniyadan keyin ochiladi.
    companion object {
        const val MAX_ATTEMPTS = 5
        const val RESEND_SECONDS = 60
    }
}
