package uz.relay.feature.conversation.search

import org.orbitmvi.orbit.OrbitContainerHost
import uz.relay.domain.model.Message

/**
 * Chat ichidagi qidiruv ekranining Orbit MVI shartnomasi. Guruh ma'lumoti / profil ekranidagi "Qidirish"dan
 * ochiladi; natija bosilsa chat shu xabarga scroll qilingan holda ochiladi.
 * SideEffect bo'sh — bu ekranda Snackbar yoki tashqi Intent yo'q, lekin shartnoma shakli boshqa ekranlar bilan bir xil qoladi.
 */
interface ChatSearchContract {

    /** Screen ko'radigan ViewModel interfeysi: state oqimi va yagona [onEventDispatcher]. */
    interface ViewModel : OrbitContainerHost<UiState, UiState, SideEffect> {
        fun onEventDispatcher(intent: Intent)
    }

    /** Foydalanuvchi harakatlari: orqaga, so'rov o'zgarishi, tozalash, natijani bosish. */
    sealed interface Intent {
        object OnBack : Intent
        data class OnQueryChange(val query: String) : Intent
        object OnClear : Intent
        data class OnResultClick(val message: Message) : Intent
    }

    sealed interface SideEffect

    /** So'rov, natijalar va ismlar keshi (natija qatorida yuboruvchi ismi uchun). */
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

    /** Qidiruvdan chiqish yo'llari — [ChatSearchDirectionsImpl]da AppNavigator orqali amalga oshadi. */
    interface Directions {
        suspend fun back()
        /** Chatni ochib, shu xabarga scroll qiladi. */
        suspend fun openMessage(chatId: String, clientMessageId: String)
    }
}
