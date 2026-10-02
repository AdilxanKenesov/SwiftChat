package uz.relay.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/** "So'nggi" emojilar: oxirgisi birinchi, takrorlanmaydi, eng ko'pi 24 ta. */
class RecentEmojisTest {

    @Test
    fun `new emoji goes first`() {
        assertEquals(listOf("😂", "👍"), RecentEmojis.push(listOf("👍"), "😂"))
    }

    @Test
    fun `used again moves to front without duplicate`() {
        assertEquals(listOf("👍", "😂", "❤️"), RecentEmojis.push(listOf("😂", "👍", "❤️"), "👍"))
    }

    @Test
    fun `list is capped`() {
        val full = (1..RecentEmojis.MAX).map { "e$it" }
        val result = RecentEmojis.push(full, "new")
        assertEquals(RecentEmojis.MAX, result.size)
        assertEquals("new", result.first())
        assertEquals("e${RecentEmojis.MAX - 1}", result.last())
    }
}
