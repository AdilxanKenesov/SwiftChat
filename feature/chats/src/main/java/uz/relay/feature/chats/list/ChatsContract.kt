package uz.relay.feature.chats.list

import org.orbitmvi.orbit.OrbitContainerHost
import uz.relay.core.common.result.AppError
import uz.relay.domain.model.ChatSummary
import uz.relay.domain.model.ChatType
import uz.relay.domain.model.ConnectionStatus
import uz.relay.domain.model.MuteDuration
import uz.relay.domain.model.User

interface ChatsContract {

    interface ViewModel : OrbitContainerHost<UiState, UiState, SideEffect> {
        fun onEventDispatcher(intent: Intent)
    }

    sealed interface Intent {
        /** Snackbar'dagi "Qayta urinish" — sync'ni qaytadan boshlash. */
        object OnRetrySync : Intent
        data class OnChatClick(val chatId: String) : Intent
        /** Qidiruv ikonkasi, FAB va bo'sh holatdagi "Yangi chat" — hammasi qidiruvga olib boradi (spec 3.5). */
        object OnSearchClick : Intent
        /** App bar'dagi o'z avatarim — mening profilim va sozlamalar. */
        object OnMyProfileClick : Intent
        /** Long-press sheet'idan: chatni tanlangan muddatga ovozsiz qilish. */
        data class OnMute(val chatId: String, val duration: MuteDuration) : Intent
        data class OnUnmute(val chatId: String) : Intent
    }

    sealed interface SideEffect {
        /** Faqat vaqtinchalik (retryable) xatolar ko'rsatiladi — ular uchun "Qayta urinish" ma'noli. */
        data class ShowError(val error: AppError) : SideEffect
        /** Bitta amal (masalan, ovozsiz qilish) bajarilmadi — "Qayta urinish" (sync) bu yerda ma'nosiz. */
        data class ShowActionError(val error: AppError) : SideEffect
    }

    data class UiState(
        val chats: List<ChatSummary> = emptyList(),
        /** `userId → ism`: guruhdagi "Malika: ..." prefiksi va SYSTEM xabar matnlari uchun. */
        val userNames: Map<String, String> = emptyMap(),
        /** App bar'dagi o'z avatarim uchun. */
        val me: User? = null,
        /** Sarlavhadagi holat ("Yangilanmoqda…", "Ulanmoqda…") va "Internet aloqasi yoʻq" banneri uchun. */
        val connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTED,
        /** `chatId → hozir yozayotgan userId'lar` (o'zim kirmayman). Oxirgi xabar o'rnida "yozmoqda…". */
        val typing: Map<String, Set<String>> = emptyMap(),
        val isBootstrapped: Boolean = false
    ) {
        /** Baza hali birinchi marta to'ldirilmagan — "chat yo'q" emas, balki "hali yuklanmagan". */
        val showSkeleton: Boolean get() = chats.isEmpty() && !isBootstrapped

        /** To'liq sync bo'lgan va haqiqatan chat yo'q. */
        val showEmpty: Boolean get() = chats.isEmpty() && isBootstrapped

        fun chatsFor(tab: ChatTab): List<ChatSummary> = chats.filter(tab::accepts)

        /** Tab belgisidagi son: shu filtrdagi o'qilmagan xabari bor chatlar soni (xabarlar soni emas). */
        fun unreadChatsIn(tab: ChatTab): Int = chats.count { tab.accepts(it) && it.unreadCount > 0 }
    }

    interface Directions {
        suspend fun navigateToChat(chatId: String)
        suspend fun navigateToSearch()
        suspend fun navigateToMyProfile()
    }
}

/** Telegram'dagi papkalar kabi tablar: chat turi bo'yicha filtr. */
enum class ChatTab {
    ALL, DIRECT, GROUPS;

    fun accepts(chat: ChatSummary): Boolean = when (this) {
        ALL -> true
        DIRECT -> chat.type == ChatType.DIRECT
        GROUPS -> chat.type == ChatType.GROUP
    }
}
