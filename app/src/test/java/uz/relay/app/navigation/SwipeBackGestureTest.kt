package uz.relay.app.navigation

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * O'ngga surib orqaga qaytish: gesture tizimdagi "orqaga" bilan bir xil yo'ldan o'tadi, shuning uchun ekrandagi
 * oddiy `BackHandler` ham uni oladi. Uzun surish — orqaga, qisqa surish va chapga surish — hech narsa.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class) // Hilt'li App kerak emas — faqat gesture tekshiriladi.
class SwipeBackGestureTest {

    @get:Rule val compose = createComposeRule()

    private var backs = 0

    private fun show(enabled: Boolean = true) = compose.setContent {
        BackHandler { backs++ }
        Box(modifier = Modifier.fillMaxSize().swipeBack(enabled))
    }

    @Test
    fun longSwipeRightGoesBack() {
        show()
        compose.onRoot().performTouchInput { swipeRight(startX = left + 10f, endX = right - 10f) }
        compose.waitForIdle()
        assertEquals(1, backs)
    }

    @Test
    fun shortSlowSwipeIsCancelled() {
        show()
        compose.onRoot().performTouchInput { swipeRight(startX = left + 10f, endX = left + width * 0.15f, durationMillis = 800) }
        compose.waitForIdle()
        assertEquals(0, backs)
    }

    @Test
    fun swipeLeftDoesNothing() {
        show()
        compose.onRoot().performTouchInput { swipeLeft() }
        compose.waitForIdle()
        assertEquals(0, backs)
    }

    @Test
    fun disabledOnRootScreens() {
        show(enabled = false)
        compose.onRoot().performTouchInput { swipeRight(startX = left + 10f, endX = right - 10f) }
        compose.waitForIdle()
        assertEquals(0, backs)
    }
}
