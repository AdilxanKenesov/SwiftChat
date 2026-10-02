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

/**
 * Profil sozlash ekranining ViewModel'i: ism va username'ni tekshiradi, profilni serverga saqlaydi.
 *
 * Username band bo'lsa (409 USERNAME_TAKEN) xatoni snackbar emas, maydon ostida ko'rsatadi va
 * bosib tanlash mumkin bo'lgan muqobil variantlarni taklif qiladi.
 */
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

    // Matn kiritish sinxron yangilanishi kerak (blockingIntent), aks holda tez yozganda kursor sakraydi.
    private fun setName(name: String) = blockingIntent {
        reduce { state.copy(name = name.take(ProfileRules.NAME_MAX)) }
    }

    // Faqat lotin harflari, raqamlar va "_" qabul qilinadi; yangi qiymat kiritilgach "band" xatosi va takliflar tozalanadi.
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
                // Profil to'ldirildi: bayroqni olib tashlaymiz, keyin asosiy qismni ochamiz.
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

    /** Ikkita taklif, masalan `name_dev`, `name01`; asos 32 belgidan oshmasligi uchun qirqiladi. */
    private fun suggestionsFor(username: String): List<String> = listOf("_dev", "01")
        .map { suffix -> username.take(ProfileRules.USERNAME_MAX - suffix.length) + suffix }
        .filter(ProfileRules::isUsernameValid)
}
