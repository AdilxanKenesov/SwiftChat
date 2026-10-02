package uz.relay.feature.conversation.chat.emoji

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * "Faqat emoji" aniqlash — katta ko'rsatish shunga bog'liq. Ekrandagi bitta belgi bitta emoji hisoblanadi:
 * ZWJ oila, teri rangi, bayroq, keycap. ICU `BreakIterator` Android'niki, shuning uchun Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
class EmojiTextTest {

    @Test
    fun countsOneToThreeEmoji() {
        assertEquals(1, emojiOnlyCount("😂"))
        assertEquals(2, emojiOnlyCount("👍❤️"))
        assertEquals(3, emojiOnlyCount("🔥 🔥 🔥"))
    }

    @Test
    fun moreThanThreeIsNormalText() {
        assertNull(emojiOnlyCount("😀😀😀😀"))
    }

    @Test
    fun complexEmojiAreOneEach() {
        assertEquals(1, emojiOnlyCount("👨‍👩‍👧"))
        assertEquals(1, emojiOnlyCount("👍🏽"))
        assertEquals(1, emojiOnlyCount("🇺🇿"))
        assertEquals(1, emojiOnlyCount("1️⃣"))
        assertEquals(2, emojiOnlyCount("❤️☀️"))
    }

    @Test
    fun textOrMixedIsNotEmojiOnly() {
        assertNull(emojiOnlyCount("Salom"))
        assertNull(emojiOnlyCount("Salom 😂"))
        assertNull(emojiOnlyCount("123"))
        assertNull(emojiOnlyCount("#"))
        assertNull(emojiOnlyCount("©"))
        assertNull(emojiOnlyCount("→"))
        assertNull(emojiOnlyCount("   "))
        assertNull(emojiOnlyCount(null))
    }

    @Test
    fun everyCatalogEmojiIsDetected() {
        val notDetected = EmojiCatalog.flatMap { it.emojis }.filter { emojiOnlyCount(it) != 1 }
        assertEquals(emptyList<String>(), notDetected)
    }
}
