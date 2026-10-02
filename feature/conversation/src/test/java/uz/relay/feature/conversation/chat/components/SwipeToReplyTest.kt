package uz.relay.feature.conversation.chat.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.domain.model.Message
import uz.relay.feature.conversation.chat.ChatItem
import uz.relay.feature.conversation.chat.message

/** Telegram'dagidek xabarni chapga surib javob berish: faqat chapga, faqat javob berish mumkin bo'lgan xabarda. */
@RunWith(RobolectricTestRunner::class)
class SwipeToReplyTest {

    @get:Rule val compose = createComposeRule()

    private var replies = 0

    private fun show(message: Message) = compose.setContent {
        SwiftChatTheme(darkTheme = false) {
            MessageRow(
                item = ChatItem.Bubble(message, replied = null, showSenderName = false, showAvatar = false),
                isGroup = false,
                names = emptyMap(),
                onLongPress = {},
                onReplyClick = {},
                onRetry = {},
                onSwipeReply = { replies++ }
            )
        }
    }

    @Test
    fun swipeLeftStartsReply() {
        show(message("m1", text = "Salom"))
        compose.onRoot().performTouchInput { swipeLeft(startX = right - 10f, endX = left + 10f) }
        compose.waitForIdle()
        assertEquals(1, replies)
    }

    @Test
    fun swipeRightIsLeftForBackGesture() {
        show(message("m1", text = "Salom"))
        compose.onNodeWithText("Salom").performTouchInput { swipeRight() }
        compose.waitForIdle()
        assertEquals(0, replies)
    }

    @Test
    fun shortSwipeIsCancelled() {
        show(message("m1", text = "Salom"))
        compose.onNodeWithText("Salom").performTouchInput { swipeLeft(startX = centerX, endX = centerX - 40f) }
        compose.waitForIdle()
        assertEquals(0, replies)
    }

    @Test
    fun notDeliveredMessageCannotBeRepliedTo() {
        show(message("m1", text = "Salom", serverId = null))
        compose.onRoot().performTouchInput { swipeLeft(startX = right - 10f, endX = left + 10f) }
        compose.waitForIdle()
        assertEquals(0, replies)
    }
}
