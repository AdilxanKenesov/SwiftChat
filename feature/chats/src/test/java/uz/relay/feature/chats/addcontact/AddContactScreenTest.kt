package uz.relay.feature.chats.addcontact

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.domain.testing.TestData
import uz.relay.feature.chats.R

/** Kontakt qo'shish: natijalar, "Qo'shish" tugmasi, allaqachon kontakt bo'lganlar belgisi, "hech narsa topilmadi". */
@RunWith(RobolectricTestRunner::class)
class AddContactScreenTest {

    @get:Rule val compose = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val intents = mutableListOf<AddContactContract.Intent>()
    private val vali = TestData.user(id = "vali", name = "Vali Valiyev", username = "vali")

    private fun show(state: AddContactContract.UiState) = compose.setContent {
        SwiftChatTheme(darkTheme = false) { AddContactContent(uiState = state, onEventDispatcher = { intents += it }) }
    }

    @Test
    fun addButtonAddsUser() {
        show(AddContactContract.UiState(query = "vali", results = listOf(vali), searchedQuery = "vali"))
        compose.onNodeWithText(context.getString(R.string.add)).performClick()
        assertEquals(listOf<AddContactContract.Intent>(AddContactContract.Intent.OnAdd(vali)), intents)
    }

    @Test
    fun existingContactIsMarked() {
        show(AddContactContract.UiState(query = "vali", results = listOf(vali), searchedQuery = "vali", contactIds = setOf("vali")))
        compose.onNodeWithContentDescription(context.getString(R.string.in_contacts)).assertExists()
        compose.onNodeWithText(context.getString(R.string.add)).assertDoesNotExist()
    }

    @Test
    fun nothingFound() {
        show(AddContactContract.UiState(query = "zzz", searchedQuery = "zzz"))
        compose.onNodeWithText(context.getString(R.string.nothing_found)).assertExists()
    }
}
