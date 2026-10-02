package uz.relay.feature.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import java.util.TimeZone
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.core.designsystem.theme.SwiftTheme
import uz.relay.feature.conversation.chat.ChatItem
import uz.relay.feature.conversation.chat.components.EmojiPanel
import uz.relay.feature.conversation.chat.components.MessageRow
import uz.relay.feature.conversation.chat.message

/** Katta emoji xabarlari (1, 2, 3 ta; kiruvchi va chiquvchi) va emoji paneli — golden rasmlar. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "+uz")
class EmojiScreenshotTest {

    @get:Rule val compose = createComposeRule()

    private fun snap(name: String, dark: Boolean, content: @Composable () -> Unit) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tashkent"))
        compose.setContent { SwiftChatTheme(darkTheme = dark) { content() } }
        compose.onRoot().captureRoboImage("src/test/screenshots/${name}_${if (dark) "dark" else "light"}.png", roborazziOptions = Options)
    }

    @Composable
    private fun EmojiMessages() {
        Column(
            modifier = Modifier.fillMaxWidth().background(SwiftTheme.colors.wall).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                message("a", text = "😂"),
                message("b", isMine = true, text = "👍❤️"),
                message("c", text = "🔥🔥🔥"),
                message("d", isMine = true, text = "Salom 😊")
            ).forEach { msg ->
                MessageRow(
                    item = ChatItem.Bubble(msg, replied = null, showSenderName = false, showAvatar = false),
                    isGroup = false, names = emptyMap(), onLongPress = {}, onReplyClick = {}, onRetry = {}
                )
            }
        }
    }

    @Test fun emojiMessagesLight() = snap("emoji_messages", dark = false) { EmojiMessages() }
    @Test fun emojiMessagesDark() = snap("emoji_messages", dark = true) { EmojiMessages() }
    @Test fun emojiPanelLight() = snap("emoji_panel", dark = false) { EmojiPanel(recent = listOf("😂", "👍", "❤️"), height = 300.dp, onEmoji = {}) }
    @Test fun emojiPanelDark() = snap("emoji_panel", dark = true) { EmojiPanel(recent = listOf("😂", "👍", "❤️"), height = 300.dp, onEmoji = {}) }
}

@OptIn(ExperimentalRoborazziApi::class)
private val Options = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f))
