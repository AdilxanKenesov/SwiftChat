package uz.relay.feature.profile.me

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.domain.model.AppLanguage
import uz.relay.domain.testing.TestData
import uz.relay.feature.profile.R

/** Mening profilim: ma'lumotlar, bildirishnoma switch'i, til tanlash sheet'i va chiqishdan oldin tasdiq. */
@RunWith(RobolectricTestRunner::class)
class MyProfileScreenTest {

    @get:Rule val compose = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val intents = mutableListOf<MyProfileContract.Intent>()
    private val state = MyProfileContract.UiState(me = TestData.user(name = "Ali Valiyev", username = "ali"))

    private fun show(state: MyProfileContract.UiState = this.state) = compose.setContent {
        SwiftChatTheme(darkTheme = false) { MyProfileContent(uiState = state, onEventDispatcher = { intents += it }) }
    }

    @Test
    fun profileInfoIsShown() {
        show()
        compose.onNodeWithText("Ali Valiyev").assertExists()
        compose.onNodeWithText("ali").assertExists()
    }

    @Test
    fun notificationsRowToggles() {
        show()
        compose.onNodeWithText(context.getString(R.string.notifications)).performClick()
        assertEquals(listOf<MyProfileContract.Intent>(MyProfileContract.Intent.OnNotificationsChange(false)), intents)
    }

    @Test
    fun languageSheetChangesLanguage() {
        show()
        compose.onNodeWithText(context.getString(R.string.language)).performClick()
        compose.onNodeWithText(context.getString(R.string.lang_en)).performClick()
        assertEquals(listOf<MyProfileContract.Intent>(MyProfileContract.Intent.OnLanguageChange(AppLanguage.EN)), intents)
    }

    @Test
    fun logoutAsksFirstAndCancelDoesNothing() {
        show()
        compose.onNodeWithText(context.getString(R.string.logout)).performScrollTo().performClick()
        compose.onNodeWithText(context.getString(R.string.logout_title)).assertExists()
        compose.onNodeWithText(context.getString(R.string.cancel)).performClick()
        assertTrue(intents.isEmpty())
    }

    @Test
    fun logoutConfirmLogsOut() {
        show()
        compose.onNodeWithText(context.getString(R.string.logout)).performScrollTo().performClick()
        // Dialogdagi "Chiqish" tugmasi — ro'yxatdagi qatordan keyin chizilgan ikkinchi tugun.
        val logoutNodes = compose.onAllNodesWithText(context.getString(R.string.logout))
        logoutNodes[logoutNodes.fetchSemanticsNodes().size - 1].performClick()
        assertEquals(listOf<MyProfileContract.Intent>(MyProfileContract.Intent.OnLogout), intents)
    }
}
