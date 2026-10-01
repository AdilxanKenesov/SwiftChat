package uz.relay.feature.auth.profile

import android.content.Context
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.feature.auth.R

/** Profil to'ldirish: qoida eslatmasi faqat xato kiritilganda, band username'da takliflar chiqadi. */
@RunWith(RobolectricTestRunner::class)
class ProfileSetupScreenTest {

    @get:Rule val compose = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val intents = mutableListOf<ProfileSetupContract.Intent>()

    private fun show(state: ProfileSetupContract.UiState) = compose.setContent {
        SwiftChatTheme(darkTheme = false) { ProfileSetupScreenContent(uiState = state, onEventDispatcher = { intents += it }) }
    }

    @Test
    fun rulesHintIsHiddenForEmptyAndValidUsername() {
        show(ProfileSetupContract.UiState(name = "Ali", username = "ali"))
        compose.onNodeWithText(context.getString(R.string.username_rules)).assertDoesNotExist()
        compose.onNodeWithText(context.getString(R.string.continue_button)).assertIsEnabled()
    }

    @Test
    fun rulesHintAppearsForInvalidUsername() {
        show(ProfileSetupContract.UiState(name = "Ali", username = "al"))
        compose.onNodeWithText(context.getString(R.string.username_rules)).assertExists()
        compose.onNodeWithText(context.getString(R.string.continue_button)).assertIsNotEnabled()
    }

    @Test
    fun takenUsernameShowsSuggestionsThatCanBePicked() {
        show(ProfileSetupContract.UiState(name = "Ali", username = "ali", usernameTaken = true, suggestions = listOf("ali_dev", "ali01")))
        compose.onNodeWithText(context.getString(R.string.username_taken)).assertExists()
        compose.onNodeWithText("ali_dev").performClick()
        assertEquals(listOf<ProfileSetupContract.Intent>(ProfileSetupContract.Intent.OnSuggestionClick("ali_dev")), intents)
    }
}
