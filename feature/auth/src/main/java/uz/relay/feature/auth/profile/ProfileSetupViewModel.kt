package uz.relay.feature.auth.profile

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.blockingIntent
import org.orbitmvi.orbit.viewmodel.orbitContainer
import uz.relay.core.common.result.AppError
import uz.relay.core.common.result.AppResult
import uz.relay.core.common.result.ErrorCodes
import uz.relay.domain.model.ProfileRules
import uz.relay.domain.usecase.auth.CompleteProfileSetupUseCase
import uz.relay.domain.usecase.user.UpdateProfileUseCase
import javax.inject.Inject

@HiltViewModel
class ProfileSetupViewModel @Inject constructor(
    private val updateProfile: UpdateProfileUseCase,
    private val completeProfileSetup: CompleteProfileSetupUseCase,
    private val directions: ProfileSetupContract.Directions
) : ViewModel(), ProfileSetupContract.ViewModel {

    override val container =
        orbitContainer<ProfileSetupContract.UiState, ProfileSetupContract.SideEffect>(ProfileSetupContract.UiState())

    override fun onEventDispatcher(intent: ProfileSetupContract.Intent) {
        when (intent) {
            is ProfileSetupContract.Intent.OnNameChange -> setName(intent.name)
            is ProfileSetupContract.Intent.OnUsernameChange -> setUsername(intent.username)
            is ProfileSetupContract.Intent.OnSuggestionClick -> setUsername(intent.username)
            ProfileSetupContract.Intent.OnContinue -> save()
        }
    }

    // Text input must update synchronously, otherwise fast typing makes the cursor jump.
    private fun setName(name: String) = blockingIntent {
        reduce { state.copy(name = name.take(ProfileRules.NAME_MAX)) }
    }

    private fun setUsername(username: String) = blockingIntent {
        reduce {
            state.copy(
                username = username.filter { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '_' }
                    .take(ProfileRules.USERNAME_MAX),
                usernameTaken = false,
                suggestions = emptyList()
            )
        }
    }

    private fun save() = intent {
        if (!state.continueEnabled || state.saving) return@intent
        reduce { state.copy(saving = true) }

        when (val result = updateProfile(displayName = state.name.trim(), username = state.username)) {
            is AppResult.Success -> {
                // The profile is complete: drop the flag, then open the main part.
                completeProfileSetup()
                reduce { state.copy(saving = false) }
                directions.navigateToChats()
            }

            is AppResult.Error -> {
                val error = result.error
                if (error is AppError.Api && error.code == ErrorCodes.USERNAME_TAKEN) {
                    reduce {
                        state.copy(saving = false, usernameTaken = true, suggestions = suggestionsFor(state.username))
                    }
                } else {
                    reduce { state.copy(saving = false) }
                    postSideEffect(ProfileSetupContract.SideEffect.ShowError(error))
                }
            }
        }
    }

    /** Two suggestions, e.g. `name_dev`, `name01`; the base is cut so they stay within 32 chars. */
    private fun suggestionsFor(username: String): List<String> = listOf("_dev", "01")
        .map { suffix -> username.take(ProfileRules.USERNAME_MAX - suffix.length) + suffix }
        .filter(ProfileRules::isUsernameValid)
}
