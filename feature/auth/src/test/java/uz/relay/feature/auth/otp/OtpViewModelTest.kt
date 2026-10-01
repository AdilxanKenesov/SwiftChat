package uz.relay.feature.auth.otp

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
import uz.relay.domain.usecase.auth.VerifyOtpUseCase

class OtpViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val phone = "+998901234567"
    private val auth = FakeAuthRepository()
    private val navigation = mutableListOf<String>()
    private val directions = object : OtpContract.Directions {
        override suspend fun back() { navigation += "back" }
        override suspend fun navigateToProfileSetup() { navigation += "profile" }
        override suspend fun navigateToChats() { navigation += "chats" }
    }
    private fun viewModel() = OtpViewModel(phone, RequestOtpUseCase(auth), VerifyOtpUseCase(auth), directions)
    private val wrongCode = AppResult.Error(TestData.apiError(ErrorCodes.INVALID_OTP))

    @Test
    fun `full code is verified and existing user goes to chats`() = runTest {
        auth.verifyOtpResult = AppResult.Success(false)
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(OtpContract.Intent.OnCodeChange("12 34 56"))
            expectState { copy(code = "123456") }
            expectState { copy(verifying = true) }
            expectState { copy(verifying = false) }
        }
        assertEquals(listOf(phone to "123456"), auth.verifiedCodes)
        assertEquals(listOf("chats"), navigation)
    }

    @Test
    fun `new user goes to profile setup`() = runTest {
        auth.verifyOtpResult = AppResult.Success(true)
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(OtpContract.Intent.OnCodeChange("123456"))
            expectState { copy(code = "123456") }
            expectState { copy(verifying = true) }
            expectState { copy(verifying = false) }
        }
        assertEquals(listOf("profile"), navigation)
    }

    @Test
    fun `partial code is not verified`() = runTest {
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(OtpContract.Intent.OnCodeChange("123"))
            expectState { copy(code = "123") }
        }
        assertEquals(emptyList<Pair<String, String>>(), auth.verifiedCodes)
    }

    @Test
    fun `wrong code counts attempts and shakes`() = runTest {
        auth.verifyOtpResult = wrongCode
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(OtpContract.Intent.OnCodeChange("111111"))
            expectState { copy(code = "111111") }
            expectState { copy(verifying = true) }
            expectState { copy(verifying = false, status = OtpContract.Status.Wrong(attemptsLeft = 4)) }
            expectSideEffect(OtpContract.SideEffect.Shake)
        }
    }

    @Test
    fun `fifth wrong code locks input`() = runTest {
        auth.verifyOtpResult = wrongCode
        viewModel().test(this) {
            expectInitialState()
            repeat(OtpContract.MAX_ATTEMPTS) { attempt ->
                containerHost.onEventDispatcher(OtpContract.Intent.OnCodeChange("111111"))
                expectState { copy(code = "111111", status = OtpContract.Status.Input) }
                expectState { copy(verifying = true) }
                val left = OtpContract.MAX_ATTEMPTS - attempt - 1
                if (left > 0) expectState { copy(verifying = false, status = OtpContract.Status.Wrong(left)) }
                else expectState { copy(verifying = false, code = "", status = OtpContract.Status.Locked) }
                expectSideEffect(OtpContract.SideEffect.Shake)
            }
            // Bloklangach kiritish o'chiq — yangi raqamlar e'tiborsiz qoldiriladi.
            containerHost.onEventDispatcher(OtpContract.Intent.OnCodeChange("222222"))
        }
        assertEquals(OtpContract.MAX_ATTEMPTS, auth.verifiedCodes.size)
    }

    @Test
    fun `expired code disables input`() = runTest {
        auth.verifyOtpResult = AppResult.Error(TestData.apiError(ErrorCodes.OTP_EXPIRED))
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(OtpContract.Intent.OnCodeChange("123456"))
            expectState { copy(code = "123456") }
            expectState { copy(verifying = true) }
            expectState { copy(verifying = false, status = OtpContract.Status.Expired) }
        }
    }

    @Test
    fun `network error clears code and shows snackbar`() = runTest {
        auth.verifyOtpResult = AppResult.Error(AppError.Network)
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(OtpContract.Intent.OnCodeChange("123456"))
            expectState { copy(code = "123456") }
            expectState { copy(verifying = true) }
            expectState { copy(verifying = false, code = "") }
            expectSideEffect(OtpContract.SideEffect.ShowError(AppError.Network))
        }
    }

    @Test
    fun `resend gives fresh attempts and restarts timer`() = runTest {
        auth.verifyOtpResult = wrongCode
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(OtpContract.Intent.OnCodeChange("111111"))
            expectState { copy(code = "111111") }
            expectState { copy(verifying = true) }
            expectState { copy(verifying = false, status = OtpContract.Status.Wrong(4)) }
            expectSideEffect(OtpContract.SideEffect.Shake)

            containerHost.onEventDispatcher(OtpContract.Intent.OnResendCode)
            expectState { copy(resending = true) }
            expectState { copy(resending = false, code = "", status = OtpContract.Status.Input) }
            cancelAndIgnoreRemainingItems()
        }
        assertEquals(listOf(phone), auth.requestedPhones)
    }

    @Test
    fun `timer counts down from sixty`() = runTest {
        viewModel().test(this) {
            expectInitialState()
            runOnCreate()
            expectState { copy(secondsLeft = 59) }
            expectState { copy(secondsLeft = 58) }
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `back goes back`() = runTest {
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(OtpContract.Intent.OnBack)
        }
        assertEquals(listOf("back"), navigation)
    }
}
