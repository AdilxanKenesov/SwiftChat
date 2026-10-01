package uz.relay.feature.auth.profile

import org.orbitmvi.orbit.OrbitContainerHost
import uz.relay.core.common.result.AppError
import uz.relay.domain.model.ProfileRules

/**
 * Yangi foydalanuvchi profilini (ism va username) to'ldirish ekranining Orbit MVI kontrakti.
 *
 * Contract Intent, UiState, SideEffect va Directions'ni bitta joyda guruhlaydi.
 * Oqim: OTP (yangi foydalanuvchi) yoki ilova ishga tushganda NEEDS_PROFILE holati (MainViewModel.startKey) -> shu ekran -> Chats.
 */
interface ProfileSetupContract {

    interface ViewModel : OrbitContainerHost<UiState, UiState, SideEffect> {
        fun onEventDispatcher(intent: Intent)
    }

    /** Foydalanuvchi harakatlari. */
    sealed interface Intent {
        data class OnNameChange(val name: String) : Intent
        data class OnUsernameChange(val username: String) : Intent
        data class OnSuggestionClick(val username: String) : Intent
        object OnContinue : Intent
    }

    sealed interface SideEffect {
        data class ShowError(val error: AppError) : SideEffect
    }

    data class UiState(
        val name: String = "",
        val username: String = "",
        /** Joriy [username] uchun 409 USERNAME_TAKEN kelgandan keyin o'rnatiladi. */
        val usernameTaken: Boolean = false,
        val suggestions: List<String> = emptyList(),
        val saving: Boolean = false
    ) {
        // Validatsiya qoidalari domain'dagi ProfileRules'da - server bilan bir xil qoidalar bitta joyda.
        val nameValid: Boolean get() = ProfileRules.isNameValid(name)
        val usernameValid: Boolean get() = ProfileRules.isUsernameValid(username)
        val continueEnabled: Boolean get() = nameValid && usernameValid && !usernameTaken
    }

    /** Profil saqlangach asosiy qismga (Chats) o'tish. */
    interface Directions {
        suspend fun navigateToChats()
    }
}
