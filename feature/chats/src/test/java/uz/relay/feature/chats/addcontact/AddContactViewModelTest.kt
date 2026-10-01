package uz.relay.feature.chats.addcontact

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.orbitmvi.orbit.test.test
import uz.relay.core.common.result.AppError
import uz.relay.core.common.result.AppResult
import uz.relay.domain.testing.FakeContactRepository
import uz.relay.domain.testing.FakeUserRepository
import uz.relay.domain.testing.MainDispatcherRule
import uz.relay.domain.testing.TestData
import uz.relay.domain.usecase.contact.AddContactUseCase
import uz.relay.domain.usecase.contact.ObserveContactIdsUseCase
import uz.relay.domain.usecase.user.SearchUsersUseCase

class AddContactViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val users = FakeUserRepository()
    private val contacts = FakeContactRepository()
    private val directions = object : AddContactContract.Directions {
        override suspend fun back() = Unit
        override suspend fun navigateToUserProfile(userId: String) = Unit
    }
    private fun viewModel() = AddContactViewModel(
        SearchUsersUseCase(users), AddContactUseCase(contacts), ObserveContactIdsUseCase(contacts), directions
    )
    private val vali = TestData.user(id = "vali", name = "Vali", username = "vali")

    @Test
    fun `search waits for typing pause and strips at sign`() = runTest {
        users.searchResult = AppResult.Success(listOf(vali))
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(AddContactContract.Intent.OnQueryChange("@va"))
            expectState { copy(query = "@va") }
            containerHost.onEventDispatcher(AddContactContract.Intent.OnQueryChange("@vali"))
            expectState { copy(query = "@vali") }
            expectState { copy(isSearching = true) }
            expectState { copy(isSearching = false, results = listOf(vali), searchedQuery = "vali") }
        }
        // Birinchi so'rov 300 ms ichida bekor qilingan — serverga faqat oxirgi matn ketdi.
        assertEquals(listOf("vali"), users.searches)
    }

    @Test
    fun `clearing query resets results without request`() = runTest {
        viewModel().test(this, AddContactContract.UiState(query = "vali", results = listOf(vali), searchedQuery = "vali")) {
            expectInitialState()
            containerHost.onEventDispatcher(AddContactContract.Intent.OnClear)
            expectState { copy(query = "") }
            expectState { copy(results = emptyList(), searchedQuery = null) }
        }
        assertTrue(users.searches.isEmpty())
    }

    @Test
    fun `nothing found only after search for the same query finished`() {
        val state = AddContactContract.UiState(query = "zzz", searchedQuery = "zzz")
        assertTrue(state.showNothingFound)
        assertFalse(state.copy(query = "zzzz").showNothingFound)
        assertFalse(state.copy(isSearching = true).showNothingFound)
    }

    @Test
    fun `retryable search error is shown while validation error is silent`() = runTest {
        users.searchResult = AppResult.Error(AppError.Network)
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(AddContactContract.Intent.OnQueryChange("vali"))
            expectState { copy(query = "vali") }
            expectState { copy(isSearching = true) }
            expectState { copy(isSearching = false) }
            expectSideEffect(AddContactContract.SideEffect.ShowError(AppError.Network))
        }
        users.searchResult = AppResult.Error(TestData.apiError())
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(AddContactContract.Intent.OnQueryChange("vali"))
            expectState { copy(query = "vali") }
            expectState { copy(isSearching = true) }
            expectState { copy(isSearching = false) }
        }
    }

    @Test
    fun `add reports name and existing contact is not added twice`() = runTest {
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(AddContactContract.Intent.OnAdd(vali))
            expectState { copy(addingUserId = "vali") }
            expectState { copy(addingUserId = null) }
            expectSideEffect(AddContactContract.SideEffect.Added("Vali"))
        }
        viewModel().test(this, AddContactContract.UiState(contactIds = setOf("vali"))) {
            expectInitialState()
            containerHost.onEventDispatcher(AddContactContract.Intent.OnAdd(vali))
        }
        assertEquals(listOf("vali"), contacts.contacts.value.map { it.id })
    }
}
