package uz.relay.feature.chats.addcontact

import org.orbitmvi.orbit.OrbitContainerHost
import uz.relay.core.common.result.AppError
import uz.relay.domain.model.User

/**
 * "Yangi kontakt": username bo'yicha topib, qurilmadagi kontaktlarga qo'shish. Relay'da telefon bo'yicha
 * qidiruv yo'q — shuning uchun Telegram'dagi "telefon raqami bilan qo'shish" o'rniga username.
 */
interface AddContactContract {

    interface ViewModel : OrbitContainerHost<UiState, UiState, SideEffect> {
        fun onEventDispatcher(intent: Intent)
    }

    sealed interface Intent {
        data class OnQueryChange(val query: String) : Intent
        object OnClear : Intent
        object OnBack : Intent
        data class OnAdd(val user: User) : Intent
        /** Qatorning o'zini bosish — profil (u yerdan ham qo'shish/yozish mumkin). */
        data class OnUserClick(val user: User) : Intent
    }

    sealed interface SideEffect {
        data class ShowError(val error: AppError) : SideEffect
        data class Added(val name: String) : SideEffect
    }

    data class UiState(
        val query: String = "",
        val results: List<User> = emptyList(),
        val searchedQuery: String? = null,
        val isSearching: Boolean = false,
        /** Allaqachon kontakt bo'lganlar — natijada "Qo'shish" o'rniga ✓. */
        val contactIds: Set<String> = emptySet(),
        /** Hozir qo'shilayotgan (profil yuklanmoqda) — tugma qayta bosilmasin. */
        val addingUserId: String? = null
    ) {
        val normalizedQuery: String get() = query.trim().removePrefix("@")
        val showNothingFound: Boolean
            get() = results.isEmpty() && normalizedQuery.isNotEmpty() && searchedQuery == normalizedQuery && !isSearching
    }

    interface Directions {
        suspend fun back()
        suspend fun navigateToUserProfile(userId: String)
    }
}
