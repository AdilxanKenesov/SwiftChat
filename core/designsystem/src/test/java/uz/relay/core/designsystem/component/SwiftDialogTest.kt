package uz.relay.core.designsystem.component

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uz.relay.core.designsystem.theme.SwiftChatTheme

/** Ilova bo'yicha yagona tasdiqlash dialogi: tugmalar to'g'ri callback'ni chaqiradi. */
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

    // SwiftInputDialog bu yerda test qilinmaydi: maydon ochilishi bilan fokus oladi va Robolectric'da kursor
    // miltillashi Compose test soatini doim "band" qiladi (AppNotIdleException). Uning qoidasi — yaroqli va
    // o'zgargan nomgina saqlanadi — guruh nomini o'zgartirish ViewModel testlarida tekshirilgan.
}
