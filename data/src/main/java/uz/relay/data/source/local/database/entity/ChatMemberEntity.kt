package uz.relay.data.source.local.database.entity

import androidx.room.Entity

/**
 * Guruh a'zosi va uning roli.
 *
 * Nega lokal jadval: API'da a'zolarni o'qiydigan GET yo'q. Ro'yxat quyidagilardan yig'iladi:
 *  - ADMIN/OWNER uchun: `POST /members` (bo'sh ro'yxat bilan) qaytargan to'liq snapshot;
 *  - `member` update'lari (qo'shilish, chiqarilish, rol o'zgarishi);
 *  - SYSTEM xabarlar (group_created, members_added, member_removed, member_left).
 * Oddiy a'zo uchun ilova o'rnatilishidan oldingi o'zgarishlar faqat yuklangan tarixdan bilinadi.
 */
@Entity(tableName = "chat_members", primaryKeys = ["chatId", "userId"])
data class ChatMemberEntity(
    val chatId: String,
    val userId: String,
    /** OWNER | ADMIN | MEMBER */
    val role: String,
    val joinedAt: Long
)
