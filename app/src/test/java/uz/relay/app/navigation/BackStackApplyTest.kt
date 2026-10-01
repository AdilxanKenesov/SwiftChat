package uz.relay.app.navigation

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Test
import uz.relay.core.navigation.AppNavigationParam
import uz.relay.core.navigation.key.ChatKey
import uz.relay.core.navigation.key.ChatsKey
import uz.relay.core.navigation.key.GroupInfoKey
import uz.relay.core.navigation.key.MyProfileKey
import uz.relay.core.navigation.key.PhoneKey
import uz.relay.core.navigation.key.SearchKey

/**
 * Navigatsiya buyruqlarining back stack'ga ta'siri. `apply` sof funksiya — har bir ekran o'tishi shunga tayanadi,
 * xato bo'lsa foydalanuvchi "orqaga" bosganda noto'g'ri ekranga tushadi yoki ilova bo'sh stek bilan yiqiladi.
 */
class BackStackApplyTest {

    private fun stack(vararg keys: NavKey) = mutableListOf(*keys)

    @Test
    fun `to adds a screen on top`() {
        val s = stack(ChatsKey).apply { apply(AppNavigationParam.To(SearchKey)) }
        assertEquals(listOf(ChatsKey, SearchKey), s)
    }

    @Test
    fun `to with single top ignores double taps`() {
        val s = stack(ChatsKey, ChatKey("c1")).apply { apply(AppNavigationParam.To(ChatKey("c1"))) }
        assertEquals(listOf(ChatsKey, ChatKey("c1")), s)
    }

    @Test
    fun `to without single top allows the same screen twice`() {
        val s = stack(ChatsKey, ChatKey("c1")).apply { apply(AppNavigationParam.To(ChatKey("c1"), singleTop = false)) }
        assertEquals(listOf(ChatsKey, ChatKey("c1"), ChatKey("c1")), s)
    }

    @Test
    fun `replace swaps the top screen`() {
        val s = stack(ChatsKey, SearchKey).apply { apply(AppNavigationParam.Replace(ChatKey("c1"))) }
        assertEquals(listOf(ChatsKey, ChatKey("c1")), s)
    }

    @Test
    fun `back never removes the last screen`() {
        val s = stack(ChatsKey, SearchKey).apply {
            apply(AppNavigationParam.Back)
            apply(AppNavigationParam.Back)
        }
        assertEquals(listOf(ChatsKey), s)
    }

    @Test
    fun `back to keeps the target and optionally removes it`() {
        val keep = stack(ChatsKey, ChatKey("c1"), GroupInfoKey("c1"), SearchKey).apply { apply(AppNavigationParam.BackTo(ChatKey("c1"))) }
        assertEquals(listOf(ChatsKey, ChatKey("c1")), keep)
        val inclusive = stack(ChatsKey, ChatKey("c1"), SearchKey).apply { apply(AppNavigationParam.BackTo(ChatKey("c1"), inclusive = true)) }
        assertEquals(listOf(ChatsKey), inclusive)
    }

    @Test
    fun `back to a missing screen does nothing`() {
        val s = stack(ChatsKey, SearchKey).apply { apply(AppNavigationParam.BackTo(MyProfileKey)) }
        assertEquals(listOf(ChatsKey, SearchKey), s)
    }

    @Test
    fun `inclusive back to the root keeps one screen`() {
        val s = stack(ChatsKey, SearchKey).apply { apply(AppNavigationParam.BackTo(ChatsKey, inclusive = true)) }
        assertEquals(listOf(ChatsKey), s)
    }

    @Test
    fun `back to or to returns to an existing screen instead of duplicating it`() {
        val existing = stack(ChatsKey, ChatKey("c1"), MyProfileKey).apply { apply(AppNavigationParam.BackToOrTo(ChatKey("c1"))) }
        assertEquals(listOf(ChatsKey, ChatKey("c1")), existing)
        val missing = stack(ChatsKey, MyProfileKey).apply { apply(AppNavigationParam.BackToOrTo(ChatKey("c2"))) }
        assertEquals(listOf(ChatsKey, MyProfileKey, ChatKey("c2")), missing)
    }

    @Test
    fun `reset to clears the stack`() {
        val s = stack(ChatsKey, ChatKey("c1"), MyProfileKey).apply { apply(AppNavigationParam.ResetTo(PhoneKey)) }
        assertEquals(listOf(PhoneKey), s)
    }
}
