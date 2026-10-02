package uz.relay.data.model.response

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** `GET /v1/updates/state` — serverdagi joriy updateSeq (bootstrap'dan keyingi boshlang'ich kursor). */
@Serializable
data class UpdatesStateResponse(
    val updateSeq: Long
)

/**
 * Update oqimining bitta qatori (REST `GET /v1/updates` da ham, WebSocket `update` frame'ida ham bir xil).
 *
 * Nega `payload` JsonObject: uning shakli `kind` ga bog'liq (7 xil). Avval `kind` o'qiladi, keyin payload
 * mos DTO'ga aylantiriladi. Noma'lum `kind` kelsa (server kelajakda qo'shsa) faqat o'sha qator o'tkazib
 * yuboriladi — butun sahifaning parse'i yiqilmaydi.
 */
@Serializable
data class UpdateResponse(
    /** Foydalanuvchining shaxsiy hodisalar kursori: qat'iy o'sadi, teshiksiz. */
    val updateSeq: Long,
    val kind: String,
    val payload: JsonObject
)

@Serializable
data class UpdatesPageResponse(
    val updates: List<UpdateResponse>,
    val state: UpdatesStateResponse,
    /**
     * `true` — kursor juda eski (7 kunlik saqlash muddatidan tashqarida yoki 10 000 hodisadan ko'p orqada).
     * Bunda `updates` bo'sh keladi va sahifalab yetib bo'lmaydi: to'liq resync kerak.
     */
    val tooLong: Boolean = false
)

/** Update `kind` qiymatlari. String konstantalar, chunki noma'lum kind'lar ham parse'dan o'tishi kerak. */
object UpdateKinds {
    const val MESSAGE_NEW = "message_new"
    const val MESSAGE_EDIT = "message_edit"
    const val MESSAGE_DELETE = "message_delete"
    const val READ = "read"
    const val DELIVERED = "delivered"
    const val MEMBER = "member"
    const val CHAT = "chat"
}

// ---- `kind` ga qarab payload shakllari (message_new → MessageResponse) ----

/** `message_edit`: xabar matni o'zgardi; [editVersion] eski update yangisini bosib ketmasligi uchun. */
@Serializable
data class MessageEditPayload(
    val serverId: Long,
    val chatId: String,
    val body: String,
    val editVersion: Int,
    val editedAt: Long
)

/** `message_delete`: xabar o'chirildi — lokalda tombstone ([deletedAt]) qo'yiladi, qator o'chirilmaydi. */
@Serializable
data class MessageDeletePayload(
    val serverId: Long,
    val chatId: String,
    val deletedAt: Long
)

/** `read` va `delivered` uchun: `userId` ning kursori `upToSeq` gacha ko'tarildi. */
@Serializable
data class CursorPayload(
    val chatId: String,
    val userId: String,
    val upToSeq: Long
)

/** `member`: guruh a'zoligi o'zgardi ([removed] = true bo'lsa a'zo chiqarilgan). */
@Serializable
data class MemberPayload(
    val chatId: String,
    /** Qo'shilgan / chiqarilgan / roli o'zgargan a'zo. */
    val userId: String,
    val role: String? = null,
    val removed: Boolean,
    /** Kim bajardi. */
    val actorId: String
)

/** Chat yaratildi yoki guruh nomi/avatari o'zgardi. To'liq qator faqat `GET /v1/chats/{id}` da. */
@Serializable
data class ChatPayload(
    val chatId: String,
    val type: String,
    val title: String? = null,
    val avatarMediaId: String? = null
)
