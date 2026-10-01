package uz.relay.core.designsystem.component

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uz.relay.core.designsystem.theme.SwiftChatTheme

/** Ilova bo'yicha yagona dialoglar: tugmalar to'g'ri callback'ni chaqiradi, kiritish dialogi yaroqsiz matnni o'tkazmaydi. */
@RunWith(RobolectricTestRunner::class)
class SwiftDialogTest {

    @get:Rule val compose = createComposeRule()

    @Test
    fun confirmAndDismissCallTheirCallbacks() {
        val events = mutableListOf<String>()
        compose.setContent {
            SwiftChatTheme(darkTheme = false) {
                SwiftDialog(
                    title = "Chiqasizmi?",
                    confirmText = "Chiqish",
                    dismissText = "Bekor qilish",
                    onConfirm = { events += "confirm" },
                    onDismiss = { events += "dismiss" }
                )
            }
        }
        compose.onNodeWithText("Chiqasizmi?").assertExists()
        compose.onNodeWithText("Bekor qilish").performClick()
        compose.onNodeWithText("Chiqish").performClick()
        assertEquals(listOf("dismiss", "confirm"), events)
    }

    @Test
    fun inputDialogEnablesConfirmOnlyForChangedValidText() {
        var confirmed: String? = null
        compose.setContent {
            SwiftChatTheme(darkTheme = false) {
                SwiftInputDialog(
                    title = "Nomini o'zgartirish",
                    label = "Guruh nomi",
                    initialValue = "Oila",
                    confirmText = "Saqlash",
                    dismissText = "Bekor qilish",
                    onConfirm = { confirmed = it },
                    onDismiss = {}
                )
            }
        }
        // Boshlang'ich qiymat o'zgarmagan — saqlash o'chiq.
        compose.onNodeWithText("Saqlash").assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextReplacement("   ")
        compose.onNodeWithText("Saqlash").assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextReplacement("  Oila 2 ")
        compose.onNodeWithText("Saqlash").assertIsEnabled().performClick()
        assertEquals("Oila 2", confirmed)
    }
}
