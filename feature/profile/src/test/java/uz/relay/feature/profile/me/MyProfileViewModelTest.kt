package uz.relay.feature.profile.me

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.orbitmvi.orbit.test.test
import uz.relay.domain.model.AppLanguage
import uz.relay.domain.model.ConnectionStatus
import uz.relay.domain.testing.FakeAuthRepository
import uz.relay.domain.testing.FakeConnectionRepository
import uz.relay.domain.testing.FakeSettingsRepository
import uz.relay.domain.testing.FakeUserRepository
import uz.relay.domain.testing.MainDispatcherRule
import uz.relay.domain.testing.TestData
import uz.relay.domain.usecase.auth.LogoutUseCase
import uz.relay.domain.usecase.chat.ObserveConnectionStatusUseCase
import uz.relay.domain.usecase.settings.ObserveLanguageUseCase
import uz.relay.domain.usecase.settings.ObserveNotificationsEnabledUseCase
import uz.relay.domain.usecase.settings.ObserveThemeModeUseCase
import uz.relay.domain.usecase.settings.SetLanguageUseCase
import uz.relay.domain.usecase.settings.SetNotificationsEnabledUseCase
import uz.relay.domain.usecase.settings.SetThemeModeUseCase
import uz.relay.domain.usecase.user.ObserveMeUseCase
import uz.relay.domain.usecase.user.RefreshMeUseCase

class MyProfileViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val users = FakeUserRepository()
    private val settings = FakeSettingsRepository()
    private val connection = FakeConnectionRepository()
    private val navigation = mutableListOf<String>()
    private val directions = object : MyProfileContract.Directions {
        override suspend fun back() { navigation += "back" }
        override suspend fun navigateToEditProfile() { navigation += "edit" }
    }

    private fun viewModel() = MyProfileViewModel(
        observeMe = ObserveMeUseCase(users),
        refreshMe = RefreshMeUseCase(users),
        observeConnectionStatus = ObserveConnectionStatusUseCase(connection),
        observeThemeMode = ObserveThemeModeUseCase(settings),
        setThemeMode = SetThemeModeUseCase(settings),
        observeNotificationsEnabled = ObserveNotificationsEnabledUseCase(settings),
        setNotificationsEnabled = SetNotificationsEnabledUseCase(settings),
        observeLanguage = ObserveLanguageUseCase(settings),
        setLanguage = SetLanguageUseCase(settings),
        logout = LogoutUseCase(auth),
        directions = directions
    )

    @Test
    fun `screen shows me settings and connection`() = runTest {
        settings.lang.value = AppLanguage.RU
        connection.current.value = ConnectionStatus.OFFLINE
        viewModel().test(this) {
            expectInitialState()
            runOnCreate()
            val state = awaitState()
            assertEquals(TestData.user(), state.me)
            assertEquals(ConnectionStatus.OFFLINE, state.connectionStatus)
            assertEquals(AppLanguage.RU, state.language)
            assertTrue(state.notificationsEnabled)
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `settings changes are saved and reflected`() = runTest {
        viewModel().test(this) {
            expectInitialState()
            runOnCreate()
            awaitState()
            containerHost.onEventDispatcher(MyProfileContract.Intent.OnNotificationsChange(false))
            assertFalse(awaitState().notificationsEnabled)
            containerHost.onEventDispatcher(MyProfileContract.Intent.OnLanguageChange(AppLanguage.EN))
            assertEquals(AppLanguage.EN, awaitState().language)
            cancelAndIgnoreRemainingItems()
        }
        assertFalse(settings.notifications.value)
        assertEquals(AppLanguage.EN, settings.lang.value)
    }

    @Test
    fun `logout runs once even if pressed twice`() = runTest {
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(MyProfileContract.Intent.OnLogout)
            expectState { copy(loggingOut = true) }
            containerHost.onEventDispatcher(MyProfileContract.Intent.OnLogout)
        }
        assertTrue(auth.loggedOut)
    }

    @Test
    fun `navigation intents`() = runTest {
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(MyProfileContract.Intent.OnEdit)
            containerHost.onEventDispatcher(MyProfileContract.Intent.OnBack)
        }
        assertEquals(listOf("edit", "back"), navigation)
    }
}
