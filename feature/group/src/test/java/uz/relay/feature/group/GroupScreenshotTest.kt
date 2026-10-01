package uz.relay.feature.group

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
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
import uz.relay.domain.model.ChatType
import uz.relay.domain.model.MemberRole
import uz.relay.domain.testing.TestData
import uz.relay.feature.group.info.GroupInfoContent
import uz.relay.feature.group.info.GroupInfoContract

/** Golden rasmlar (src/test/screenshots): `verifyRoborazziDebug` dizayndagi kutilmagan o'zgarishni ushlaydi. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "+uz")
class GroupScreenshotTest {

    @get:Rule val compose = createComposeRule()

    private fun snap(name: String, dark: Boolean, content: @Composable () -> Unit) {
        // Vaqt/sana matnlari vaqt zonasiga bog'liq — har qanday kompyuterda (CI'da ham) bir xil rasm chiqsin.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tashkent"))
        compose.setContent { SwiftChatTheme(darkTheme = dark) { content() } }
        compose.onRoot().captureRoboImage("src/test/screenshots/${name}_${if (dark) "dark" else "light"}.png", roborazziOptions = ScreenshotOptions)
    }

    private val info = GroupInfoContract.UiState(
        chat = TestData.chat(id = "g1", type = ChatType.GROUP, title = "Oila guruhi", peerUserId = null),
        members = listOf(
            TestData.member("me", MemberRole.OWNER, isMe = true),
            TestData.member("vali", MemberRole.ADMIN),
            TestData.member("malika")
        )
    )

    @Test fun groupInfoLight() = snap("group_info", dark = false) { GroupInfoContent(info) {} }
    @Test fun groupInfoDark() = snap("group_info", dark = true) { GroupInfoContent(info) {} }
}

/**
 * Turli OS'larda (Fedora'da yozilgan golden, CI'da Ubuntu) shrift chetlarining silliqlanishi bir necha pikselga farq
 * qiladi. 1% gacha farq "bir xil" hisoblanadi — joylashuv, rang yoki matn o'zgarsa test baribir yiqiladi.
 */
@OptIn(ExperimentalRoborazziApi::class)
private val ScreenshotOptions = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f))
