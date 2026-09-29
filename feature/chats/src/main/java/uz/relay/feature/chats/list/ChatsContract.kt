package uz.relay.feature.chats.list

import org.orbitmvi.orbit.OrbitContainerHost
import uz.relay.core.common.result.AppError
import uz.relay.domain.model.ChatSummary
import uz.relay.domain.model.ChatType
import uz.relay.domain.model.ConnectionStatus
import uz.relay.domain.model.MuteDuration
import uz.relay.domain.model.User

/**
 * Chatlar ro'yxati ekranining "shartnomasi" (Contract): Intent, UiState, SideEffect va Directions shu yerda
 * bitta interfeys ichida guruhlangan. Sabab — ekran haqidagi hamma narsa bir faylda ko'rinadi, Screen va
 * ViewModel faqat shu shartnomaga tayanadi (Uzum uslubidagi Orbit MVI).
 *
 * Oqim: login/splash'dan keyin ilova shu ekranga keladi; bu yerdan chat, qidiruv (yangi chat/guruh)
 * va o'z profilimga o'tiladi.
 */
interface ChatsContract {

    /**
     * Orbit MVI: bitta o'zgarmas [UiState], UI'dan keladigan [Intent]'lar va bir martalik [SideEffect]'lar.
     * Holat ViewModel'da yashaydi — ekran burilsa (config change) yo'qolmaydi, test qilish ham oson.
     */
    interface ViewModel : OrbitContainerHost<UiState, UiState, SideEffect> {
        fun onEventDispatcher(intent: Intent)
    }

    /** Foydalanuvchi harakatlari — UI faqat shularni `onEventDispatcher` orqali yuboradi. */
    sealed interface Intent {
        /** Snackbar'dagi "Qayta urinish" — sync'ni qaytadan boshlash. */
        object OnRetrySync : Intent
        /** Qatorga bosish — chat ekrani ochiladi. */
        data class OnChatClick(val chatId: String) : Intent
        /** App bar'dagi 🔍 — chatlar va foydalanuvchilar bo'yicha qidiruv. */
        object OnSearchClick : Intent
        /**
         * FAB (✎) va bo'sh holatdagi "Yangi chat" — "Yangi xabar" ekrani (yangi guruh, yangi kontakt, kontaktlar).
         * Telegram'dagidek: qidiruv va yangi suhbat boshlash — ikki xil vazifa, ikki xil ekran.
         */
        object OnNewMessageClick : Intent
        /** App bar'dagi o'z avatarim — mening profilim va sozlamalar. */
        object OnMyProfileClick : Intent
        /** Long-press sheet'idan: chatni tanlangan muddatga ovozsiz qilish. */
        data class OnMute(val chatId: String, val duration: MuteDuration) : Intent
        /** Long-press sheet'idan: ovozni qayta yoqish. */
        data class OnUnmute(val chatId: String) : Intent
    }

    /** Bir martalik hodisalar (snackbar) — holatga yozilmaydi, aks holda burilishda qayta ko'rinardi. */
    sealed interface SideEffect {
        /** Faqat vaqtinchalik (retryable) xatolar ko'rsatiladi — ular uchun "Qayta urinish" ma'noli. */
        data class ShowError(val error: AppError) : SideEffect
        /** Bitta amal (masalan, ovozsiz qilish) bajarilmadi — "Qayta urinish" (sync) bu yerda ma'nosiz. */
        data class ShowActionError(val error: AppError) : SideEffect
    }

    /** Ekranning to'liq, o'zgarmas holati. Hammasi lokal bazadan (offline-first) keladi. */
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
        /** Birinchi to'liq sync tugaganmi — skeleton va "chat yo'q" holatlarini ajratish uchun. */
        val isBootstrapped: Boolean = false
    ) {
        /** Baza hali birinchi marta to'ldirilmagan — "chat yo'q" emas, balki "hali yuklanmagan". */
        val showSkeleton: Boolean get() = chats.isEmpty() && !isBootstrapped

        /** To'liq sync bo'lgan va haqiqatan chat yo'q. */
        val showEmpty: Boolean get() = chats.isEmpty() && isBootstrapped

        /** Tanlangan tabga mos chatlar (filtr UI'da emas, holatda — Preview va testda ham bir xil ishlaydi). */
        fun chatsFor(tab: ChatTab): List<ChatSummary> = chats.filter(tab::accepts)

        /** Tab belgisidagi son: shu filtrdagi o'qilmagan xabari bor chatlar soni (xabarlar soni emas). */
        fun unreadChatsIn(tab: ChatTab): Int = chats.count { tab.accepts(it) && it.unreadCount > 0 }
    }

    /**
     * Navigatsiya abstraksiyasi: ViewModel qayerga o'tishni aytadi, qanday o'tishni (NavKey, back stack) bilmaydi.
     * Amalga oshirish — [ChatsDirectionsImpl]; testda soxta (fake) Directions berish oson.
     */
    interface Directions {
        suspend fun navigateToChat(chatId: String)
        suspend fun navigateToSearch()
        suspend fun navigateToNewMessage()
        suspend fun navigateToMyProfile()
    }
}

/** Telegram'dagi papkalar kabi tablar: chat turi bo'yicha filtr. */
enum class ChatTab {
    ALL, DIRECT, GROUPS;

    /** Chat shu tabga tegishlimi. */
    fun accepts(chat: ChatSummary): Boolean = when (this) {
        ALL -> true
        DIRECT -> chat.type == ChatType.DIRECT
        GROUPS -> chat.type == ChatType.GROUP
    }
}
