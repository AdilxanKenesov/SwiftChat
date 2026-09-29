package uz.relay.data.mapper

import kotlinx.serialization.json.Json
import uz.relay.data.model.response.ChatResponse
import uz.relay.data.model.response.MessagePreviewResponse
import uz.relay.data.model.response.MessageResponse
import uz.relay.data.model.response.SystemMessageBodyResponse
import uz.relay.data.source.local.database.dao.ChatListItem
import uz.relay.data.source.local.database.entity.ChatEntity
import uz.relay.data.source.local.database.entity.LastMessageEmbedded
import uz.relay.domain.model.ChatSummary
import uz.relay.domain.model.ChatType
import uz.relay.domain.model.LastMessage
import uz.relay.domain.model.MessageStatus
import uz.relay.domain.model.MessageType
import uz.relay.domain.model.SystemEvent

fun ChatResponse.toEntity() = ChatEntity(
    id = id,
    type = type,
    title = title,
    avatarMediaId = avatarMediaId,
    peerUserId = peerUserId,
    lastMessage = lastMessage?.toEmbedded(),
    lastActivityAt = lastActivityAt,
    unreadCount = unreadCount,
    readUpToSeq = readUpToSeq,
    topSeq = topSeq,
    muted = muted,
    mutedUntil = mutedUntil
)

fun MessagePreviewResponse.toEmbedded() = LastMessageEmbedded(
    serverId = serverId,
    senderId = senderId,
    type = type,
    body = body,
    serverSeq = serverSeq,
    createdAt = createdAt,
    deletedAt = deletedAt
)

fun MessageResponse.toEmbedded() = LastMessageEmbedded(
    serverId = serverId,
    senderId = senderId,
    type = type,
    body = body,
    serverSeq = serverSeq,
    createdAt = createdAt,
    deletedAt = deletedAt
)

/**
 * Ro'yxat qatori → domain modeli.
 * @param myUserId "meniki"mi degan savol uchun ("Siz: ..." va ✓ belgilari).
 */
fun ChatListItem.toDomain(myUserId: String, json: Json): ChatSummary {
    val type = chat.type.toChatType()
    return ChatSummary(
        id = chat.id,
        type = type,
        title = if (type == ChatType.DIRECT) peerDisplayName else chat.title,
        peerUserId = chat.peerUserId,
        peerOnline = peerOnline == true,
        peerLastSeenAt = peerLastSeenAt,
        lastMessage = chat.lastMessage?.let { last ->
            val messageType = last.type.toMessageType()
            val isSystem = messageType == MessageType.SYSTEM
            LastMessage(
                serverId = last.serverId,
                senderId = last.senderId,
                isMine = last.senderId == myUserId,
                type = messageType,
                text = if (isSystem) null else last.body,
                systemEvent = if (isSystem) parseSystemEvent(last.body, json) else null,
                isDeleted = last.deletedAt != null,
                createdAt = last.createdAt,
                status = outgoingStatus(
                    sendStatus = MessageStatus.SENT,
                    serverSeq = last.serverSeq,
                    peers = PeerCursors(peerReadUpToSeq ?: 0, peerDeliveredUpToSeq ?: 0)
                )
            )
        },
        lastActivityAt = chat.lastActivityAt,
        unreadCount = chat.unreadCount,
        muted = chat.muted
    )
}

/** SYSTEM xabar `body`si JSON satr. Buzuq bo'lsa `null` — ro'yxat baribir chiziladi. */
fun parseSystemEvent(body: String?, json: Json): SystemEvent? {
    if (body.isNullOrBlank()) return null
    return runCatching { json.decodeFromString(SystemMessageBodyResponse.serializer(), body) }
        .getOrNull()
        ?.let { SystemEvent(event = it.event, actorId = it.actorId, targetUserIds = it.targetUserIds, title = it.title) }
}

/** SYSTEM xabar matnini tuzish uchun kimlarning ismi kerak (keshga oldindan yuklab qo'yiladi). */
fun systemEventUserIds(body: String?, json: Json): List<String> =
    parseSystemEvent(body, json)?.let { listOf(it.actorId) + it.targetUserIds }.orEmpty()

internal fun String.toChatType(): ChatType = when (this) {
    "DIRECT" -> ChatType.DIRECT
    "GROUP" -> ChatType.GROUP
    else -> ChatType.UNKNOWN
}

internal fun String.toMessageType(): MessageType = when (this) {
    "TEXT" -> MessageType.TEXT
    "IMAGE" -> MessageType.IMAGE
    "VIDEO" -> MessageType.VIDEO
    "FILE" -> MessageType.FILE
    "SYSTEM" -> MessageType.SYSTEM
    else -> MessageType.UNKNOWN
}
