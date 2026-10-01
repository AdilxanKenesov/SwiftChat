package uz.relay.feature.profile.user

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.orbitmvi.orbit.test.test
import uz.relay.core.common.result.AppError
import uz.relay.core.common.result.AppResult
import uz.relay.domain.testing.FakeChatRepository
import uz.relay.domain.testing.FakeContactRepository
import uz.relay.domain.testing.FakeUserRepository
import uz.relay.domain.testing.MainDispatcherRule
import uz.relay.domain.testing.TestData
import uz.relay.domain.usecase.chat.ObserveDirectChatUseCase
import uz.relay.domain.usecase.chat.OpenDirectChatUseCase
import uz.relay.domain.usecase.chat.SetChatMutedUseCase
import uz.relay.domain.usecase.contact.AddContactUseCase
import uz.relay.domain.usecase.contact.ObserveContactIdsUseCase
import uz.relay.domain.usecase.contact.RemoveContactUseCase
import uz.relay.domain.usecase.user.ObserveUserUseCase
import uz.relay.domain.usecase.user.RefreshUserUseCase

class UserProfileViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val vali = TestData.user(id = "vali", name = "Vali")
    private val users = FakeUserRepository().apply { this.users.value = mapOf("vali" to vali) }
    private val chats = FakeChatRepository()
    private val contacts = FakeContactRepository()
    private val openedChats = mutableListOf<String>()
    private val directions = object : UserProfileContract.Directions {
        override suspend fun back() = Unit
        override suspend fun navigateToChat(chatId: String) { openedChats += chatId }
    }

    private fun viewModel(userId: String = "vali") = UserProfileViewModel(
        userId = userId,
        observeUser = ObserveUserUseCase(users),
        refreshUser = RefreshUserUseCase(users),
        observeDirectChat = ObserveDirectChatUseCase(chats),
        openDirectChat = OpenDirectChatUseCase(chats),
        setChatMuted = SetChatMutedUseCase(chats),
        observeContactIds = ObserveContactIdsUseCase(contacts),
        addContact = AddContactUseCase(contacts),
        removeContact = RemoveContactUseCase(contacts),
        directions = directions
    )

    @Test
    fun `message opens existing chat without creating a new one`() = runTest {
        val chat = TestData.chat(id = "c1", peerUserId = "vali")
        viewModel().test(this, UserProfileContract.UiState(user = vali, chat = chat)) {
            expectInitialState()
            containerHost.onEventDispatcher(UserProfileContract.Intent.OnMessage)
        }
        assertEquals(listOf("c1"), openedChats)
        assertTrue(chats.openedDirects.isEmpty())
    }

    @Test
    fun `message creates direct chat when none exists`() = runTest {
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(UserProfileContract.Intent.OnMessage)
        }
        assertEquals(listOf("vali"), chats.openedDirects)
        assertEquals(listOf("direct-vali"), openedChats)
    }

    @Test
    fun `failed chat creation shows error`() = runTest {
        chats.openDirectResult = AppResult.Error(AppError.Network)
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(UserProfileContract.Intent.OnMessage)
            expectSideEffect(UserProfileContract.SideEffect.ShowError(AppError.Network))
        }
        assertTrue(openedChats.isEmpty())
    }

    @Test
    fun `mute toggles existing chat`() = runTest {
        val chat = TestData.chat(id = "c1", peerUserId = "vali")
        chats.chats.value = listOf(chat)
        viewModel().test(this, UserProfileContract.UiState(user = vali, chat = chat)) {
            expectInitialState()
            containerHost.onEventDispatcher(UserProfileContract.Intent.OnToggleMute)
            expectState { copy(isBusy = true) }
            expectState { copy(isBusy = false) }
        }
        assertEquals(listOf(Triple("c1", true, null as Long?)), chats.mutes)
    }

    @Test
    fun `adding contact reports success`() = runTest {
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(UserProfileContract.Intent.OnToggleContact)
            expectState { copy(isBusy = true) }
            expectState { copy(isBusy = false) }
            expectSideEffect(UserProfileContract.SideEffect.ContactAdded)
        }
        assertEquals(listOf("vali"), contacts.contacts.value.map { it.id })
    }

    @Test
    fun `existing contact is removed`() = runTest {
        contacts.contacts.value = listOf(vali)
        viewModel().test(this, UserProfileContract.UiState(user = vali, isContact = true)) {
            expectInitialState()
            containerHost.onEventDispatcher(UserProfileContract.Intent.OnToggleContact)
        }
        assertTrue(contacts.contacts.value.isEmpty())
    }

    @Test
    fun `screen observes user chat and contact flag`() = runTest {
        val chat = TestData.chat(id = "c1", peerUserId = "vali")
        chats.chats.value = listOf(chat)
        contacts.contacts.value = listOf(vali)
        viewModel().test(this) {
            expectInitialState()
            runOnCreate()
            expectState { copy(user = vali, chat = chat, isContact = true) }
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `unknown user that cannot be loaded shows error`() = runTest {
        viewModel(userId = "ghost").test(this) {
            expectInitialState()
            runOnCreate()
            assertTrue(awaitSideEffect() is UserProfileContract.SideEffect.ShowError)
            cancelAndIgnoreRemainingItems()
        }
    }
}
