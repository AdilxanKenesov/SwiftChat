package uz.relay.feature.group.info

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.orbitmvi.orbit.test.test
import uz.relay.core.common.result.AppError
import uz.relay.core.common.result.AppResult
import uz.relay.domain.model.ChatType
import uz.relay.domain.model.MemberRole
import uz.relay.domain.testing.FakeChatRepository
import uz.relay.domain.testing.FakeGroupRepository
import uz.relay.domain.testing.MainDispatcherRule
import uz.relay.domain.testing.TestData
import uz.relay.domain.usecase.chat.ObserveChatUseCase
import uz.relay.domain.usecase.chat.OpenDirectChatUseCase
import uz.relay.domain.usecase.chat.SetChatMutedUseCase
import uz.relay.domain.usecase.group.ChangeMemberRoleUseCase
import uz.relay.domain.usecase.group.LeaveGroupUseCase
import uz.relay.domain.usecase.group.ObserveMembersUseCase
import uz.relay.domain.usecase.group.RefreshMembersUseCase
import uz.relay.domain.usecase.group.RemoveMemberUseCase
import uz.relay.domain.usecase.group.RenameGroupUseCase

class GroupInfoViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val chats = FakeChatRepository()
    private val groups = FakeGroupRepository()
    private val navigation = mutableListOf<String>()
    private val directions = object : GroupInfoContract.Directions {
        override suspend fun back() { navigation += "back" }
        override suspend fun navigateToAddMembers(chatId: String) { navigation += "add:$chatId" }
        override suspend fun navigateToChatSearch(chatId: String) { navigation += "search:$chatId" }
        override suspend fun navigateToChat(chatId: String) { navigation += "chat:$chatId" }
        override suspend fun backToChats() { navigation += "chats" }
    }
    private fun viewModel() = GroupInfoViewModel(
        chatId = "g1",
        observeChat = ObserveChatUseCase(chats),
        observeMembers = ObserveMembersUseCase(groups),
        refreshMembers = RefreshMembersUseCase(groups),
        setChatMuted = SetChatMutedUseCase(chats),
        renameGroup = RenameGroupUseCase(groups),
        changeMemberRole = ChangeMemberRoleUseCase(groups),
        removeMember = RemoveMemberUseCase(groups),
        leaveGroup = LeaveGroupUseCase(groups),
        openDirectChat = OpenDirectChatUseCase(chats),
        directions = directions
    )
    private val group = TestData.chat(id = "g1", type = ChatType.GROUP, title = "Oila", peerUserId = null)
    private val me = TestData.member("me", MemberRole.ADMIN, isMe = true)
    private val vali = TestData.member("vali")

    @Test
    fun `chat and members are observed`() = runTest {
        chats.chats.value = listOf(group)
        groups.members.value = mapOf("g1" to listOf(me, vali))
        viewModel().test(this) {
            expectInitialState()
            runOnCreate()
            val state = awaitState()
            assertEquals(group, state.chat)
            assertEquals(MemberRole.ADMIN, state.myRole)
            assertTrue(state.canManage)
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `rename role and remove go through busy guard`() = runTest {
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(GroupInfoContract.Intent.OnRename("Yangi nom"))
            expectState { copy(isBusy = true) }
            expectState { copy(isBusy = false) }
            containerHost.onEventDispatcher(GroupInfoContract.Intent.OnChangeRole(vali, MemberRole.ADMIN))
            expectState { copy(isBusy = true) }
            expectState { copy(isBusy = false) }
            containerHost.onEventDispatcher(GroupInfoContract.Intent.OnRemove(vali))
            expectState { copy(isBusy = true) }
            expectState { copy(isBusy = false) }
        }
        assertEquals(listOf("g1" to "Yangi nom"), groups.renamed)
        assertEquals(listOf(Triple("g1", "vali", MemberRole.ADMIN)), groups.roleChanges)
        assertEquals(listOf("g1" to "vali"), groups.removed)
    }

    @Test
    fun `failed action shows error`() = runTest {
        groups.actionResult = AppResult.Error(AppError.Network)
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(GroupInfoContract.Intent.OnRename("X"))
            expectState { copy(isBusy = true) }
            expectState { copy(isBusy = false) }
            expectSideEffect(GroupInfoContract.SideEffect.ShowError(AppError.Network))
        }
    }

    @Test
    fun `leave goes back to chats`() = runTest {
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(GroupInfoContract.Intent.OnLeave)
            expectState { copy(isBusy = true) }
            expectState { copy(isBusy = false) }
        }
        assertEquals(listOf("g1"), groups.left)
        assertEquals(listOf("chats"), navigation)
    }

    @Test
    fun `mute toggles current state`() = runTest {
        chats.chats.value = listOf(group)
        viewModel().test(this, GroupInfoContract.UiState(chat = group)) {
            expectInitialState()
            containerHost.onEventDispatcher(GroupInfoContract.Intent.OnToggleMute)
            expectState { copy(isBusy = true) }
            expectState { copy(isBusy = false) }
        }
        assertEquals(Triple("g1", true, null as Long?), chats.mutes.single())
    }

    @Test
    fun `write message opens direct chat`() = runTest {
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(GroupInfoContract.Intent.OnWriteMessage(vali))
            containerHost.onEventDispatcher(GroupInfoContract.Intent.OnSearch)
            containerHost.onEventDispatcher(GroupInfoContract.Intent.OnAddMembers)
        }
        assertEquals(listOf("chat:direct-vali", "search:g1", "add:g1"), navigation)
    }

    @Test
    fun `member cannot manage`() {
        assertFalse(GroupInfoContract.UiState(members = listOf(me.copy(role = MemberRole.MEMBER))).canManage)
    }
}
