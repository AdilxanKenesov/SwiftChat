package uz.relay.feature.auth.otp

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.feature.auth.R

/** OTP ekrani holatlari: xato urinishlar soni, muddati o'tgan / bloklangan kodda "Yangi kod olish". */
@RunWith(RobolectricTestRunner::class)
class OtpScreenTest {

    @get:Rule val compose = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val intents = mutableListOf<OtpContract.Intent>()
    private val base = OtpContract.UiState(phone = "+998901234567", codeLength = 6)

    private fun show(state: OtpContract.UiState) = compose.setContent {
        SwiftChatTheme(darkTheme = false) { OtpScreenContent(uiState = state, shakeEvents = emptyFlow(), onEventDispatcher = { intents += it }) }
    }

    @Test
    fun wrongCodeShowsAttemptsLeft() {
        show(base.copy(status = OtpContract.Status.Wrong(attemptsLeft = 3)))
        val text = context.resources.getQuantityString(R.plurals.otp_wrong, 3, 3)
        compose.onNodeWithText(text).assertExists()
    }

    @Test
    fun expiredCodeOffersNewCode() {
        show(base.copy(status = OtpContract.Status.Expired))
        compose.onNodeWithText(context.getString(R.string.otp_expired)).assertExists()
        compose.onNodeWithText(context.getString(R.string.new_code)).performClick()
        assertEquals(listOf<OtpContract.Intent>(OtpContract.Intent.OnResendCode), intents)
    }

    @Test
    fun lockedCodeShowsLockedCard() {
        show(base.copy(status = OtpContract.Status.Locked))
        compose.onNodeWithText(context.getString(R.string.otp_locked)).assertExists()
        compose.onNodeWithText(context.getString(R.string.new_code)).assertExists()
    }

    @Test
    fun resendIsAvailableWhenTimerEnds() {
        show(base.copy(secondsLeft = 0))
        compose.onNodeWithText(context.getString(R.string.resend_code)).performClick()
        assertEquals(listOf<OtpContract.Intent>(OtpContract.Intent.OnResendCode), intents)
    }

    @Test
    fun backButtonGoesBack() {
        show(base)
        compose.onNodeWithContentDescription(context.getString(R.string.back)).performClick()
        assertEquals(listOf<OtpContract.Intent>(OtpContract.Intent.OnBack), intents)
    }
}
