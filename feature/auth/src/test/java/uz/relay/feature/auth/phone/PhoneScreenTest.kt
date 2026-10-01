package uz.relay.feature.auth.phone

import android.content.Context
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.feature.auth.R

/** Telefon ekrani: "Kod olish" faqat to'liq raqamda faol, kiritish va bosish ViewModel'ga intent bo'lib ketadi. */
@RunWith(RobolectricTestRunner::class)
class PhoneScreenTest {

    @get:Rule val compose = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val intents = mutableListOf<PhoneContract.Intent>()

    private fun show(state: PhoneContract.UiState) = compose.setContent {
        SwiftChatTheme(darkTheme = false) { PhoneScreenContent(uiState = state, onEventDispatcher = { intents += it }) }
    }

    @Test
    fun getCodeIsDisabledForIncompleteNumber() {
        show(PhoneContract.UiState(digits = "90123"))
        compose.onNodeWithText(context.getString(R.string.get_code)).assertIsNotEnabled()
    }

    @Test
    fun fullNumberEnablesGetCode() {
        show(PhoneContract.UiState(digits = "901234567"))
        compose.onNodeWithText(context.getString(R.string.get_code)).assertIsEnabled().performClick()
        assertEquals(listOf<PhoneContract.Intent>(PhoneContract.Intent.OnGetCode), intents)
    }

    @Test
    fun typingSendsDigitsToViewModel() {
        show(PhoneContract.UiState())
        compose.onNode(hasSetTextAction()).performTextInput("9")
        assertEquals(listOf<PhoneContract.Intent>(PhoneContract.Intent.OnPhoneChange("9")), intents)
    }

    @Test
    fun countryPrefixIsShown() {
        show(PhoneContract.UiState())
        compose.onNodeWithText("+998").assertExists()
        compose.onNodeWithText(context.getString(R.string.phone_title)).assertExists()
    }
}
