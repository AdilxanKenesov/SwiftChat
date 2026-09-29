package uz.relay.feature.chats.newmessage

import org.orbitmvi.orbit.OrbitContainerHost
import uz.relay.core.common.result.AppError
import uz.relay.domain.model.User

/**
 * "Yangi xabar" ekrani (chatlar ekranidagi FAB) — Telegram'dagi "New Message" kabi: yangi guruh, yangi kontakt
 * va kontaktlar ro'yxati. Kontaktni bosish — u bilan shaxsiy chat, long-press — kontaktni o'chirish.
 */
interface NewMessageContract {

    interface ViewModel : OrbitContainerHost<UiState, UiState, SideEffect> {
        fun onEventDispatcher(intent: Intent)
    }

    sealed interface Intent {
        object OnBack : Intent
        object OnNewGroup : Intent
        object OnNewContact : Intent
        data class OnContactClick(val user: User) : Intent
        /** Tasdiq dialogidan keyin. */
        data class OnRemoveContact(val user: User) : Intent
    }

    sealed interface SideEffect {
        data class ShowError(val error: AppError) : SideEffect
    }

    data class UiState(
        val contacts: List<User> = emptyList(),
        /** Kontaktlar birinchi marta bazadan o'qildimi — o'qilmaguncha "kontakt yo'q" ko'rsatilmaydi. */
        val isLoaded: Boolean = false,
        /** Chat ochilmoqda (ikki marta bosishdan himoya). */
        val openingUserId: String? = null
    )

    interface Directions {
        suspend fun back()
        suspend fun navigateToGroupCreate()
        suspend fun navigateToAddContact()
        suspend fun navigateToChat(chatId: String)
    }
}
