package uz.relay.feature.profile

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
import uz.relay.domain.testing.TestData
import uz.relay.feature.profile.me.MyProfileContent
import uz.relay.feature.profile.me.MyProfileContract
import uz.relay.feature.profile.user.UserProfileContent
import uz.relay.feature.profile.user.UserProfileContract

/** Golden rasmlar (src/test/screenshots): `verifyRoborazziDebug` dizayndagi kutilmagan o'zgarishni ushlaydi. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "+uz")
class ProfileScreenshotTest {

    @get:Rule val compose = createComposeRule()

    private fun snap(name: String, dark: Boolean, content: @Composable () -> Unit) {
        // Vaqt/sana matnlari vaqt zonasiga bog'liq — har qanday kompyuterda (CI'da ham) bir xil rasm chiqsin.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tashkent"))
        compose.setContent { SwiftChatTheme(darkTheme = dark) { content() } }
        compose.onRoot().captureRoboImage("src/test/screenshots/${name}_${if (dark) "dark" else "light"}.png")
    }

    private val me = MyProfileContract.UiState(me = TestData.user(name = "Ali Valiyev", username = "ali"))
    private val user = UserProfileContract.UiState(user = TestData.user(id = "vali", name = "Vali Valiyev", username = "vali"), isContact = true)

    @Test fun myProfileLight() = snap("my_profile", dark = false) { MyProfileContent(me) {} }
    @Test fun myProfileDark() = snap("my_profile", dark = true) { MyProfileContent(me) {} }
    @Test fun userProfileLight() = snap("user_profile", dark = false) { UserProfileContent("vali", user) {} }
    @Test fun userProfileDark() = snap("user_profile", dark = true) { UserProfileContent("vali", user) {} }
}
