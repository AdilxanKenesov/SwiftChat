package uz.relay.feature.auth.profile

import org.orbitmvi.orbit.OrbitContainerHost
import uz.relay.core.common.result.AppError
import uz.relay.domain.model.ProfileRules

interface ProfileSetupContract {

    interface ViewModel : OrbitContainerHost<UiState, UiState, SideEffect> {
        fun onEventDispatcher(intent: Intent)
    }

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
        /** Set after 409 USERNAME_TAKEN for the current [username]. */
        val usernameTaken: Boolean = false,
        val suggestions: List<String> = emptyList(),
        val saving: Boolean = false
    ) {
        val nameValid: Boolean get() = ProfileRules.isNameValid(name)
        val usernameValid: Boolean get() = ProfileRules.isUsernameValid(username)
        val continueEnabled: Boolean get() = nameValid && usernameValid && !usernameTaken
    }

    interface Directions {
        suspend fun navigateToChats()
    }
}
