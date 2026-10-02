package uz.relay.data.source.local.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Xabar — serverdan kelgani ham, hali yuborilmagani (outbox) ham shu jadvalda.
 *
 * Nega PRIMARY KEY `clientMessageId` (serverId emas): u xabar yaratilgan zahoti (serverga yetmasdan oldin)
 * ma'lum va hech qachon o'zgarmaydi. Server javobi, tarix sahifasi va WebSocket echo'si — hammasi shu kalit
 * bo'yicha upsert qilinadi, shuning uchun dublikat bo'lmaydi. `serverId`/`serverSeq` esa faqat server
 * qabul qilgandan keyin paydo bo'ladi (shuning uchun nullable).
 */
@Entity(
    tableName = "messages",
    indices = [
        Index("chatId", "serverSeq"),
        Index("serverId"),
        Index("status")
    ]
)
data class MessageEntity(
    @PrimaryKey val clientMessageId: String,
    val chatId: String,
    val senderId: String,
    val serverId: Long?,
    val serverSeq: Long?,
    val type: String,
    val body: String?,
    val replyToClientMessageId: String?,
    /** Server vaqti; hali yuborilmagan xabar uchun — lokal yaratilgan vaqt. */
    val createdAt: Long,
    val editedAt: Long?,
    val editVersion: Int,
    val deletedAt: Long?,
    val status: SendStatus,
    /** FAILED bo'lsa — server qaytargan xato kodi. */
    val sendError: String?,
    /** Serverdagi media meta'lari (JSON ustun, [MediaConverters]). O'zim yuborayotganda — bo'sh. */
    val media: List<MediaItemEntity> = emptyList()
)

/** Room enum'ni nomi bo'yicha TEXT sifatida saqlaydi (SQL so'rovlarda 'PENDING' kabi yoziladi). */
enum class SendStatus {
    /** Outbox'da, serverga yuborilishini kutmoqda. */
    PENDING,

    /** Server qabul qilgan (serverSeq bor). */
    SENT,

    /** Server qayta urinib bo'lmaydigan xato bilan rad etgan (retryable = false). */
    FAILED
}
