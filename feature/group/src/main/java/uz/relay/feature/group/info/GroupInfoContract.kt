package uz.relay.feature.group.info

import org.orbitmvi.orbit.OrbitContainerHost
import uz.relay.core.common.result.AppError
import uz.relay.domain.model.ChatMember
import uz.relay.domain.model.ChatSummary
import uz.relay.domain.model.GroupPermissions
import uz.relay.domain.model.MemberRole

/**
 * Guruh ma'lumotlari ekranining Orbit MVI shartnomasi (contract).
 *
 * Ekran chat ekranidagi sarlavha bosilganda ochiladi: guruh nomi, a'zolar ro'yxati, ovozsiz/qidiruv/qo'shish
 * tugmalari va a'zolar bilan ishlash (rol berish, chiqarish). Hamma tur — Intent, UiState, SideEffect, Directions —
 * bitta interfeysda, shuning uchun ekran mantiqini bir joydan ko'rish mumkin.
 */
interface GroupInfoContract {

    /** UI ViewModel bilan faqat shu interfeys orqali gaplashadi: holat oqimi + bitta kirish nuqtasi [onEventDispatcher]. */
    interface ViewModel : OrbitContainerHost<UiState, UiState, SideEffect> {
        fun onEventDispatcher(intent: Intent)
    }

    /** Foydalanuvchi harakatlari. Tasdiqlash dialoglari UI'da; ViewModel'ga faqat tasdiqlangan amal keladi. */
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

    /** Bir martalik hodisalar (snackbar) — holatda saqlanmaydi. */
    sealed interface SideEffect {
        data class ShowError(val error: AppError) : SideEffect
    }

    /** Ekranning yagona o'zgarmas holati: chat va a'zolar lokal bazadan keladi, qolgan maydonlar ulardan hisoblanadi. */
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

    /** Ekrandan chiqish yo'llari — ViewModel qaysi Nav3 kalit ishlatilishini bilmaydi, test uchun almashtirish oson. */
    interface Directions {
        suspend fun back()
        suspend fun navigateToAddMembers(chatId: String)
        suspend fun navigateToChatSearch(chatId: String)
        suspend fun navigateToChat(chatId: String)
        /** Guruhdan chiqildi — chatlar ro'yxatiga qaytish (guruh chati endi yo'q). */
        suspend fun backToChats()
    }
}
