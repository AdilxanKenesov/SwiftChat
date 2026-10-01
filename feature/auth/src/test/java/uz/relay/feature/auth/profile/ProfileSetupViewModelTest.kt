package uz.relay.feature.auth.profile

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
import uz.relay.domain.testing.FakeAuthRepository
import uz.relay.domain.testing.FakeUserRepository
import uz.relay.domain.testing.MainDispatcherRule
import uz.relay.domain.testing.TestData
import uz.relay.domain.usecase.auth.CompleteProfileSetupUseCase
import uz.relay.domain.usecase.user.UpdateProfileUseCase

class ProfileSetupViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val users = FakeUserRepository()
    private var openedChats = 0
    private val directions = object : ProfileSetupContract.Directions {
        override suspend fun navigateToChats() { openedChats++ }
    }
    private fun viewModel() = ProfileSetupViewModel(UpdateProfileUseCase(users), CompleteProfileSetupUseCase(auth), directions)

    @Test
    fun `username keeps only allowed characters`() = runTest {
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(ProfileSetupContract.Intent.OnUsernameChange("ali valiyev-01!"))
            expectState { copy(username = "alivaliyev01") }
        }
    }

    @Test
    fun `continue is disabled until name and username are valid`() {
        assertFalse(ProfileSetupContract.UiState(name = "Ali", username = "al").continueEnabled)
        assertFalse(ProfileSetupContract.UiState(name = " ", username = "ali").continueEnabled)
        assertTrue(ProfileSetupContract.UiState(name = "Ali", username = "ali").continueEnabled)
        assertFalse(ProfileSetupContract.UiState(name = "Ali", username = "ali", usernameTaken = true).continueEnabled)
    }

    @Test
    fun `save trims name completes setup and opens chats`() = runTest {
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(ProfileSetupContract.Intent.OnNameChange("  Ali  "))
            expectState { copy(name = "  Ali  ") }
            containerHost.onEventDispatcher(ProfileSetupContract.Intent.OnUsernameChange("ali"))
            expectState { copy(username = "ali") }
            containerHost.onEventDispatcher(ProfileSetupContract.Intent.OnContinue)
            expectState { copy(saving = true) }
            expectState { copy(saving = false) }
        }
        assertEquals(listOf("Ali" to "ali"), users.updates)
        assertTrue(auth.profileSetupCompleted)
        assertEquals(1, openedChats)
    }

    @Test
    fun `taken username shows suggestions and selecting one clears the error`() = runTest {
        users.updateProfileResult = AppResult.Error(TestData.apiError(ErrorCodes.USERNAME_TAKEN, 409))
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(ProfileSetupContract.Intent.OnNameChange("Ali"))
            expectState { copy(name = "Ali") }
            containerHost.onEventDispatcher(ProfileSetupContract.Intent.OnUsernameChange("ali"))
            expectState { copy(username = "ali") }
            containerHost.onEventDispatcher(ProfileSetupContract.Intent.OnContinue)
            expectState { copy(saving = true) }
            expectState { copy(saving = false, usernameTaken = true, suggestions = listOf("ali_dev", "ali01")) }
            containerHost.onEventDispatcher(ProfileSetupContract.Intent.OnSuggestionClick("ali_dev"))
            expectState { copy(username = "ali_dev", usernameTaken = false, suggestions = emptyList()) }
        }
        assertFalse(auth.profileSetupCompleted)
        assertEquals(0, openedChats)
    }

    @Test
    fun `suggestions stay within username length limit`() = runTest {
        users.updateProfileResult = AppResult.Error(TestData.apiError(ErrorCodes.USERNAME_TAKEN, 409))
        val long = "a".repeat(32)
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(ProfileSetupContract.Intent.OnNameChange("Ali"))
            expectState { copy(name = "Ali") }
            containerHost.onEventDispatcher(ProfileSetupContract.Intent.OnUsernameChange(long))
            expectState { copy(username = long) }
            containerHost.onEventDispatcher(ProfileSetupContract.Intent.OnContinue)
            expectState { copy(saving = true) }
            val taken = awaitState()
            assertTrue(taken.suggestions.all { it.length <= 32 })
            assertEquals(2, taken.suggestions.size)
        }
    }

    @Test
    fun `other errors go to snackbar`() = runTest {
        users.updateProfileResult = AppResult.Error(AppError.Network)
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(ProfileSetupContract.Intent.OnNameChange("Ali"))
            expectState { copy(name = "Ali") }
            containerHost.onEventDispatcher(ProfileSetupContract.Intent.OnUsernameChange("ali"))
            expectState { copy(username = "ali") }
            containerHost.onEventDispatcher(ProfileSetupContract.Intent.OnContinue)
            expectState { copy(saving = true) }
            expectState { copy(saving = false) }
            expectSideEffect(ProfileSetupContract.SideEffect.ShowError(AppError.Network))
        }
    }
}
