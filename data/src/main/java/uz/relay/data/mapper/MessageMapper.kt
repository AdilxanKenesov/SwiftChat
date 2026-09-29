package uz.relay.data.mapper

import kotlinx.serialization.json.Json
import uz.relay.data.model.request.SendMessageRequest
import uz.relay.data.model.response.MessageResponse
import uz.relay.data.source.local.database.entity.MessageEntity
import uz.relay.data.source.local.database.entity.SendStatus
import uz.relay.domain.model.Message
import uz.relay.domain.model.MessageStatus
import uz.relay.domain.model.MessageType

/** Mendan boshqa a'zolarning eng katta kursorlari (guruhda "kamida bittasi"). */
data class PeerCursors(val readUpToSeq: Long = 0, val deliveredUpToSeq: Long = 0)

/**
 * Chiquvchi xabar holati (spec 3.8): ack yo'q → SENDING; ack bor → SENT; suhbatdosh yetkazilish kursori
 * ≥ serverSeq → DELIVERED; o'qish kursori ≥ serverSeq → READ. SENDING/FAILED kursorlarga bog'liq emas.
 *
 * Nega saqlanmaydi, hisoblanadi: kursor bir marta ko'tarilsa, undan oldingi BARCHA xabarlar birdaniga ✓✓
 * bo'ladi. Har xabar qatorini yangilash o'rniga holatni kursordan "tortib olamiz" — bitta raqam, arzon va izchil.
 */
fun outgoingStatus(sendStatus: MessageStatus, serverSeq: Long?, peers: PeerCursors): MessageStatus {
    if (sendStatus != MessageStatus.SENT || serverSeq == null) return sendStatus
    return when {
        serverSeq <= peers.readUpToSeq -> MessageStatus.READ
        serverSeq <= peers.deliveredUpToSeq -> MessageStatus.DELIVERED
        else -> MessageStatus.SENT
    }
}

/** Serverdan kelgan xabar — demak u allaqachon qabul qilingan (SENT). */
fun MessageResponse.toEntity() = MessageEntity(
    clientMessageId = clientMessageId,
    chatId = chatId,
    senderId = senderId,
    serverId = serverId,
    serverSeq = serverSeq,
    type = type,
    body = body,
    replyToClientMessageId = replyToClientMessageId,
    createdAt = createdAt,
    editedAt = editedAt,
    editVersion = editVersion,
    deletedAt = deletedAt,
    status = SendStatus.SENT,
    sendError = null
)

fun MessageEntity.toDomain(myUserId: String?, peers: PeerCursors, json: Json): Message {
    val messageType = type.toMessageType()
    val isSystem = messageType == MessageType.SYSTEM
    val isMine = senderId == myUserId
    val sendStatus = when (status) {
        SendStatus.PENDING -> MessageStatus.SENDING
        SendStatus.SENT -> MessageStatus.SENT
        SendStatus.FAILED -> MessageStatus.FAILED
    }
    return Message(
        clientMessageId = clientMessageId,
        serverId = serverId,
        chatId = chatId,
        senderId = senderId,
        isMine = isMine,
        serverSeq = serverSeq,
        type = messageType,
        text = if (isSystem) null else body,
        systemEvent = if (isSystem) parseSystemEvent(body, json) else null,
        replyToClientMessageId = replyToClientMessageId,
        createdAt = createdAt,
        isEdited = editedAt != null,
        isDeleted = deletedAt != null,
        status = if (isMine) outgoingStatus(sendStatus, serverSeq, peers) else sendStatus
    )
}

fun MessageEntity.toSendRequest() = SendMessageRequest(
    clientMessageId = clientMessageId,
    type = type,
    body = body,
    replyTo = replyToClientMessageId
)
