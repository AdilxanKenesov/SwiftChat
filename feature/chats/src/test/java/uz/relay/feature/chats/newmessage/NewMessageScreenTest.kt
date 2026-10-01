package uz.relay.feature.chats.newmessage

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.domain.testing.TestData
import uz.relay.feature.chats.R

/** "Yangi xabar": amallar qatori, bo'sh kontaktlar va long-press → tasdiq dialogi orqali o'chirish. */
@RunWith(RobolectricTestRunner::class)
class NewMessageScreenTest {

    @get:Rule val compose = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val intents = mutableListOf<NewMessageContract.Intent>()
    private val vali = TestData.user(id = "vali", name = "Vali Valiyev")

    private fun show(state: NewMessageContract.UiState) = compose.setContent {
        SwiftChatTheme(darkTheme = false) { NewMessageContent(uiState = state, onEventDispatcher = { intents += it }) }
    }

    @Test
    fun emptyContactsMessage() {
        show(NewMessageContract.UiState(isLoaded = true))
        compose.onNodeWithText(context.getString(R.string.no_contacts)).assertExists()
    }

    @Test
    fun actionRowsNavigate() {
        show(NewMessageContract.UiState(isLoaded = true))
        compose.onNodeWithText(context.getString(R.string.new_group)).performClick()
        compose.onNodeWithText(context.getString(R.string.new_contact)).performClick()
        assertEquals(listOf(NewMessageContract.Intent.OnNewGroup, NewMessageContract.Intent.OnNewContact), intents)
    }

    @Test
    fun contactClickOpensChat() {
        show(NewMessageContract.UiState(contacts = listOf(vali), isLoaded = true))
        compose.onNodeWithText("Vali Valiyev").performClick()
        assertEquals(listOf<NewMessageContract.Intent>(NewMessageContract.Intent.OnContactClick(vali)), intents)
    }

    @Test
    fun longPressAsksBeforeRemoving() {
        show(NewMessageContract.UiState(contacts = listOf(vali), isLoaded = true))
        compose.onNodeWithText("Vali Valiyev").performTouchInput { longClick() }
        compose.onNodeWithText(context.getString(R.string.remove_contact_title, "Vali Valiyev")).assertExists()
        compose.onNodeWithText(context.getString(R.string.remove)).performClick()
        assertEquals(listOf<NewMessageContract.Intent>(NewMessageContract.Intent.OnRemoveContact(vali)), intents)
    }
}
