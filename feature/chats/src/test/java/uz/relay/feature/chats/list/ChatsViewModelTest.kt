package uz.relay.feature.chats.list

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
import uz.relay.domain.model.MuteDuration
import uz.relay.domain.testing.FakeChatRepository
import uz.relay.domain.testing.FakeConnectionRepository
import uz.relay.domain.testing.FakeTypingRepository
import uz.relay.domain.testing.FakeUserRepository
import uz.relay.domain.testing.MainDispatcherRule
import uz.relay.domain.testing.TestData
import uz.relay.domain.usecase.chat.ObserveChatsUseCase
import uz.relay.domain.usecase.chat.ObserveConnectionStatusUseCase
import uz.relay.domain.usecase.chat.ObserveSyncStatusUseCase
import uz.relay.domain.usecase.chat.ObserveTypingUseCase
import uz.relay.domain.usecase.chat.RefreshChatsUseCase
import uz.relay.domain.usecase.chat.SetChatMutedUseCase
import uz.relay.domain.usecase.user.ObserveMeUseCase
import uz.relay.domain.usecase.user.ObserveUserNamesUseCase
import uz.relay.domain.usecase.user.RefreshMeUseCase

class ChatsViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val chats = FakeChatRepository()
    private val users = FakeUserRepository()
    private val connection = FakeConnectionRepository()
    private val typing = FakeTypingRepository()
    private val navigation = mutableListOf<String>()
    private val directions = object : ChatsContract.Directions {
        override suspend fun navigateToChat(chatId: String) { navigation += "chat:$chatId" }
        override suspend fun navigateToSearch() { navigation += "search" }
        override suspend fun navigateToNewMessage() { navigation += "new" }
        override suspend fun navigateToMyProfile() { navigation += "me" }
    }

    private fun viewModel() = ChatsViewModel(
        observeChats = ObserveChatsUseCase(chats),
        observeSyncStatus = ObserveSyncStatusUseCase(chats),
        observeUserNames = ObserveUserNamesUseCase(users),
        observeMe = ObserveMeUseCase(users),
        observeConnectionStatus = ObserveConnectionStatusUseCase(connection),
        observeTyping = ObserveTypingUseCase(typing),
        refreshChats = RefreshChatsUseCase(chats),
        refreshMe = RefreshMeUseCase(users),
        setChatMuted = SetChatMutedUseCase(chats),
        directions = directions
    )

    @Test
    fun `my own typing is hidden from the list`() = runTest {
        typing.current.value = mapOf("c1" to setOf("me", "vali"), "c2" to setOf("me"))
        viewModel().test(this) {
            expectInitialState()
            runOnCreate()
            val state = awaitState()
            assertEquals(mapOf("c1" to setOf("vali")), state.typing)
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `skeleton before bootstrap and empty state after`() {
        assertTrue(ChatsContract.UiState(isBootstrapped = false).showSkeleton)
        assertTrue(ChatsContract.UiState(isBootstrapped = true).showEmpty)
        assertFalse(ChatsContract.UiState(chats = listOf(TestData.chat()), isBootstrapped = false).showSkeleton)
    }

    @Test
    fun `tabs filter chats and count unread`() {
        val direct = TestData.chat(id = "d", type = ChatType.DIRECT).copy(unreadCount = 2)
        val group = TestData.chat(id = "g", type = ChatType.GROUP, peerUserId = null).copy(unreadCount = 0)
        val group2 = TestData.chat(id = "g2", type = ChatType.GROUP, peerUserId = null).copy(unreadCount = 5)
        val state = ChatsContract.UiState(chats = listOf(direct, group, group2))
        assertEquals(listOf(direct), state.chatsFor(ChatTab.DIRECT))
        assertEquals(listOf(group, group2), state.chatsFor(ChatTab.GROUPS))
        assertEquals(2, state.unreadChatsIn(ChatTab.ALL))
        assertEquals(1, state.unreadChatsIn(ChatTab.GROUPS))
    }

    @Test
    fun `retryable sync error is shown`() = runTest {
        chats.refreshResult = AppResult.Error(AppError.Network)
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(ChatsContract.Intent.OnRetrySync)
            expectSideEffect(ChatsContract.SideEffect.ShowError(AppError.Network))
        }
    }

    @Test
    fun `mute with duration sends absolute end time`() = runTest {
        chats.chats.value = listOf(TestData.chat(id = "c1"))
        val before = System.currentTimeMillis()
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(ChatsContract.Intent.OnMute("c1", MuteDuration.HOUR))
            containerHost.onEventDispatcher(ChatsContract.Intent.OnUnmute("c1"))
        }
        val (chatId, muted, until) = chats.mutes.first()
        assertEquals("c1", chatId)
        assertTrue(muted)
        assertTrue(until!! >= before + MuteDuration.HOUR.millis!!)
        assertEquals(Triple("c1", false, null as Long?), chats.mutes[1])
    }

    @Test
    fun `navigation intents`() = runTest {
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(ChatsContract.Intent.OnChatClick("c1"))
            containerHost.onEventDispatcher(ChatsContract.Intent.OnSearchClick)
            containerHost.onEventDispatcher(ChatsContract.Intent.OnNewMessageClick)
            containerHost.onEventDispatcher(ChatsContract.Intent.OnMyProfileClick)
        }
        assertEquals(listOf("chat:c1", "search", "new", "me"), navigation)
    }
}
