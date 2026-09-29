package uz.relay.data.mapper

import kotlinx.serialization.json.Json
import uz.relay.data.BuildConfig
import uz.relay.data.model.request.SendMessageRequest
import uz.relay.data.model.response.MediaMetaResponse
import uz.relay.data.model.response.MessageResponse
import uz.relay.data.source.local.database.entity.MediaItemEntity
import uz.relay.data.source.local.database.entity.MessageEntity
import uz.relay.data.source.local.database.entity.SendStatus
import uz.relay.data.source.local.database.entity.UploadEntity
import uz.relay.domain.model.MediaKind
import uz.relay.domain.model.Message
import uz.relay.domain.model.MessageMedia
import uz.relay.domain.model.MessageStatus
import uz.relay.domain.model.MessageType
import uz.relay.domain.model.UploadProgress

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
    sendError = null,
    media = media.map { it.toEntity() }
)

fun MediaMetaResponse.toEntity() = MediaItemEntity(
    mediaId = mediaId,
    kind = kind,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    width = width,
    height = height,
    durationMs = durationMs
)

/** Media yuklab olish manzili. Token bilan ochiladi (Coil/ExoPlayer media klienti orqali). */
fun mediaUrl(mediaId: String): String = "${BuildConfig.BASE_URL}v1/media/$mediaId"

/**
 * @param upload o'zim yuborgan fayl (bo'lsa): lokal nusxa va yuklash progressi shundan olinadi.
 */
fun MessageEntity.toDomain(myUserId: String?, peers: PeerCursors, json: Json, upload: UploadEntity? = null): Message {
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
        status = if (isMine) outgoingStatus(sendStatus, serverSeq, peers) else sendStatus,
        media = mediaFor(upload),
        // Progress faqat server hali qabul qilmagan xabar uchun: yuborilgandan keyin halqa ko'rinmaydi.
        upload = upload?.takeIf { status != SendStatus.SENT }?.let {
            UploadProgress(sentBytes = if (it.completed) it.sizeBytes else it.confirmedBytes, totalBytes = it.sizeBytes)
        }
    )
}

/**
 * Serverdagi meta bor bo'lsa — o'sha (lokal nusxa mediaId mos kelsa qo'shiladi). Hali serverga yetmagan
 * xabarda meta yo'q — yuklash qatoridan quriladi, UI rasmni lokal fayldan darhol ko'rsatadi.
 */
private fun MessageEntity.mediaFor(upload: UploadEntity?): List<MessageMedia> {
    if (media.isEmpty()) return listOfNotNull(upload?.toDomainMedia())
    return media.map { item ->
        val local = upload?.takeIf { it.mediaId == item.mediaId || media.size == 1 }
        MessageMedia(
            mediaId = item.mediaId,
            kind = item.kind.toMediaKind(),
            mimeType = item.mimeType,
            sizeBytes = item.sizeBytes,
            width = item.width,
            height = item.height,
            durationMs = item.durationMs,
            url = mediaUrl(item.mediaId),
            localPath = local?.localPath,
            posterPath = local?.posterPath
        )
    }
}

private fun UploadEntity.toDomainMedia() = MessageMedia(
    mediaId = mediaId,
    kind = kind.toMediaKind(),
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    width = width,
    height = height,
    durationMs = durationMs,
    url = null,
    localPath = localPath,
    posterPath = posterPath
)

fun String.toMediaKind(): MediaKind = when (this) {
    "IMAGE" -> MediaKind.IMAGE
    "VIDEO" -> MediaKind.VIDEO
    else -> MediaKind.FILE
}

fun MessageEntity.toSendRequest(mediaIds: List<String>? = null) = SendMessageRequest(
    clientMessageId = clientMessageId,
    type = type,
    body = body,
    mediaIds = mediaIds,
    replyTo = replyToClientMessageId
)
