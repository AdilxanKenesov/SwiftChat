package uz.relay.feature.auth.phone

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.orbitmvi.orbit.test.test
import uz.relay.core.common.result.AppError
import uz.relay.core.common.result.AppResult
import uz.relay.core.common.result.ErrorCodes
import uz.relay.domain.testing.FakeAuthRepository
import uz.relay.domain.testing.MainDispatcherRule
import uz.relay.domain.testing.TestData
import uz.relay.domain.usecase.auth.RequestOtpUseCase

class PhoneViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val openedOtp = mutableListOf<String>()
    private val directions = object : PhoneContract.Directions {
        override suspend fun navigateToOtp(phone: String) { openedOtp += phone }
    }
    private fun viewModel() = PhoneViewModel(RequestOtpUseCase(auth), directions)

    @Test
    fun `input keeps only digits and at most nine`() = runTest {
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(PhoneContract.Intent.OnPhoneChange("90 123-45-67 89"))
            expectState { copy(digits = "901234567") }
        }
    }

    @Test
    fun `incomplete number does not request code`() = runTest {
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(PhoneContract.Intent.OnPhoneChange("90123"))
            expectState { copy(digits = "90123") }
            containerHost.onEventDispatcher(PhoneContract.Intent.OnGetCode)
        }
        assertTrue(auth.requestedPhones.isEmpty())
    }

    @Test
    fun `success requests code with country prefix and opens otp`() = runTest {
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(PhoneContract.Intent.OnPhoneChange("901234567"))
            expectState { copy(digits = "901234567") }
            containerHost.onEventDispatcher(PhoneContract.Intent.OnGetCode)
            expectState { copy(loading = true) }
            expectState { copy(loading = false) }
        }
        assertEquals(listOf("+998901234567"), auth.requestedPhones)
        assertEquals(listOf("+998901234567"), openedOtp)
    }

    @Test
    fun `telegram not linked shows bot sheet instead of error`() = runTest {
        auth.requestOtpResult = AppResult.Error(
            AppError.Api(400, ErrorCodes.TELEGRAM_NOT_LINKED, "link", retryable = false, botUrl = "https://t.me/bot")
        )
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(PhoneContract.Intent.OnPhoneChange("901234567"))
            expectState { copy(digits = "901234567") }
            containerHost.onEventDispatcher(PhoneContract.Intent.OnGetCode)
            expectState { copy(loading = true) }
            expectState { copy(loading = false, botUrl = "https://t.me/bot") }
            containerHost.onEventDispatcher(PhoneContract.Intent.OnOpenBot)
            expectSideEffect(PhoneContract.SideEffect.OpenUrl("https://t.me/bot"))
            containerHost.onEventDispatcher(PhoneContract.Intent.OnDismissTelegramSheet)
            expectState { copy(botUrl = null) }
        }
        assertTrue(openedOtp.isEmpty())
    }

    @Test
    fun `other errors go to snackbar`() = runTest {
        auth.requestOtpResult = AppResult.Error(AppError.Network)
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(PhoneContract.Intent.OnPhoneChange("901234567"))
            expectState { copy(digits = "901234567") }
            containerHost.onEventDispatcher(PhoneContract.Intent.OnGetCode)
            expectState { copy(loading = true) }
            expectState { copy(loading = false) }
            expectSideEffect(PhoneContract.SideEffect.ShowError(AppError.Network))
        }
    }

    @Test
    fun `rate limit error is shown not swallowed`() = runTest {
        val error = TestData.apiError(ErrorCodes.RATE_LIMITED, 429)
        auth.requestOtpResult = AppResult.Error(error)
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(PhoneContract.Intent.OnPhoneChange("901234567"))
            expectState { copy(digits = "901234567") }
            containerHost.onEventDispatcher(PhoneContract.Intent.OnGetCode)
            expectState { copy(loading = true) }
            expectState { copy(loading = false) }
            expectSideEffect(PhoneContract.SideEffect.ShowError(error))
        }
    }
}
