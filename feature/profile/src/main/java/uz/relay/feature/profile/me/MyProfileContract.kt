package uz.relay.feature.profile.me

import org.orbitmvi.orbit.OrbitContainerHost
import uz.relay.core.common.result.AppError
import uz.relay.domain.model.ConnectionStatus
import uz.relay.domain.model.ThemeMode
import uz.relay.domain.model.User

interface MyProfileContract {

    interface ViewModel : OrbitContainerHost<UiState, UiState, SideEffect> {
        fun onEventDispatcher(intent: Intent)
    }

    sealed interface Intent {
        object OnBack : Intent
        object OnEdit : Intent
        data class OnNotificationsChange(val enabled: Boolean) : Intent
        /** Switch'ning yangi holati. Tanlov tizim temasidan ustun turadi (ThemeMode.LIGHT/DARK). */
        data class OnDarkModeChange(val enabled: Boolean) : Intent
        /** Tasdiq dialogidan keyin. */
        object OnLogout : Intent
    }

    sealed interface SideEffect {
        data class ShowError(val error: AppError) : SideEffect
    }

    data class UiState(
        val me: User? = null,
        /** Ism tagidagi holat: ulangan bo'lsam "online", aks holda "Ulanmoqda…" / "Internet aloqasi yoʻq". */
        val connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTED,
        val themeMode: ThemeMode = ThemeMode.SYSTEM,
        val notificationsEnabled: Boolean = true,
        /** Chiqish ketmoqda — tugma qayta bosilmasin, ichida progress. */
        val loggingOut: Boolean = false
    )

    // Logout'dan keyingi o'tish bu yerda yo'q: sessiya o'chishi bilan MainViewModel o'zi login ekraniga olib boradi.
    interface Directions {
        suspend fun back()
        suspend fun navigateToEditProfile()
    }
}
