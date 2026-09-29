package uz.relay.feature.conversation.search

import org.orbitmvi.orbit.OrbitContainerHost
import uz.relay.domain.model.Message

interface ChatSearchContract {

    interface ViewModel : OrbitContainerHost<UiState, UiState, SideEffect> {
        fun onEventDispatcher(intent: Intent)
    }

    sealed interface Intent {
        object OnBack : Intent
        data class OnQueryChange(val query: String) : Intent
        object OnClear : Intent
        data class OnResultClick(val message: Message) : Intent
    }

    sealed interface SideEffect

    data class UiState(
        val query: String = "",
        val results: List<Message> = emptyList(),
        /** Oxirgi tugagan qidiruv qaysi so'rov uchun — "Hech narsa topilmadi" faqat o'shanda ko'rinadi. */
        val searchedQuery: String? = null,
        val userNames: Map<String, String> = emptyMap()
    ) {
        val showNothingFound: Boolean
            get() = results.isEmpty() && query.isNotBlank() && searchedQuery == query.trim()
    }

    interface Directions {
        suspend fun back()
        /** Chatni ochib, shu xabarga scroll qiladi. */
        suspend fun openMessage(chatId: String, clientMessageId: String)
    }
}
