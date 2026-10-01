package uz.relay.feature.chats.list

import android.content.Context
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.domain.model.ChatType
import uz.relay.domain.model.MuteDuration
import uz.relay.domain.testing.TestData
import uz.relay.feature.chats.R

/** Chatlar ro'yxati: bo'sh holat, qatorlar, tablar va long-press bilan ovozsiz qilish menyusi. */
@RunWith(RobolectricTestRunner::class)
class ChatsScreenTest {

    @get:Rule val compose = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val intents = mutableListOf<ChatsContract.Intent>()

    private val direct = TestData.chat(id = "d1", title = "Vali Valiyev", peerUserId = "vali")
    private val group = TestData.chat(id = "g1", type = ChatType.GROUP, title = "Oila guruhi", peerUserId = null)

    /**
     * Tablar HorizontalPager'da — qo'shni sahifalar ham oldindan chiziladi, shuning uchun bitta chat bir necha
     * sahifada topiladi. Ekranda haqiqatan ko'rinib turganini olamiz.
     */
    private fun visible(text: String): SemanticsNodeInteraction {
        val nodes = compose.onAllNodesWithText(text)
        val count = nodes.fetchSemanticsNodes().size
        return (0 until count).map { nodes[it] }.first { runCatching { it.assertIsDisplayed() }.isSuccess }
    }

    private fun isVisible(text: String): Boolean = runCatching { visible(text) }.isSuccess

    private fun show(state: ChatsContract.UiState) = compose.setContent {
        SwiftChatTheme(darkTheme = false) { ChatsScreenContent(uiState = state, onEventDispatcher = { intents += it }) }
    }

    @Test
    fun emptyStateAfterBootstrap() {
        show(ChatsContract.UiState(isBootstrapped = true, me = TestData.user()))
        compose.onNodeWithText(context.getString(R.string.no_chats)).assertExists()
    }

    @Test
    fun chatClickOpensChat() {
        show(ChatsContract.UiState(chats = listOf(direct, group), isBootstrapped = true, me = TestData.user()))
        visible("Vali Valiyev").performClick()
        assertEquals(listOf<ChatsContract.Intent>(ChatsContract.Intent.OnChatClick("d1")), intents)
    }

    @Test
    fun groupsTabShowsOnlyGroups() {
        show(ChatsContract.UiState(chats = listOf(direct, group), isBootstrapped = true, me = TestData.user()))
        compose.onNodeWithText(context.getString(R.string.tab_groups)).performClick()
        compose.waitForIdle()
        assertTrue(isVisible("Oila guruhi"))
        assertFalse(isVisible("Vali Valiyev"))
    }

    @Test
    fun longPressOpensMuteSheet() {
        show(ChatsContract.UiState(chats = listOf(direct), isBootstrapped = true, me = TestData.user()))
        visible("Vali Valiyev").performTouchInput { longClick() }
        compose.onNodeWithText(context.getString(R.string.mute_8h)).performClick()
        assertEquals(listOf<ChatsContract.Intent>(ChatsContract.Intent.OnMute("d1", MuteDuration.EIGHT_HOURS)), intents)
    }

    @Test
    fun searchAndNewChatButtons() {
        show(ChatsContract.UiState(chats = listOf(direct), isBootstrapped = true, me = TestData.user()))
        compose.onNodeWithContentDescription(context.getString(R.string.search)).performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.new_chat)).performClick()
        assertEquals(listOf(ChatsContract.Intent.OnSearchClick, ChatsContract.Intent.OnNewMessageClick), intents)
    }
}
