package uz.relay.feature.profile.edit

import org.orbitmvi.orbit.OrbitContainerHost
import uz.relay.core.common.result.AppError
import uz.relay.domain.model.ProfileRules

interface EditProfileContract {

    interface ViewModel : OrbitContainerHost<UiState, UiState, SideEffect> {
        fun onEventDispatcher(intent: Intent)
    }

    sealed interface Intent {
        object OnBack : Intent
        data class OnNameChange(val name: String) : Intent
        data class OnUsernameChange(val username: String) : Intent
        object OnSave : Intent
    }

    sealed interface SideEffect {
        data class ShowError(val error: AppError) : SideEffect
    }

    data class UiState(
        /** Profil keshdan o'qilib, maydonlar to'ldirildi. Unga qadar saqlash tugmasi o'chiq. */
        val loaded: Boolean = false,
        /** Avatar rangi uchun (u id'dan hisoblanadi — ism o'zgarsa ham rang o'zgarmaydi). */
        val userId: String = "",
        val name: String = "",
        val username: String = "",
        val initialName: String = "",
        val initialUsername: String = "",
        /** Joriy [username] uchun 409 USERNAME_TAKEN kelgan. Username o'zgarishi bilan tushadi. */
        val usernameTaken: Boolean = false,
        val saving: Boolean = false
    ) {
        val usernameValid: Boolean get() = ProfileRules.isUsernameValid(username)

        /** Hech narsa o'zgarmagan bo'lsa so'rov yuborishning ma'nosi yo'q. */
        private val changed: Boolean get() = name.trim() != initialName || username != initialUsername

        val saveEnabled: Boolean
            get() = loaded && ProfileRules.isNameValid(name) && usernameValid && !usernameTaken && changed
    }

    interface Directions {
        suspend fun back()
    }
}
