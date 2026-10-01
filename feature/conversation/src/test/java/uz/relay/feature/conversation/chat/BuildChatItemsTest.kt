package uz.relay.feature.conversation.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.relay.domain.model.MessageType
import uz.relay.feature.conversation.util.dayStartMillis
import java.time.LocalDate
import java.time.ZoneId

/**
 * Ro'yxat teskari (eng yangisi birinchi) chiziladi — sana ajratgichi va "ketma-ketlik" bayroqlarida adashish oson.
 * Vaqtlar telefon vaqt zonasida hisoblanadi, shuning uchun testda ham shu zona ishlatiladi.
 */
class BuildChatItemsTest {

    private val zone = ZoneId.systemDefault()
    private val day1 = LocalDate.of(2026, 9, 1).atTime(10, 0).atZone(zone).toInstant().toEpochMilli()
    private val day2 = LocalDate.of(2026, 9, 2).atTime(10, 0).atZone(zone).toInstant().toEpochMilli()

    private fun List<ChatItem>.bubbles() = filterIsInstance<ChatItem.Bubble>()

    @Test
    fun `date separator goes after oldest message of each day`() {
        // Eng yangisi birinchi: ikkinchi kunning ikki xabari, keyin birinchi kunning bittasi.
        val messages = listOf(message("c", createdAt = day2 + 2), message("b", createdAt = day2 + 1), message("a", createdAt = day1))
        val items = buildChatItems(messages, isGroup = false)
        assertEquals(listOf("c", "b", "date-${dayStartMillis(day2)}", "a", "date-${dayStartMillis(day1)}"), items.map { it.key })
    }

    @Test
    fun `direct chat never shows sender name or avatar`() {
        val items = buildChatItems(listOf(message("b"), message("a")), isGroup = false).bubbles()
        assertTrue(items.none { it.showSenderName || it.showAvatar })
    }

    @Test
    fun `group run shows name on oldest and avatar on newest`() {
        // Ekranda yuqoridan pastga: a (eski), b, c (yangi) — hammasi Ali'dan.
        val messages = listOf(message("c", createdAt = day1 + 3), message("b", createdAt = day1 + 2), message("a", createdAt = day1 + 1))
        val byId = buildChatItems(messages, isGroup = true).bubbles().associateBy { it.message.clientMessageId }
        assertTrue(byId.getValue("a").showSenderName)
        assertFalse(byId.getValue("a").showAvatar)
        assertFalse(byId.getValue("b").showSenderName)
        assertFalse(byId.getValue("b").showAvatar)
        assertFalse(byId.getValue("c").showSenderName)
        assertTrue(byId.getValue("c").showAvatar)
    }

    @Test
    fun `new sender starts a new run`() {
        val messages = listOf(message("b", senderId = "vali", createdAt = day1 + 2), message("a", senderId = "ali", createdAt = day1 + 1))
        val byId = buildChatItems(messages, isGroup = true).bubbles().associateBy { it.message.clientMessageId }
        assertTrue(byId.getValue("a").showSenderName && byId.getValue("a").showAvatar)
        assertTrue(byId.getValue("b").showSenderName && byId.getValue("b").showAvatar)
    }

    @Test
    fun `new day breaks the run even for same sender`() {
        val messages = listOf(message("b", createdAt = day2), message("a", createdAt = day1))
        val byId = buildChatItems(messages, isGroup = true).bubbles().associateBy { it.message.clientMessageId }
        assertTrue(byId.getValue("b").showSenderName)
        assertTrue(byId.getValue("a").showAvatar)
    }

    @Test
    fun `my messages in group have no name or avatar`() {
        val items = buildChatItems(listOf(message("a", senderId = "me", isMine = true)), isGroup = true).bubbles()
        assertFalse(items.single().showSenderName)
        assertFalse(items.single().showAvatar)
    }

    @Test
    fun `system message becomes centered chip and breaks the run`() {
        val messages = listOf(
            message("c", createdAt = day1 + 3),
            message("sys", type = MessageType.SYSTEM, text = null, createdAt = day1 + 2),
            message("a", createdAt = day1 + 1)
        )
        val items = buildChatItems(messages, isGroup = true)
        assertTrue(items[1] is ChatItem.System)
        val byId = items.bubbles().associateBy { it.message.clientMessageId }
        assertTrue(byId.getValue("c").showSenderName)
        assertTrue(byId.getValue("a").showAvatar)
    }

    @Test
    fun `reply is resolved from loaded messages`() {
        val original = message("a", createdAt = day1)
        val reply = message("b", createdAt = day1 + 1, replyTo = "a")
        val missing = message("c", createdAt = day1 + 2, replyTo = "not-loaded")
        val byId = buildChatItems(listOf(missing, reply, original), isGroup = false).bubbles().associateBy { it.message.clientMessageId }
        assertEquals(original, byId.getValue("b").replied)
        assertEquals(null, byId.getValue("c").replied)
    }

    @Test
    fun `empty list gives no items`() {
        assertTrue(buildChatItems(emptyList(), isGroup = true).isEmpty())
    }
}
