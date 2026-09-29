package uz.relay.feature.group.info

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import uz.relay.core.common.result.AppResult
import uz.relay.domain.model.MemberRole
import uz.relay.domain.usecase.chat.ObserveChatUseCase
import uz.relay.domain.usecase.chat.OpenDirectChatUseCase
import uz.relay.domain.usecase.chat.SetChatMutedUseCase
import uz.relay.domain.usecase.group.ChangeMemberRoleUseCase
import uz.relay.domain.usecase.group.LeaveGroupUseCase
import uz.relay.domain.usecase.group.ObserveMembersUseCase
import uz.relay.domain.usecase.group.RefreshMembersUseCase
import uz.relay.domain.usecase.group.RemoveMemberUseCase
import uz.relay.domain.usecase.group.RenameGroupUseCase

/**
 * Guruh ma'lumotlari ekranining ViewModel'i (Orbit MVI).
 *
 * `chatId` Nav3 kalitidan runtime'da keladi, shuning uchun `@AssistedInject` + [Factory] ishlatiladi —
 * qolgan use case'larni Hilt o'zi beradi. Ma'lumot manbasi — lokal baza (Room): ekran uni kuzatadi, amallar esa
 * serverga yuboriladi va natija sinxronizatsiya orqali bazaga, u yerdan UI'ga qaytadi (yagona haqiqat manbasi).
 */
@HiltViewModel(assistedFactory = GroupInfoViewModel.Factory::class)
class GroupInfoViewModel @AssistedInject constructor(
    @Assisted private val chatId: String,
    private val observeChat: ObserveChatUseCase,
    private val observeMembers: ObserveMembersUseCase,
    private val refreshMembers: RefreshMembersUseCase,
    private val setChatMuted: SetChatMutedUseCase,
    private val renameGroup: RenameGroupUseCase,
    private val changeMemberRole: ChangeMemberRoleUseCase,
    private val removeMember: RemoveMemberUseCase,
    private val leaveGroup: LeaveGroupUseCase,
    private val openDirectChat: OpenDirectChatUseCase,
    private val directions: GroupInfoContract.Directions
) : ViewModel(), GroupInfoContract.ViewModel {

    /** Hilt generatsiya qiladigan factory: ekran `hiltViewModel(creationCallback = ...)` ichida chaqiradi. */
    @AssistedFactory
    interface Factory {
        fun create(chatId: String): GroupInfoViewModel
    }

    // Container yaratilganda: bazani kuzatish boshlanadi va a'zolar ro'yxati serverdan bir marta yangilanadi.
    override val container =
        orbitContainer<GroupInfoContract.UiState, GroupInfoContract.SideEffect>(GroupInfoContract.UiState()) {
            observeData()
            refresh()
        }

    /** UI'dan keladigan barcha Intent'lar uchun yagona kirish nuqtasi. */
    override fun onEventDispatcher(intent: GroupInfoContract.Intent) {
        when (intent) {
            GroupInfoContract.Intent.OnBack -> intent { directions.back() }
            GroupInfoContract.Intent.OnToggleMute -> toggleMute()
            GroupInfoContract.Intent.OnSearch -> intent { directions.navigateToChatSearch(chatId) }
            GroupInfoContract.Intent.OnAddMembers -> intent { directions.navigateToAddMembers(chatId) }
            is GroupInfoContract.Intent.OnRename -> action { renameGroup(chatId, intent.title) }
            is GroupInfoContract.Intent.OnChangeRole -> action { changeMemberRole(chatId, intent.member.userId, intent.role) }
            is GroupInfoContract.Intent.OnRemove -> action { removeMember(chatId, intent.member.userId) }
            is GroupInfoContract.Intent.OnWriteMessage -> writeMessage(intent.member.userId)
            GroupInfoContract.Intent.OnLeave -> leave()
        }
    }

    /** Ekran lokal bazani kuzatadi: a'zo qo'shilsa/chiqarilsa (o'zim yoki boshqa qurilma, socket) ro'yxat o'zi yangilanadi. */
    private fun observeData() = intent {
        // `repeatOnSubscription`: ekran ko'rinib turgandagina kuzatadi, fonda keraksiz ishlamaydi.
        repeatOnSubscription {
            combine(observeChat(chatId), observeMembers(chatId)) { chat, members -> chat to members }
                .collect { (chat, members) -> reduce { state.copy(chat = chat, members = members) } }
        }
    }

    /** A'zolar ro'yxatini yangilash (ADMIN/OWNER — serverdan to'liq, oddiy a'zo — tarixdan). */
    private fun refresh() = intent {
        when (val result = refreshMembers(chatId)) {
            is AppResult.Success -> Unit
            is AppResult.Error -> postSideEffect(GroupInfoContract.SideEffect.ShowError(result.error))
        }
    }

    /** Ovozsiz rejimni almashtiradi (joriy holat bazadagi chatdan olinadi). */
    private fun toggleMute() = intent {
        val muted = state.chat?.muted ?: return@intent
        runAction { setChatMuted(chatId, !muted) }
    }

    /** A'zo bilan shaxsiy chatni ochadi (bo'lmasa server yaratadi) va unga o'tadi. */
    private fun writeMessage(userId: String) = intent {
        when (val result = openDirectChat(userId)) {
            is AppResult.Success -> directions.navigateToChat(result.data)
            is AppResult.Error -> postSideEffect(GroupInfoContract.SideEffect.ShowError(result.error))
        }
    }

    /** Guruhdan chiqish: muvaffaqiyatda chatlar ro'yxatiga qaytiladi, chunki guruh chati endi mavjud emas. */
    private fun leave() = intent {
        if (state.isBusy) return@intent
        reduce { state.copy(isBusy = true) }
        when (val result = leaveGroup(chatId)) {
            is AppResult.Success -> {
                reduce { state.copy(isBusy = false) }
                directions.backToChats()
            }
            is AppResult.Error -> {
                reduce { state.copy(isBusy = false) }
                postSideEffect(GroupInfoContract.SideEffect.ShowError(result.error))
            }
        }
    }

    /** Oddiy amal: natija ro'yxatga baza orqali keladi, bu yerda faqat xato ko'rsatiladi. */
    private fun action(block: suspend () -> AppResult<Unit>) = intent { runAction(block) }

    /** `isBusy` bilan himoyalangan umumiy amal: ikki marta bosishda so'rov takrorlanmaydi, xato bo'lsa snackbar. */
    private suspend fun Syntax<GroupInfoContract.UiState, GroupInfoContract.SideEffect>.runAction(
        block: suspend () -> AppResult<Unit>
    ) {
        if (state.isBusy) return
        reduce { state.copy(isBusy = true) }
        val result = block()
        reduce { state.copy(isBusy = false) }
        if (result is AppResult.Error) postSideEffect(GroupInfoContract.SideEffect.ShowError(result.error))
    }
}
