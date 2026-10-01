package uz.relay.feature.chats.search

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.orbitmvi.orbit.test.test
import uz.relay.core.common.result.AppResult
import uz.relay.domain.model.ChatType
import uz.relay.domain.testing.FakeChatRepository
import uz.relay.domain.testing.FakeUserRepository
import uz.relay.domain.testing.MainDispatcherRule
import uz.relay.domain.testing.TestData
import uz.relay.domain.usecase.chat.ObserveChatsUseCase
import uz.relay.domain.usecase.chat.OpenDirectChatUseCase
import uz.relay.domain.usecase.user.SearchUsersUseCase

class SearchViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val users = FakeUserRepository()
    private val chats = FakeChatRepository()
    private val opened = mutableListOf<String>()
    private val directions = object : SearchContract.Directions {
        override suspend fun back() = Unit
        override suspend fun navigateToChat(chatId: String) { opened += chatId }
    }
    private fun viewModel() = SearchViewModel(SearchUsersUseCase(users), OpenDirectChatUseCase(chats), ObserveChatsUseCase(chats), directions)

    @Test
    fun `local chats match instantly and users after debounce`() = runTest {
        val group = TestData.chat(id = "g1", type = ChatType.GROUP, title = "Oila guruhi", peerUserId = null)
        val other = TestData.chat(id = "g2", type = ChatType.GROUP, title = "Ish", peerUserId = null)
        chats.chats.value = listOf(group, other)
        val vali = TestData.user(id = "vali", name = "Oilaviy Vali")
        users.searchResult = AppResult.Success(listOf(vali))
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(SearchContract.Intent.OnQueryChange("oila"))
            expectState { copy(query = "oila") }
            expectState { copy(chatResults = listOf(group)) }
            expectState { copy(isSearching = true) }
            expectState { copy(isSearching = false, results = listOf(vali), searchedQuery = "oila") }
        }
    }

    @Test
    fun `user click opens direct chat`() = runTest {
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(SearchContract.Intent.OnUserClick(TestData.user(id = "vali")))
            expectState { copy(openingUserId = "vali") }
            expectState { copy(openingUserId = null) }
            containerHost.onEventDispatcher(SearchContract.Intent.OnChatClick("g1"))
        }
        assertEquals(listOf("direct-vali", "g1"), opened)
    }
}
