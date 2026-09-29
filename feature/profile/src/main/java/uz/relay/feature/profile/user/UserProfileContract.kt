package uz.relay.feature.profile.user

import org.orbitmvi.orbit.OrbitContainerHost
import uz.relay.core.common.result.AppError
import uz.relay.domain.model.ChatSummary
import uz.relay.domain.model.User

interface UserProfileContract {

    interface ViewModel : OrbitContainerHost<UiState, UiState, SideEffect> {
        fun onEventDispatcher(intent: Intent)
    }

    sealed interface Intent {
        object OnBack : Intent
        /** "Xabar" — shu odam bilan shaxsiy chat (bo'lmasa yaratiladi). */
        object OnMessage : Intent
        object OnToggleMute : Intent
    }

    sealed interface SideEffect {
        data class ShowError(val error: AppError) : SideEffect
    }

    data class UiState(
        val user: User? = null,
        /** Shu odam bilan shaxsiy chat. Hali yozishilmagan bo'lsa `null`. */
        val chat: ChatSummary? = null,
        /** Biror amal bajarilmoqda (ikki marta bosishdan himoya). */
        val isBusy: Boolean = false
    ) {
        val muted: Boolean get() = chat?.muted == true
    }

    interface Directions {
        suspend fun back()
        /** Chat stekda bo'lsa (chat → profil) — unga qaytadi, aks holda ochadi. */
        suspend fun navigateToChat(chatId: String)
    }
}
