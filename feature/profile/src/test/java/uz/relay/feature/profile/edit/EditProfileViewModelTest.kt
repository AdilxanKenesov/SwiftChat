package uz.relay.feature.profile.edit

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.orbitmvi.orbit.test.test
import uz.relay.core.common.result.AppError
import uz.relay.core.common.result.AppResult
import uz.relay.core.common.result.ErrorCodes
import uz.relay.domain.testing.FakeUserRepository
import uz.relay.domain.testing.MainDispatcherRule
import uz.relay.domain.testing.TestData
import uz.relay.domain.usecase.user.ObserveMeUseCase
import uz.relay.domain.usecase.user.UpdateProfileUseCase

class EditProfileViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val users = FakeUserRepository().apply { me.value = TestData.user(name = "Ali", username = "ali") }
    private var backs = 0
    private val directions = object : EditProfileContract.Directions {
        override suspend fun back() { backs++ }
    }
    private fun viewModel() = EditProfileViewModel(ObserveMeUseCase(users), UpdateProfileUseCase(users), directions)

    private val loaded = EditProfileContract.UiState(
        loaded = true, userId = "me", name = "Ali", username = "ali", initialName = "Ali", initialUsername = "ali"
    )

    @Test
    fun `fields are filled from my profile`() = runTest {
        viewModel().test(this) {
            expectInitialState()
            runOnCreate()
            expectState(loaded)
        }
    }

    @Test
    fun `save is disabled until something changes`() {
        assertFalse(loaded.saveEnabled)
        assertFalse(loaded.copy(name = " Ali ").saveEnabled)
        assertTrue(loaded.copy(name = "Ali V").saveEnabled)
        assertFalse(loaded.copy(username = "al").saveEnabled)
        assertFalse(loaded.copy(username = "alii", usernameTaken = true).saveEnabled)
    }

    @Test
    fun `save sends trimmed name and goes back`() = runTest {
        viewModel().test(this) {
            expectInitialState()
            runOnCreate()
            expectState(loaded)
            containerHost.onEventDispatcher(EditProfileContract.Intent.OnNameChange(" Ali Valiyev "))
            expectState { copy(name = " Ali Valiyev ") }
            containerHost.onEventDispatcher(EditProfileContract.Intent.OnSave)
            expectState { copy(saving = true) }
            expectState { copy(saving = false) }
        }
        assertEquals(listOf("Ali Valiyev" to "ali"), users.updates)
        assertEquals(1, backs)
    }

    @Test
    fun `taken username is marked and editing clears it`() = runTest {
        users.updateProfileResult = AppResult.Error(TestData.apiError(ErrorCodes.USERNAME_TAKEN, 409))
        viewModel().test(this) {
            expectInitialState()
            runOnCreate()
            expectState(loaded)
            containerHost.onEventDispatcher(EditProfileContract.Intent.OnUsernameChange("vali!"))
            expectState { copy(username = "vali") }
            containerHost.onEventDispatcher(EditProfileContract.Intent.OnSave)
            expectState { copy(saving = true) }
            expectState { copy(saving = false, usernameTaken = true) }
            containerHost.onEventDispatcher(EditProfileContract.Intent.OnUsernameChange("vali_1"))
            expectState { copy(username = "vali_1", usernameTaken = false) }
        }
        assertEquals(0, backs)
    }

    @Test
    fun `network error is shown`() = runTest {
        users.updateProfileResult = AppResult.Error(AppError.Network)
        viewModel().test(this) {
            expectInitialState()
            runOnCreate()
            expectState(loaded)
            containerHost.onEventDispatcher(EditProfileContract.Intent.OnNameChange("Vali"))
            expectState { copy(name = "Vali") }
            containerHost.onEventDispatcher(EditProfileContract.Intent.OnSave)
            expectState { copy(saving = true) }
            expectState { copy(saving = false) }
            expectSideEffect(EditProfileContract.SideEffect.ShowError(AppError.Network))
        }
    }
}
