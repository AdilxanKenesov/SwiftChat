package uz.relay.feature.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import uz.relay.domain.model.CallLog
import uz.relay.domain.model.CallOutcome
import uz.relay.domain.testing.TestData
import uz.relay.feature.conversation.chat.components.CallLogBubble
import uz.relay.feature.conversation.chat.components.ChatTopBar
import uz.relay.feature.conversation.chat.components.GroupCallBanner
import uz.relay.feature.conversation.chat.message

/** Golden rasmlar (src/test/screenshots): `verifyRoborazziDebug` dizayndagi kutilmagan o'zgarishni ushlaydi. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "+uz")
class CallUiScreenshotTest {

    @get:Rule val compose = createComposeRule()

    private fun snap(name: String, dark: Boolean, content: @Composable () -> Unit) {
        // Vaqt/sana matnlari vaqt zonasiga bog'liq — har qanday kompyuterda (CI'da ham) bir xil rasm chiqsin.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tashkent"))
        compose.setContent { SwiftChatTheme(darkTheme = dark) { content() } }
        compose.onRoot().captureRoboImage("src/test/screenshots/${name}_${if (dark) "dark" else "light"}.png", roborazziOptions = ScreenshotOptions)
    }

    /** Sarlavha (qo'ng'iroq tugmalari), video chat banneri va barcha turdagi qo'ng'iroq yozuvlari bitta rasmda. */
    @Composable
    private fun CallUi() {
        Column(modifier = Modifier.background(SwiftTheme.colors.wall)) {
            ChatTopBar(
                chat = TestData.chat(title = "Vali Valiyev"), typingUserIds = emptySet(), names = emptyMap(), memberCount = 0,
                onBack = {}, onTitleClick = {}, onMoreClick = {}, onAudioCall = {}, onVideoCall = {}
            )
            GroupCallBanner(participants = 3, onJoin = {})
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CallLogBubble(message("a", isMine = true), CallLog(false, CallOutcome.ANSWERED, 151))
                CallLogBubble(message("b"), CallLog(true, CallOutcome.MISSED, 0))
                CallLogBubble(message("c", isMine = true), CallLog(false, CallOutcome.DECLINED, 0))
                CallLogBubble(message("d"), CallLog(true, CallOutcome.STARTED, 0, group = true))
                CallLogBubble(message("e"), CallLog(true, CallOutcome.ANSWERED, 754, group = true))
            }
        }
    }

    @Test fun callUiLight() = snap("call_ui", dark = false) { CallUi() }
    @Test fun callUiDark() = snap("call_ui", dark = true) { CallUi() }
}

/**
 * Turli OS'larda (Fedora'da yozilgan golden, CI'da Ubuntu) shrift chetlarining silliqlanishi bir necha pikselga farq
 * qiladi. 1% gacha farq "bir xil" hisoblanadi — joylashuv, rang yoki matn o'zgarsa test baribir yiqiladi.
 */
@OptIn(ExperimentalRoborazziApi::class)
private val ScreenshotOptions = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f))
