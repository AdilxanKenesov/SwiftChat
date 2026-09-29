package uz.relay.feature.chats.search

import org.orbitmvi.orbit.OrbitContainerHost
import uz.relay.core.common.result.AppError
import uz.relay.domain.model.User

interface SearchContract {

    interface ViewModel : OrbitContainerHost<UiState, UiState, SideEffect> {
        fun onEventDispatcher(intent: Intent)
    }

    sealed interface Intent {
        data class OnQueryChange(val query: String) : Intent
        object OnClear : Intent
        object OnBack : Intent
        object OnNewGroup : Intent
        data class OnUserClick(val user: User) : Intent
    }

    sealed interface SideEffect {
        data class ShowError(val error: AppError) : SideEffect
    }

    data class UiState(
        val query: String = "",
        val results: List<User> = emptyList(),
        /** Oxirgi tugagan qidiruv qaysi so'rov uchun edi — "Hech kim topilmadi" faqat o'shanda ko'rsatiladi. */
        val searchedQuery: String? = null,
        val isSearching: Boolean = false,
        /** Chat ochilayotgan odam — ikki marta bosishdan himoya. */
        val openingUserId: String? = null
    ) {
        /** So'rovdagi username qismi ("@ali" → "ali") — natijada qalin qilib ko'rsatiladi. */
        val normalizedQuery: String get() = query.trim().removePrefix("@")

        val showNothingFound: Boolean
            get() = results.isEmpty() && normalizedQuery.isNotEmpty() && searchedQuery == normalizedQuery && !isSearching
    }

    interface Directions {
        suspend fun back()
        suspend fun navigateToChat(chatId: String)
        suspend fun navigateToGroupCreate()
    }
}
