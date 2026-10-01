package uz.relay.feature.calls.call

import android.content.Context
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.orbitmvi.orbit.test.test
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import uz.relay.domain.testing.FakeMessageRepository
import uz.relay.domain.testing.MainDispatcherRule
import uz.relay.domain.usecase.message.SendTextMessageUseCase

/**
 * Stream client ulanmagan holat (API key yo'q yoki hali login qilinmagan): qo'ng'iroq ekrani ochilsa ham ilova
 * yiqilmasligi, "qo'ng'iroqlar ishlamayapti" holatini ko'rsatishi va istalgan harakatda ekranni yopishi kerak.
 * Stream SDK obyektlari bilan ishlaydigan qismlar (qabul qilish, tugatish, tarix yozuvi) real qurilmada sinaladi.
 */
@RunWith(RobolectricTestRunner::class)
class CallViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val context: Context = RuntimeEnvironment.getApplication()
    private val messages = FakeMessageRepository()
    private var backs = 0
    private val directions = object : CallContract.Directions {
        override suspend fun back() { backs++ }
    }

    private fun viewModel(video: Boolean? = true, chatId: String? = "chat", group: Boolean = false) =
        CallViewModel("call-1", video, chatId, group, context, SendTextMessageUseCase(messages), directions)

    @Test
    fun `without stream client the screen is marked unavailable`() {
        val vm = viewModel()
        assertTrue(vm.container.stateFlow.value.unavailable)
    }

    @Test
    fun `group call is always video`() {
        val state = viewModel(video = false, group = true).container.stateFlow.value
        assertTrue(state.isVideo)
        assertTrue(state.isGroup)
    }

    @Test
    fun `audio call keeps audio mode`() {
        assertEquals(false, viewModel(video = false).container.stateFlow.value.isVideo)
    }

    @Test
    fun `any action closes an unavailable call and writes no call log`() = runTest {
        viewModel().test(this) {
            expectInitialState()
            containerHost.onEventDispatcher(CallContract.Intent.OnBack)
            containerHost.onEventDispatcher(CallContract.Intent.OnFinished)
        }
        assertEquals(2, backs)
        assertTrue(messages.sentTexts.isEmpty())
    }
}
