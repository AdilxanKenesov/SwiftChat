package uz.relay.feature.conversation.chat

import org.orbitmvi.orbit.OrbitContainerHost
import uz.relay.core.common.result.AppError
import uz.relay.domain.model.ChatSummary
import uz.relay.domain.model.ChatType
import uz.relay.domain.model.Message
import uz.relay.domain.model.MessageType

interface ChatContract {

    interface ViewModel : OrbitContainerHost<UiState, UiState, SideEffect> {
        fun onEventDispatcher(intent: Intent)
    }

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
    }

    sealed interface SideEffect {
        data class ShowError(val error: AppError) : SideEffect
    }

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
        val isLoadingOlder: Boolean = false
    ) {
        val isGroup: Boolean get() = chat?.type == ChatType.GROUP
        val canSend: Boolean get() = composerText.isNotBlank()
    }

    /** Yozish panelining rejimi: oddiy, javob berish yoki tahrirlash. */
    sealed interface ComposerMode {
        object None : ComposerMode
        data class Reply(val message: Message) : ComposerMode
        data class Edit(val message: Message) : ComposerMode
    }

    interface Directions {
        suspend fun back()
    }
}

/** Server qoidasi: tahrirlash faqat yuboruvchiga va createdAt'dan 48 soat ichida. */
private const val EDIT_WINDOW_MS = 48L * 60 * 60 * 1000

/** "Tahrirlash" menyuda ko'rinadimi (spec 3.8: o'zimniki, < 48 soat). Faqat serverga yetgan matnli xabar. */
fun Message.canEdit(now: Long = System.currentTimeMillis()): Boolean =
    isMine && !isDeleted && serverId != null && type == MessageType.TEXT && now - createdAt < EDIT_WINDOW_MS

/**
 * "Oʻchirish" menyuda ko'rinadimi. Server qoidasi: yuboruvchi har doim, guruhda OWNER/ADMIN ham.
 * Guruhdagi rolim a'zolar ro'yxati bilan (guruh bosqichida) bilinadi — hozircha faqat o'zimnikini.
 */
fun Message.canDelete(): Boolean = isMine && !isDeleted && serverId != null

/** Javob berish va nusxalash: o'chirilmagan, serverga yetgan har qanday oddiy xabar. */
fun Message.canReply(): Boolean = !isDeleted && serverId != null && type != MessageType.SYSTEM
