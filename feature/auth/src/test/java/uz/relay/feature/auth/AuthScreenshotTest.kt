package uz.relay.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import java.util.TimeZone
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.feature.auth.otp.OtpContract
import uz.relay.feature.auth.otp.OtpScreenContent
import uz.relay.feature.auth.phone.PhoneContract
import uz.relay.feature.auth.phone.PhoneScreenContent
import uz.relay.feature.auth.profile.ProfileSetupContract
import uz.relay.feature.auth.profile.ProfileSetupScreenContent

/**
 * Auth ekranlarining golden rasmlari (src/test/screenshots). Dizayn tasodifan buzilsa — rang, joylashuv, matn
 * kesilishi — `verifyRoborazziDebug` farqni ko'rsatib, testni yiqitadi. Rasmni ataylab o'zgartirganda
 * `recordRoborazziDebug` bilan yangilanadi.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Asosiy til (o'zbekcha, `values/`) — golden rasmlar kompyuter tiliga bog'liq bo'lmasin.
@Config(qualifiers = "+uz")
class AuthScreenshotTest {

    @get:Rule val compose = createComposeRule()

    private fun snap(name: String, dark: Boolean, content: @Composable () -> Unit) {
        // Vaqt/sana matnlari vaqt zonasiga bog'liq — har qanday kompyuterda (CI'da ham) bir xil rasm chiqsin.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tashkent"))
        compose.setContent { SwiftChatTheme(darkTheme = dark) { content() } }
        compose.onRoot().captureRoboImage("src/test/screenshots/${name}_${if (dark) "dark" else "light"}.png", roborazziOptions = ScreenshotOptions)
    }

    private val filledPhone = PhoneContract.UiState(digits = "901234567")

    @Test fun phoneLight() = snap("phone", dark = false) { PhoneScreenContent(filledPhone) {} }
    @Test fun phoneDark() = snap("phone", dark = true) { PhoneScreenContent(filledPhone) {} }

    private val wrongOtp = OtpContract.UiState(phone = "+998901234567", codeLength = 6, code = "123456", status = OtpContract.Status.Wrong(3))

    @Test fun otpWrongLight() = snap("otp_wrong", dark = false) { OtpScreenContent(wrongOtp, emptyFlow()) {} }
    @Test fun otpWrongDark() = snap("otp_wrong", dark = true) { OtpScreenContent(wrongOtp, emptyFlow()) {} }

    private val taken = ProfileSetupContract.UiState(name = "Ali Valiyev", username = "ali", usernameTaken = true, suggestions = listOf("ali_dev", "ali01"))

    @Test fun profileTakenLight() = snap("profile_taken", dark = false) { ProfileSetupScreenContent(taken) {} }
    @Test fun profileTakenDark() = snap("profile_taken", dark = true) { ProfileSetupScreenContent(taken) {} }
}

/**
 * Turli OS'larda (Fedora'da yozilgan golden, CI'da Ubuntu) shrift chetlarining silliqlanishi bir necha pikselga farq
 * qiladi. 1% gacha farq "bir xil" hisoblanadi — joylashuv, rang yoki matn o'zgarsa test baribir yiqiladi.
 */
@OptIn(ExperimentalRoborazziApi::class)
private val ScreenshotOptions = RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f))
