package uz.relay.feature.conversation.chat

import org.orbitmvi.orbit.OrbitContainerHost
import uz.relay.core.common.result.AppError
import uz.relay.domain.model.Attachment
import uz.relay.domain.model.CallLogFormat
import uz.relay.domain.model.ChatSummary
import uz.relay.domain.model.ChatType
import uz.relay.domain.model.GroupPermissions
import uz.relay.domain.model.MemberRole
import uz.relay.domain.model.Message
import uz.relay.domain.model.MessageType

/**
 * Chat ekranining Orbit MVI shartnomasi: bitta [UiState], foydalanuvchi harakatlari [Intent] va bir martalik
 * [SideEffect]lar.
 *
 * Nega shunday: ekran holati bitta immutable obyektda bo'lgani uchun UI har doim izchil chiziladi va
 * process death/rotatsiyada tiklash oson. Xato ko'rsatish yoki fayl ochish kabi "bir marta bo'ladigan" ishlar
 * state'ga emas, SideEffect'ga qo'yiladi — aks holda qayta chizishda takrorlanib qolardi. Navigatsiya esa
 * [Directions] interfeysi orqali: ViewModel navigator tafsilotlarini bilmaydi va testda oson almashtiriladi.
 */
interface ChatContract {

    /** Screen faqat shu interfeysni ko'radi: state/sideEffect oqimi va yagona kirish nuqtasi [onEventDispatcher]. */
    interface ViewModel : OrbitContainerHost<UiState, UiState, SideEffect> {
        fun onEventDispatcher(intent: Intent)
    }

    /** Ekrandan ViewModel'ga keladigan barcha foydalanuvchi harakatlari. */
    sealed interface Intent {
        object OnBack : Intent
        data class OnTextChange(val text: String) : Intent
        object OnSend : Intent
        data class OnReply(val message: Message) : Intent
        data class OnEdit(val message: Message) : Intent
        object OnCancelComposerMode : Intent
        /** Tasdiqlash dialogidan keyin keladi. */
        data class OnDelete(val message: Message) : Intent
        data class OnRetry(val message: Message) : Intent
        /** Ro'yxat yuqorisiga (eski xabarlarga) yaqinlashildi. */
        object OnLoadOlder : Intent
        /** Ro'yxat pastida turibmiz — ko'rinib turgan xabarlar o'qildi. */
        object OnBottomVisible : Intent
        /** Sarlavha yoki "ko'proq" bosildi — guruhda guruh ma'lumoti ochiladi. */
        object OnOpenInfo : Intent
        /** Galereya/kamera/fayldan tanlandi. Maydondagi matn rasm/videoga izoh bo'lib ketadi. */
        data class OnAttach(val attachment: Attachment) : Intent
        data class OnCancelUpload(val message: Message) : Intent
        /** Rasm/video — ko'ruvchi; fayl — yuklab olib, tizimdagi mos ilovada ochish. */
        data class OnMediaClick(val message: Message) : Intent
        /** Sarlavhadagi 📞 (audio) yoki 🎥 (video) — suhbatdoshga qo'ng'iroq. */
        data class OnStartCall(val video: Boolean) : Intent
        /** Guruhda 🎥, "Qo'shilish" banneri yoki video chat yozuvi — guruh video chatini boshlash yoki unga qo'shilish. */
        object OnGroupCall : Intent
    }

    /** Bir martalik hodisalar: Screen ularni `collectSideEffect` bilan tutib, Snackbar/Intent'ga aylantiradi. */
    sealed interface SideEffect {
        data class ShowError(val error: AppError) : SideEffect
        /** Yuklab olingan faylni boshqa ilovada ochish (FileProvider orqali — bu UI qatlami ishi). */
        data class OpenFile(val path: String, val mimeType: String) : SideEffect
    }

    /** Chat ekranining to'liq holati; hisoblanadigan bayroqlar (isGroup, canSend...) shu yerda getter sifatida. */
    data class UiState(
        /** Sarlavha uchun (ism, avatar, online). Chat bazada hali bo'lmasa `null`. */
        val chat: ChatSummary? = null,
        val items: List<ChatItem> = emptyList(),
        val userNames: Map<String, String> = emptyMap(),
        val myUserId: String? = null,
        /** Hozir shu chatda yozayotganlar (o'zimsiz). */
        val typingUserIds: Set<String> = emptySet(),
        val composerText: String = "",
        val composerMode: ComposerMode = ComposerMode.None,
        val hasMore: Boolean = true,
        val isLoadingOlder: Boolean = false,
        /** Guruh a'zolari soni (sarlavhadagi "12 aʼzo"). DIRECT chatda 0. */
        val memberCount: Int = 0,
        /** Guruhdagi rolim — boshqalarning xabarini o'chirish huquqi shunga bog'liq. */
        val myRole: MemberRole? = null,
        /** `clientMessageId → 0..1`: hozir yuklab olinayotgan fayllar. */
        val fileDownloads: Map<String, Float> = emptyMap(),
        /** Fayl tayyorlanmoqda (nusxalash/hash) — katta videoda bir necha soniya. */
        val isPreparingMedia: Boolean = false,
        /** Qo'ng'iroq yaratilmoqda — tugma ikki marta bosilsa ikkinchi qo'ng'iroq boshlanmasin. */
        val isStartingCall: Boolean = false,
        /** Guruh video chatida hozir nechta odam bor (0 — video chat yo'q, banner ko'rinmaydi). */
        val groupCallCount: Int = 0,
        /** Guruh a'zolari id'lari — video chat xonasiga a'zo qilib qo'shish uchun. */
        val memberIds: List<String> = emptyList()
    ) {
        val isGroup: Boolean get() = chat?.type == ChatType.GROUP
        val canSend: Boolean get() = composerText.isNotBlank()
        val canDeleteOthers: Boolean get() = isGroup && GroupPermissions.canDeleteOthersMessages(myRole)
    }

    /** Yozish panelining rejimi: oddiy, javob berish yoki tahrirlash. */
    sealed interface ComposerMode {
        object None : ComposerMode
        data class Reply(val message: Message) : ComposerMode
        data class Edit(val message: Message) : ComposerMode
    }

    /**
     * Chat'dan chiqish yo'llari: orqaga, guruh ma'lumoti, foydalanuvchi profili va media ko'ruvchi.
     * Amalga oshirilishi — [ChatDirectionsImpl] (AppNavigator event bus orqali).
     */
    interface Directions {
        suspend fun back()
        suspend fun navigateToGroupInfo(chatId: String)
        suspend fun navigateToUserProfile(userId: String)
        suspend fun navigateToMediaViewer(chatId: String, clientMessageId: String)
        suspend fun navigateToCall(callId: String, video: Boolean, chatId: String)
        suspend fun navigateToGroupCall(callId: String, chatId: String)
    }
}

/** Server qoidasi: tahrirlash faqat yuboruvchiga va createdAt'dan 48 soat ichida. */
private const val EDIT_WINDOW_MS = 48L * 60 * 60 * 1000

/** "Tahrirlash" menyuda ko'rinadimi (spec 3.8: o'zimniki, < 48 soat). Faqat serverga yetgan matnli xabar. */
fun Message.canEdit(now: Long = System.currentTimeMillis()): Boolean =
    isMine && !isDeleted && serverId != null && type == MessageType.TEXT && now - createdAt < EDIT_WINDOW_MS &&
        // Qo'ng'iroq yozuvi matn bo'lib saqlanadi, lekin uni tahrirlash formatni buzadi.
        CallLogFormat.parse(text) == null

/**
 * "Oʻchirish" menyuda ko'rinadimi. Server qoidasi: yuboruvchi har doim, guruhda OWNER/ADMIN ham
 * ([canDeleteOthers] — guruhdagi rolimdan hisoblanadi).
 */
fun Message.canDelete(canDeleteOthers: Boolean): Boolean =
    !isDeleted && serverId != null && (isMine || canDeleteOthers)

/** Javob berish va nusxalash: o'chirilmagan, serverga yetgan har qanday oddiy xabar. */
fun Message.canReply(): Boolean = !isDeleted && serverId != null && type != MessageType.SYSTEM
