package uz.relay.feature.group.create

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.orbitmvi.orbit.test.test
import uz.relay.core.common.result.AppError
import uz.relay.core.common.result.AppResult
import uz.relay.domain.testing.FakeGroupRepository
import uz.relay.domain.testing.FakeUserRepository
import uz.relay.domain.testing.MainDispatcherRule
import uz.relay.domain.testing.TestData
import uz.relay.domain.usecase.group.AddMembersUseCase
import uz.relay.domain.usecase.group.CreateGroupUseCase
import uz.relay.domain.usecase.group.ObserveMembersUseCase
import uz.relay.domain.usecase.user.ObserveKnownUsersUseCase
import uz.relay.domain.usecase.user.SearchUsersUseCase

class GroupCreateViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val users = FakeUserRepository()
    private val groups = FakeGroupRepository()
    private val navigation = mutableListOf<String>()
    private val directions = object : GroupCreateContract.Directions {
        override suspend fun back() { navigation += "back" }
        override suspend fun openCreatedChat(chatId: String) { navigation += "chat:$chatId" }
    }
    private fun viewModel(addTo: String? = null) = GroupCreateViewModel(
        addToChatId = addTo,
        observeKnownUsers = ObserveKnownUsersUseCase(users),
        observeMembers = ObserveMembersUseCase(groups),
        searchUsers = SearchUsersUseCase(users),
        createGroup = CreateGroupUseCase(groups),
        addMembers = AddMembersUseCase(groups),
        directions = directions
    )
    private val ali = TestData.user(id = "ali", name = "Ali", username = "ali")
    private val vali = TestData.user(id = "vali", name = "Vali", username = "vali")

    @Test
    fun `candidates exclude existing members in add mode`() = runTest {
        users.users.value = mapOf("ali" to ali, "vali" to vali)
        groups.members.value = mapOf("g1" to listOf(TestData.member("ali")))
        viewModel(addTo = "g1").test(this) {
            expectInitialState()
            runOnCreate()
            assertEquals(listOf(vali), awaitState().candidates)
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `toggle selects and deselects`() = runTest {
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(GroupCreateContract.Intent.OnToggle(ali))
            expectState { copy(selected = listOf(ali)) }
            containerHost.onEventDispatcher(GroupCreateContract.Intent.OnToggle(vali))
            expectState { copy(selected = listOf(ali, vali)) }
            containerHost.onEventDispatcher(GroupCreateContract.Intent.OnToggle(ali))
            expectState { copy(selected = listOf(vali)) }
        }
    }

    @Test
    fun `create flow goes pick then name then opens chat`() = runTest {
        viewModel().test(this, GroupCreateContract.UiState(selected = listOf(ali, vali))) {
            expectInitialState()
            containerHost.onEventDispatcher(GroupCreateContract.Intent.OnNext)
            expectState { copy(step = GroupCreateContract.Step.NAME) }
            containerHost.onEventDispatcher(GroupCreateContract.Intent.OnTitleChange("Oila"))
            expectState { copy(title = "Oila") }
            containerHost.onEventDispatcher(GroupCreateContract.Intent.OnCreate)
            expectState { copy(isSubmitting = true) }
            expectState { copy(isSubmitting = false) }
        }
        assertEquals(listOf("Oila" to listOf("ali", "vali")), groups.created)
        assertEquals(listOf("chat:group-1"), navigation)
    }

    @Test
    fun `back on name step returns to picking`() = runTest {
        viewModel().test(this, GroupCreateContract.UiState(step = GroupCreateContract.Step.NAME)) {
            expectInitialState()
            containerHost.onEventDispatcher(GroupCreateContract.Intent.OnBack)
            expectState { copy(step = GroupCreateContract.Step.PICK) }
            containerHost.onEventDispatcher(GroupCreateContract.Intent.OnBack)
        }
        assertEquals(listOf("back"), navigation)
    }

    @Test
    fun `add mode adds members and goes back`() = runTest {
        viewModel(addTo = "g1").test(this, GroupCreateContract.UiState(addToChatId = "g1", selected = listOf(vali))) {
            expectInitialState()
            containerHost.onEventDispatcher(GroupCreateContract.Intent.OnNext)
            expectState { copy(isSubmitting = true) }
            expectState { copy(isSubmitting = false) }
        }
        assertEquals(listOf("g1" to listOf("vali")), groups.added)
        assertEquals(listOf("back"), navigation)
    }

    @Test
    fun `create error is shown`() = runTest {
        groups.createResult = AppResult.Error(AppError.Network)
        viewModel().test(this, GroupCreateContract.UiState(step = GroupCreateContract.Step.NAME, selected = listOf(ali), title = "Oila")) {
            expectInitialState()
            containerHost.onEventDispatcher(GroupCreateContract.Intent.OnCreate)
            expectState { copy(isSubmitting = true) }
            expectState { copy(isSubmitting = false) }
            expectSideEffect(GroupCreateContract.SideEffect.ShowError(AppError.Network))
        }
    }

    @Test
    fun `create needs title and at least one member`() {
        assertFalse(GroupCreateContract.UiState(title = " ", selected = listOf(ali)).canCreate)
        assertFalse(GroupCreateContract.UiState(title = "Oila").canCreate)
        assertTrue(GroupCreateContract.UiState(title = "Oila", selected = listOf(ali)).canCreate)
        assertFalse(GroupCreateContract.UiState(title = "x".repeat(129), selected = listOf(ali)).canCreate)
    }
}
