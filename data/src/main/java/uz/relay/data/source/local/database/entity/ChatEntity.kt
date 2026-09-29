package uz.relay.data.source.local.database.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Serverdagi `Chat` obyektining lokal nusxasi. UI chatlar ro'yxatini faqat shu jadvaldan o'qiydi.
 *
 * `type` lar String sifatida saqlanadi (enum emas): server yangi qiymat qo'shsa, bazaga yozishda ilova
 * yiqilmasin. Enum'ga aylantirish domain mapper'da bo'ladi.
 */
@Entity(
    tableName = "chats",
    // Ro'yxat doim shu ustun bo'yicha tartiblanadi — indeks so'rovni tezlashtiradi.
    indices = [Index("lastActivityAt")]
)
data class ChatEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String?,
    val avatarMediaId: String?,
    /** Faqat DIRECT chat uchun — ism `users` jadvalidan JOIN orqali olinadi. */
    val peerUserId: String?,
    @Embedded(prefix = "last_") val lastMessage: LastMessageEmbedded?,
    val lastActivityAt: Long,
    val unreadCount: Int,
    /** Mening o'qish kursorim. */
    val readUpToSeq: Long,
    /** Chatdagi eng katta serverSeq — yangi xabar shundan katta bo'lsagina hisobga olinadi. */
    val topSeq: Long,
    val muted: Boolean,
    val mutedUntil: Long?
)

/** Ro'yxatda ko'rinadigan "oxirgi xabar" — to'liq xabar emas, faqat preview uchun keraklisi. */
data class LastMessageEmbedded(
    val serverId: Long,
    val senderId: String,
    val type: String,
    val body: String?,
    val serverSeq: Long,
    val createdAt: Long,
    val deletedAt: Long?
)
