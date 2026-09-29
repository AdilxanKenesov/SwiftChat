package uz.relay.feature.profile.edit

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.blockingIntent
import org.orbitmvi.orbit.viewmodel.orbitContainer
import uz.relay.core.common.result.AppError
import uz.relay.core.common.result.AppResult
import uz.relay.core.common.result.ErrorCodes
import uz.relay.domain.model.ProfileRules
import uz.relay.domain.usecase.user.ObserveMeUseCase
import uz.relay.domain.usecase.user.UpdateProfileUseCase
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val observeMe: ObserveMeUseCase,
    private val updateProfile: UpdateProfileUseCase,
    private val directions: EditProfileContract.Directions
) : ViewModel(), EditProfileContract.ViewModel {

    override val container =
        orbitContainer<EditProfileContract.UiState, EditProfileContract.SideEffect>(EditProfileContract.UiState()) {
            loadInitial()
        }

    override fun onEventDispatcher(intent: EditProfileContract.Intent) {
        when (intent) {
            EditProfileContract.Intent.OnBack -> intent { directions.back() }
            is EditProfileContract.Intent.OnNameChange -> setName(intent.name)
            is EditProfileContract.Intent.OnUsernameChange -> setUsername(intent.username)
            EditProfileContract.Intent.OnSave -> save()
        }
    }

    /**
     * Maydonlar bir marta, keshdagi profildan to'ldiriladi. Kuzatib turilmaydi: foydalanuvchi yozayotgan
     * paytda sync kelib maydonni ustidan yozib yubormasligi kerak.
     */
    private fun loadInitial() = intent {
        val me = observeMe().filterNotNull().first()
        val username = me.username.orEmpty()
        reduce {
            state.copy(
                loaded = true,
                userId = me.id,
                name = me.displayName,
                username = username,
                initialName = me.displayName,
                initialUsername = username
            )
        }
    }

    // Matn kiritish sinxron yangilanishi kerak, aks holda tez yozganda kursor sakraydi.
    private fun setName(name: String) = blockingIntent {
        reduce { state.copy(name = name.take(ProfileRules.NAME_MAX)) }
    }

    /** Ruxsat etilmagan belgilar yozilmaydi — server qoidasi (`^[a-zA-Z0-9_]+$`) bilan bir xil. */
    private fun setUsername(username: String) = blockingIntent {
        reduce {
            state.copy(
                username = username.filter { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '_' }
                    .take(ProfileRules.USERNAME_MAX),
                usernameTaken = false
            )
        }
    }

    private fun save() = intent {
        if (!state.saveEnabled || state.saving) return@intent
        reduce { state.copy(saving = true) }

        // Server javobi keshga yoziladi — profil va chatlar sarlavhasi o'zi yangilanadi.
        when (val result = updateProfile(displayName = state.name.trim(), username = state.username)) {
            is AppResult.Success -> {
                reduce { state.copy(saving = false) }
                directions.back()
            }

            is AppResult.Error -> {
                val error = result.error
                if (error is AppError.Api && error.code == ErrorCodes.USERNAME_TAKEN) {
                    reduce { state.copy(saving = false, usernameTaken = true) }
                } else {
                    reduce { state.copy(saving = false) }
                    postSideEffect(EditProfileContract.SideEffect.ShowError(error))
                }
            }
        }
    }
}
