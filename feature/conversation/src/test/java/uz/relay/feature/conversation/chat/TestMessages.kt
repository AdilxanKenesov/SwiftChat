package uz.relay.feature.conversation.chat

import uz.relay.domain.model.Message
import uz.relay.domain.model.MessageStatus
import uz.relay.domain.model.MessageType

/** Testlar uchun xabar: faqat test uchun muhim maydonlar beriladi, qolganlari oddiy qiymatda. */
internal fun message(
    id: String,
    senderId: String = "ali",
    isMine: Boolean = false,
    createdAt: Long = 0,
    type: MessageType = MessageType.TEXT,
    text: String? = "Salom",
    serverId: Long? = 1,
    isDeleted: Boolean = false,
    replyTo: String? = null
) = Message(
    clientMessageId = id,
    serverId = serverId,
    chatId = "chat",
    senderId = senderId,
    isMine = isMine,
    serverSeq = serverId,
    type = type,
    text = text,
    systemEvent = null,
    replyToClientMessageId = replyTo,
    createdAt = createdAt,
    isEdited = false,
    isDeleted = isDeleted,
    status = MessageStatus.SENT
)
