package uz.relay.feature.chats.newmessage

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
import uz.relay.domain.testing.MainDispatcherRule
import uz.relay.domain.testing.TestData
import uz.relay.domain.usecase.chat.OpenDirectChatUseCase
import uz.relay.domain.usecase.contact.ObserveContactsUseCase
import uz.relay.domain.usecase.contact.RemoveContactUseCase

class NewMessageViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val contacts = FakeContactRepository()
    private val chats = FakeChatRepository()
    private val navigation = mutableListOf<String>()
    private val directions = object : NewMessageContract.Directions {
        override suspend fun back() { navigation += "back" }
        override suspend fun navigateToGroupCreate() { navigation += "group" }
        override suspend fun navigateToAddContact() { navigation += "add" }
        override suspend fun navigateToChat(chatId: String) { navigation += "chat:$chatId" }
    }
    private fun viewModel() = NewMessageViewModel(
        ObserveContactsUseCase(contacts), RemoveContactUseCase(contacts), OpenDirectChatUseCase(chats), directions
    )
    private val vali = TestData.user(id = "vali", name = "Vali")

    @Test
    fun `contacts are shown and loaded flag set even when empty`() = runTest {
        viewModel().test(this) {
            expectInitialState()
            runOnCreate()
            expectState { copy(isLoaded = true) }
            contacts.contacts.value = listOf(vali)
            expectState { copy(contacts = listOf(vali)) }
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `contact click opens direct chat`() = runTest {
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(NewMessageContract.Intent.OnContactClick(vali))
            expectState { copy(openingUserId = "vali") }
            expectState { copy(openingUserId = null) }
        }
        assertEquals(listOf("chat:direct-vali"), navigation)
    }

    @Test
    fun `failed open shows error`() = runTest {
        chats.openDirectResult = AppResult.Error(AppError.Network)
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(NewMessageContract.Intent.OnContactClick(vali))
            expectState { copy(openingUserId = "vali") }
            expectState { copy(openingUserId = null) }
            expectSideEffect(NewMessageContract.SideEffect.ShowError(AppError.Network))
        }
        assertTrue(navigation.isEmpty())
    }

    @Test
    fun `remove deletes local contact`() = runTest {
        contacts.contacts.value = listOf(vali)
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(NewMessageContract.Intent.OnRemoveContact(vali))
        }
        assertTrue(contacts.contacts.value.isEmpty())
    }

    @Test
    fun `action rows navigate`() = runTest {
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(NewMessageContract.Intent.OnNewGroup)
            containerHost.onEventDispatcher(NewMessageContract.Intent.OnNewContact)
            containerHost.onEventDispatcher(NewMessageContract.Intent.OnBack)
        }
        assertEquals(listOf("group", "add", "back"), navigation)
    }
}
