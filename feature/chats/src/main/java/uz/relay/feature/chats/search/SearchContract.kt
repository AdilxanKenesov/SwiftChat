package uz.relay.feature.chats.search

import org.orbitmvi.orbit.OrbitContainerHost
import uz.relay.core.common.result.AppError
import uz.relay.domain.model.ChatSummary
import uz.relay.domain.model.User

/**
 * Foydalanuvchi qidiruvi ekrani shartnomasi (Orbit MVI: Intent / UiState / SideEffect / Directions bir joyda).
 * Chatlar ro'yxatidagi qidiruv ikonkasidan ochiladi. Bu yerda o'z chatlarim (nomi bo'yicha) va username bo'yicha
 * odamlar qidiriladi; natijadan chat yoki odam bilan DIRECT chat ochiladi. Yangi guruh "Yangi xabar" ekranida.
 */
interface SearchContract {

    /** Orbit container egasi: UI holatni kuzatadi va Intent'larni [onEventDispatcher] orqali yuboradi. */
    interface ViewModel : OrbitContainerHost<UiState, UiState, SideEffect> {
        fun onEventDispatcher(intent: Intent)
    }

    /**
     * Foydalanuvchi harakatlari: matn o'zgarishi, tozalash, orqaga, natijadagi chat yoki odamga bosish.
     */
    sealed interface Intent {
        data class OnQueryChange(val query: String) : Intent
        object OnClear : Intent
        object OnBack : Intent
        data class OnChatClick(val chatId: String) : Intent
        data class OnUserClick(val user: User) : Intent
    }

    /** Bir martalik xato xabari (snackbar). */
    sealed interface SideEffect {
        data class ShowError(val error: AppError) : SideEffect
    }

    /** Qidiruv ekranining o'zgarmas holati. */
    data class UiState(
        val query: String = "",
        val results: List<User> = emptyList(),
        /** Lokal chatlar ichidan nomi so'rovga mos keladiganlar (tarmoqsiz, darhol). */
        val chatResults: List<ChatSummary> = emptyList(),
        /** Oxirgi tugagan qidiruv qaysi so'rov uchun edi — "Hech kim topilmadi" faqat o'shanda ko'rsatiladi. */
        val searchedQuery: String? = null,
        val isSearching: Boolean = false,
        /** Chat ochilayotgan odam — ikki marta bosishdan himoya. */
        val openingUserId: String? = null
    ) {
        /** So'rovdagi username qismi ("@ali" → "ali") — natijada qalin qilib ko'rsatiladi. */
        val normalizedQuery: String get() = query.trim().removePrefix("@")

        /**
         * "Hech kim topilmadi" faqat joriy so'rov bo'yicha qidiruv haqiqatan tugagan bo'lsa — debounce kutilayotganda
         * yoki eski so'rov natijasi turganda noto'g'ri bo'sh holat ko'rinmasin.
         */
        val showNothingFound: Boolean
            get() = results.isEmpty() && chatResults.isEmpty() && normalizedQuery.isNotEmpty() &&
                searchedQuery == normalizedQuery && !isSearching
    }

    /** Navigatsiya abstraksiyasi — ViewModel NavKey'larni bilmaydi, amalga oshirish [SearchDirectionsImpl]da. */
    interface Directions {
        suspend fun back()
        suspend fun navigateToChat(chatId: String)
    }
}
