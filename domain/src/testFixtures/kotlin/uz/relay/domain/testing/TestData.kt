package uz.relay.domain.testing

import uz.relay.core.common.result.AppError
import uz.relay.domain.model.ChatMember
import uz.relay.domain.model.ChatSummary
import uz.relay.domain.model.ChatType
import uz.relay.domain.model.MemberRole
import uz.relay.domain.model.User

/** Testlar uchun tayyor obyektlar: faqat test uchun muhim maydonlar beriladi. */
object TestData {

    fun user(id: String = "me", name: String = "Ali Valiyev", username: String? = "ali", phone: String? = "+998000000001") =
        User(id = id, username = username, displayName = name, avatarMediaId = null, avatarVersion = 0, phone = phone)

    fun chat(id: String = "chat", type: ChatType = ChatType.DIRECT, title: String? = "Ali", peerUserId: String? = "ali") =
        ChatSummary(
            id = id,
            type = type,
            title = title,
            peerUserId = peerUserId,
            peerOnline = false,
            peerLastSeenAt = null,
            lastMessage = null,
            lastActivityAt = 0,
            unreadCount = 0,
            muted = false
        )

    fun member(userId: String, role: MemberRole = MemberRole.MEMBER, isMe: Boolean = false) =
        ChatMember(userId = userId, displayName = userId, role = role, online = false, lastSeenAt = null, isMe = isMe)

    fun apiError(code: String = "SOME_ERROR", status: Int = 400) =
        AppError.Api(httpStatus = status, code = code, message = code, retryable = false)
}
