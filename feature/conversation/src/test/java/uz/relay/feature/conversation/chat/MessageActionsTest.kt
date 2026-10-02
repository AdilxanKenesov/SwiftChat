package uz.relay.feature.conversation.chat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.relay.domain.model.MessageType

/** Xabar menyusidagi amallar server qoidalariga mos ko'rinishi: tahrirlash, o'chirish, javob berish. */
class MessageActionsTest {

    private val now = 100L * 60 * 60 * 1000
    private val hour = 60L * 60 * 1000

    @Test
    fun `my sent text can be edited within 48 hours`() {
        assertTrue(message("a", isMine = true, createdAt = now - 47 * hour).canEdit(now))
        assertFalse(message("a", isMine = true, createdAt = now - 49 * hour).canEdit(now))
    }

    @Test
    fun `others unsent deleted or non text messages cannot be edited`() {
        assertFalse(message("a", isMine = false, createdAt = now).canEdit(now))
        assertFalse(message("a", isMine = true, createdAt = now, serverId = null).canEdit(now))
        assertFalse(message("a", isMine = true, createdAt = now, isDeleted = true).canEdit(now))
        assertFalse(message("a", isMine = true, createdAt = now, type = MessageType.IMAGE).canEdit(now))
    }

    @Test
    fun `call log text cannot be edited`() {
        assertFalse(message("a", isMine = true, createdAt = now, text = "📞 Call · audio · 2:31").canEdit(now))
    }

    @Test
    fun `delete is allowed for own messages or for group admins`() {
        assertTrue(message("a", isMine = true).canDelete(canDeleteOthers = false))
        assertFalse(message("a", isMine = false).canDelete(canDeleteOthers = false))
        assertTrue(message("a", isMine = false).canDelete(canDeleteOthers = true))
        assertFalse(message("a", isMine = true, serverId = null).canDelete(canDeleteOthers = true))
        assertFalse(message("a", isMine = true, isDeleted = true).canDelete(canDeleteOthers = true))
    }

    @Test
    fun `reply needs a delivered non system message`() {
        assertTrue(message("a").canReply())
        assertFalse(message("a", serverId = null).canReply())
        assertFalse(message("a", isDeleted = true).canReply())
        assertFalse(message("a", type = MessageType.SYSTEM).canReply())
    }
}
