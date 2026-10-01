package uz.relay.feature.chats

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import java.util.TimeZone
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.domain.model.ChatType
import uz.relay.domain.testing.TestData
import uz.relay.feature.chats.list.ChatsContract
import uz.relay.feature.chats.list.ChatsScreenContent
import uz.relay.feature.chats.newmessage.NewMessageContent
import uz.relay.feature.chats.newmessage.NewMessageContract

/** Golden rasmlar (src/test/screenshots): `verifyRoborazziDebug` dizayndagi kutilmagan o'zgarishni ushlaydi. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "+uz")
class ChatsScreenshotTest {

    @get:Rule val compose = createComposeRule()

    private fun snap(name: String, dark: Boolean, content: @Composable () -> Unit) {
        // Vaqt/sana matnlari vaqt zonasiga bog'liq — har qanday kompyuterda (CI'da ham) bir xil rasm chiqsin.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tashkent"))
        compose.setContent { SwiftChatTheme(darkTheme = dark) { content() } }
        compose.onRoot().captureRoboImage("src/test/screenshots/${name}_${if (dark) "dark" else "light"}.png")
    }

    private val chats = ChatsContract.UiState(
        chats = listOf(
            TestData.chat(id = "d1", title = "Vali Valiyev", peerUserId = "vali").copy(unreadCount = 3),
            TestData.chat(id = "g1", type = ChatType.GROUP, title = "Oila guruhi", peerUserId = null).copy(muted = true),
            TestData.chat(id = "d2", title = "Malika", peerUserId = "malika")
        ),
        me = TestData.user(),
        isBootstrapped = true
    )
    private val empty = ChatsContract.UiState(me = TestData.user(), isBootstrapped = true)
    private val contacts = NewMessageContract.UiState(
        contacts = listOf(TestData.user(id = "vali", name = "Vali Valiyev"), TestData.user(id = "malika", name = "Malika")),
        isLoaded = true
    )

    @Test fun chatsLight() = snap("chats", dark = false) { ChatsScreenContent(chats) {} }
    @Test fun chatsDark() = snap("chats", dark = true) { ChatsScreenContent(chats) {} }
    @Test fun chatsEmptyLight() = snap("chats_empty", dark = false) { ChatsScreenContent(empty) {} }
    @Test fun newMessageLight() = snap("new_message", dark = false) { NewMessageContent(contacts) {} }
    @Test fun newMessageDark() = snap("new_message", dark = true) { NewMessageContent(contacts) {} }
}
