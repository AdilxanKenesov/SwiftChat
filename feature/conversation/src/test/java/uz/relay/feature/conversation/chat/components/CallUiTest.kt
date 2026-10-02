package uz.relay.feature.conversation.chat.components

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.domain.model.CallLog
import uz.relay.domain.model.CallOutcome
import uz.relay.domain.model.ChatType
import uz.relay.domain.testing.TestData
import uz.relay.feature.conversation.R
import uz.relay.feature.conversation.chat.message

/** Chatdagi qo'ng'iroq UI'i: tarix yozuvi sarlavhalari, guruh video chati banneri, sarlavhadagi qo'ng'iroq tugmalari. */
@RunWith(RobolectricTestRunner::class)
class CallUiTest {

    @get:Rule val compose = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun outgoingAnsweredCallShowsTitleAndDuration() {
        compose.setContent {
            SwiftChatTheme(darkTheme = false) {
                CallLogBubble(message = message("c1", isMine = true), log = CallLog(video = false, outcome = CallOutcome.ANSWERED, durationSeconds = 151))
            }
        }
        compose.onNodeWithText(context.getString(R.string.call_outgoing)).assertExists()
        compose.onNodeWithText("2:31").assertExists()
    }

    @Test
    fun missedIncomingCallTitle() {
        compose.setContent {
            SwiftChatTheme(darkTheme = false) {
                CallLogBubble(message = message("c1", isMine = false), log = CallLog(video = true, outcome = CallOutcome.MISSED, durationSeconds = 0))
            }
        }
        compose.onNodeWithText(context.getString(R.string.call_missed)).assertExists()
    }

    @Test
    fun groupCallLogTitles() {
        compose.setContent {
            SwiftChatTheme(darkTheme = false) {
                Column {
                    CallLogBubble(message = message("a"), log = CallLog(true, CallOutcome.STARTED, 0, group = true))
                    CallLogBubble(message = message("b"), log = CallLog(true, CallOutcome.ANSWERED, 754, group = true))
                }
            }
        }
        compose.onNodeWithText(context.getString(R.string.call_group_started)).assertExists()
        compose.onNodeWithText(context.getString(R.string.call_group_ended)).assertExists()
        compose.onNodeWithText("12:34").assertExists()
    }

    @Test
    fun groupCallBannerShowsCountAndJoins() {
        var joined = 0
        compose.setContent { SwiftChatTheme(darkTheme = false) { GroupCallBanner(participants = 3, onJoin = { joined++ }) } }
        compose.onNodeWithText(context.resources.getQuantityString(R.plurals.group_call_participants, 3, 3)).assertExists()
        compose.onNodeWithText(context.getString(R.string.group_call_join)).performClick()
        assertEquals(1, joined)
    }

    @Test
    fun directChatHasAudioAndVideoButtons() {
        val calls = mutableListOf<String>()
        compose.setContent {
            SwiftChatTheme(darkTheme = false) {
                ChatTopBar(
                    chat = TestData.chat(title = "Vali"), typingUserIds = emptySet(), names = emptyMap(), memberCount = 0,
                    onBack = {}, onTitleClick = {}, onMoreClick = {},
                    onAudioCall = { calls += "audio" }, onVideoCall = { calls += "video" }
                )
            }
        }
        compose.onNodeWithContentDescription(context.getString(R.string.audio_call)).performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.video_call)).performClick()
        assertEquals(listOf("audio", "video"), calls)
    }

    @Test
    fun groupChatHasOnlyVideoButton() {
        compose.setContent {
            SwiftChatTheme(darkTheme = false) {
                ChatTopBar(
                    chat = TestData.chat(type = ChatType.GROUP, title = "Oila", peerUserId = null), typingUserIds = emptySet(),
                    names = emptyMap(), memberCount = 3, onBack = {}, onTitleClick = {}, onMoreClick = {},
                    onAudioCall = null, onVideoCall = {}
                )
            }
        }
        compose.onNodeWithContentDescription(context.getString(R.string.video_call)).assertExists()
        compose.onNodeWithContentDescription(context.getString(R.string.audio_call)).assertDoesNotExist()
    }
}
