package uz.relay.feature.group.info

import org.orbitmvi.orbit.OrbitContainerHost
import uz.relay.core.common.result.AppError
import uz.relay.domain.model.ChatMember
import uz.relay.domain.model.ChatSummary
import uz.relay.domain.model.GroupPermissions
import uz.relay.domain.model.MemberRole

interface GroupInfoContract {

    interface ViewModel : OrbitContainerHost<UiState, UiState, SideEffect> {
        fun onEventDispatcher(intent: Intent)
    }

    sealed interface Intent {
        object OnBack : Intent
        object OnToggleMute : Intent
        object OnSearch : Intent
        object OnAddMembers : Intent
        data class OnRename(val title: String) : Intent
        data class OnChangeRole(val member: ChatMember, val role: MemberRole) : Intent
        data class OnRemove(val member: ChatMember) : Intent
        data class OnWriteMessage(val member: ChatMember) : Intent
        object OnLeave : Intent
    }

    sealed interface SideEffect {
        data class ShowError(val error: AppError) : SideEffect
    }

    data class UiState(
        val chat: ChatSummary? = null,
        val members: List<ChatMember> = emptyList(),
        /** Biror amal bajarilmoqda (ikki marta bosishdan himoya). */
        val isBusy: Boolean = false
    ) {
        /** Mening rolim — tugmalarni ko'rsatish/yashirish shunga bog'liq. */
        val myRole: MemberRole? get() = members.firstOrNull { it.isMe }?.role
        val canManage: Boolean get() = GroupPermissions.canManage(myRole)
        val onlineCount: Int get() = members.count { it.online }
    }

    interface Directions {
        suspend fun back()
        suspend fun navigateToAddMembers(chatId: String)
        suspend fun navigateToChatSearch(chatId: String)
        suspend fun navigateToChat(chatId: String)
        /** Guruhdan chiqildi — chatlar ro'yxatiga qaytish (guruh chati endi yo'q). */
        suspend fun backToChats()
    }
}
