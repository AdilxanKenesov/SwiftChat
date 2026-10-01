package uz.relay.data.mapper

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.relay.data.source.local.database.entity.MediaItemEntity
import uz.relay.data.source.local.database.entity.MessageEntity
import uz.relay.data.source.local.database.entity.SendStatus
import uz.relay.data.source.local.database.entity.UploadEntity
import uz.relay.domain.model.MediaKind
import uz.relay.domain.model.MessageStatus
import uz.relay.domain.model.MessageType

/** Bazadagi xabar → ekrandagi xabar: kimniki, holati, SYSTEM hodisasi, media va yuklash progressi. */
class MessageMapperTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val me = "me"

    private fun entity(
        senderId: String = me,
        type: String = "TEXT",
        body: String? = "Salom",
        status: SendStatus = SendStatus.SENT,
        serverSeq: Long? = 10,
        editedAt: Long? = null,
        deletedAt: Long? = null,
        media: List<MediaItemEntity> = emptyList()
    ) = MessageEntity(
        clientMessageId = "c1",
        chatId = "chat",
        senderId = senderId,
        serverId = serverSeq,
        serverSeq = serverSeq,
        type = type,
        body = body,
        replyToClientMessageId = null,
        createdAt = 1_000,
        editedAt = editedAt,
        editVersion = 0,
        deletedAt = deletedAt,
        status = status,
        sendError = null,
        media = media
    )

    private fun upload(mediaId: String? = null, completed: Boolean = false, confirmed: Long = 40) = UploadEntity(
        clientMessageId = "c1",
        chatId = "chat",
        localPath = "/local/photo.jpg",
        posterPath = null,
        kind = "IMAGE",
        mimeType = "image/jpeg",
        sizeBytes = 100,
        sha256 = "hash",
        width = 10,
        height = 20,
        durationMs = null,
        thumbBase64 = null,
        uploadId = "u1",
        mediaId = mediaId,
        chunkSize = 50,
        confirmedBytes = confirmed,
        completed = completed
    )

    @Test
    fun `my message status comes from peer cursors`() {
        val message = entity(serverSeq = 10).toDomain(me, PeerCursors(readUpToSeq = 10), json)
        assertTrue(message.isMine)
        assertEquals(MessageStatus.READ, message.status)
    }

    @Test
    fun `others message is not mine and keeps plain sent status`() {
        val message = entity(senderId = "ali").toDomain(me, PeerCursors(readUpToSeq = 100), json)
        assertFalse(message.isMine)
        assertEquals(MessageStatus.SENT, message.status)
    }

    @Test
    fun `pending and failed map to sending and failed`() {
        assertEquals(MessageStatus.SENDING, entity(status = SendStatus.PENDING, serverSeq = null).toDomain(me, PeerCursors(), json).status)
        assertEquals(MessageStatus.FAILED, entity(status = SendStatus.FAILED, serverSeq = null).toDomain(me, PeerCursors(), json).status)
    }

    @Test
    fun `edited and deleted flags follow timestamps`() {
        val message = entity(editedAt = 5, deletedAt = 6).toDomain(me, PeerCursors(), json)
        assertTrue(message.isEdited)
        assertTrue(message.isDeleted)
    }

    @Test
    fun `system message exposes parsed event instead of raw text`() {
        val body = """{"event":"members_added","actorId":"ali","targetUserIds":["vali"],"extra":1}"""
        val message = entity(senderId = "ali", type = "SYSTEM", body = body).toDomain(me, PeerCursors(), json)
        assertEquals(MessageType.SYSTEM, message.type)
        assertNull(message.text)
        assertEquals("members_added", message.systemEvent?.event)
        assertEquals(listOf("vali"), message.systemEvent?.targetUserIds)
    }

    @Test
    fun `broken system body does not crash`() {
        val message = entity(type = "SYSTEM", body = "{not json").toDomain(me, PeerCursors(), json)
        assertNull(message.systemEvent)
    }

    @Test
    fun `unknown type from newer server is unknown not crash`() {
        assertEquals(MessageType.UNKNOWN, entity(type = "POLL").toDomain(me, PeerCursors(), json).type)
    }

    @Test
    fun `server media gets url and local copy when upload matches`() {
        val media = listOf(MediaItemEntity("m1", "IMAGE", "image/jpeg", 100, 10, 20, null))
        val message = entity(type = "IMAGE", media = media).toDomain(me, PeerCursors(), json, upload(mediaId = "m1", completed = true))
        val item = message.media.single()
        assertEquals("m1", item.mediaId)
        assertTrue(item.url!!.endsWith("v1/media/m1"))
        assertEquals("/local/photo.jpg", item.localPath)
        // Server qabul qilgan xabarda progress halqasi ko'rinmaydi.
        assertNull(message.upload)
    }

    @Test
    fun `not yet sent media is built from upload row with progress`() {
        val message = entity(type = "IMAGE", status = SendStatus.PENDING, serverSeq = null)
            .toDomain(me, PeerCursors(), json, upload(confirmed = 40))
        val item = message.media.single()
        assertNull(item.url)
        assertEquals("/local/photo.jpg", item.localPath)
        assertEquals(40L, message.upload?.sentBytes)
        assertEquals(100L, message.upload?.totalBytes)
    }

    @Test
    fun `completed upload shows full progress before server ack`() {
        val message = entity(type = "IMAGE", status = SendStatus.PENDING, serverSeq = null)
            .toDomain(me, PeerCursors(), json, upload(completed = true, confirmed = 50))
        assertEquals(100L, message.upload?.sentBytes)
    }

    @Test
    fun `unknown media kind falls back to file`() {
        assertEquals(MediaKind.IMAGE, "IMAGE".toMediaKind())
        assertEquals(MediaKind.VIDEO, "VIDEO".toMediaKind())
        assertEquals(MediaKind.FILE, "AUDIO".toMediaKind())
    }

    @Test
    fun `send request keeps client id as idempotency key`() {
        val request = entity().toSendRequest(mediaIds = listOf("m1"))
        assertEquals("c1", request.clientMessageId)
        assertEquals("TEXT", request.type)
        assertEquals("Salom", request.body)
        assertEquals(listOf("m1"), request.mediaIds)
    }
}
