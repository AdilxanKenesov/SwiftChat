package uz.relay.feature.group.info

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uz.relay.core.designsystem.theme.SwiftChatTheme
import uz.relay.domain.model.ChatType
import uz.relay.domain.model.MemberRole
import uz.relay.domain.testing.TestData
import uz.relay.feature.group.R

/** Guruh ma'lumoti: boshqaruv tugmalari faqat admin/egasiga, chiqishdan oldin tasdiq. */
@RunWith(RobolectricTestRunner::class)
class GroupInfoScreenTest {

    @get:Rule val compose = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val intents = mutableListOf<GroupInfoContract.Intent>()
    private val group = TestData.chat(id = "g1", type = ChatType.GROUP, title = "Oila guruhi", peerUserId = null)

    private fun state(myRole: MemberRole) = GroupInfoContract.UiState(
        chat = group,
        members = listOf(TestData.member("me", myRole, isMe = true), TestData.member("vali"))
    )

    private fun show(state: GroupInfoContract.UiState) = compose.setContent {
        SwiftChatTheme(darkTheme = false) { GroupInfoContent(uiState = state, onEventDispatcher = { intents += it }) }
    }

    @Test
    fun adminSeesManageActions() {
        show(state(MemberRole.ADMIN))
        compose.onNodeWithText("Oila guruhi").assertExists()
        compose.onNodeWithContentDescription(context.getString(R.string.edit)).assertExists()
        compose.onNodeWithText(context.getString(R.string.add_members)).performScrollTo().performClick()
        assertEquals(listOf<GroupInfoContract.Intent>(GroupInfoContract.Intent.OnAddMembers), intents)
    }

    @Test
    fun memberDoesNotSeeManageActions() {
        show(state(MemberRole.MEMBER))
        compose.onNodeWithContentDescription(context.getString(R.string.edit)).assertDoesNotExist()
        compose.onNodeWithText(context.getString(R.string.add_members)).assertDoesNotExist()
    }

    @Test
    fun leaveAsksBeforeLeaving() {
        show(state(MemberRole.MEMBER))
        compose.onNodeWithText(context.getString(R.string.leave_group)).performScrollTo().performClick()
        compose.onNodeWithText(context.getString(R.string.leave_title)).assertExists()
        compose.onNodeWithText(context.getString(R.string.leave_confirm)).performClick()
        assertEquals(listOf<GroupInfoContract.Intent>(GroupInfoContract.Intent.OnLeave), intents)
    }
}
