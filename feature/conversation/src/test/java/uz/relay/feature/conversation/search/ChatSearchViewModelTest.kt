package uz.relay.feature.conversation.search

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.orbitmvi.orbit.test.test
import uz.relay.domain.testing.FakeMessageRepository
import uz.relay.domain.testing.FakeUserRepository
import uz.relay.domain.testing.MainDispatcherRule
import uz.relay.domain.usecase.message.SearchMessagesUseCase
import uz.relay.domain.usecase.user.ObserveUserNamesUseCase
import uz.relay.feature.conversation.chat.message

class ChatSearchViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val messages = FakeMessageRepository()
    private val users = FakeUserRepository()
    private val opened = mutableListOf<String>()
    private val directions = object : ChatSearchContract.Directions {
        override suspend fun back() = Unit
        override suspend fun openMessage(chatId: String, clientMessageId: String) { opened += "$chatId/$clientMessageId" }
    }
    private fun viewModel() = ChatSearchViewModel("chat", SearchMessagesUseCase(messages), ObserveUserNamesUseCase(users), directions)

    @Test
    fun `search runs after short pause with trimmed query`() = runTest {
        val found = message("m1", text = "salom dunyo")
        messages.searchResult = listOf(found)
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(ChatSearchContract.Intent.OnQueryChange(" salom "))
            expectState { copy(query = " salom ") }
            expectState { copy(results = listOf(found), searchedQuery = "salom") }
            containerHost.onEventDispatcher(ChatSearchContract.Intent.OnResultClick(found))
        }
        assertEquals(listOf("chat/m1"), opened)
    }

    @Test
    fun `clear resets results`() = runTest {
        viewModel().test(this, ChatSearchContract.UiState(query = "a", results = listOf(message("m1")), searchedQuery = "a")) {
            expectInitialState()
            containerHost.onEventDispatcher(ChatSearchContract.Intent.OnClear)
            expectState { copy(query = "") }
            expectState { copy(results = emptyList(), searchedQuery = null) }
        }
    }
}
