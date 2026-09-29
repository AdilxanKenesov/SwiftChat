package uz.relay.data.source.local.database.entity

import androidx.room.Entity

/**
 * Chat a'zosining o'qish va yetkazilish kursorlari (`read` / `delivered` update'laridan).
 * O'zimning oxirgi xabarim uchun ✓✓ belgisi shu jadvaldan hisoblanadi.
 *
 * Nega hozirdan saqlaymiz: update oqimi bir marta o'tadi — kursor oshib ketgach, o'sha hodisalar
 * qayta kelmaydi. Hozir tashlab yuborsak, keyin ularni tiklab bo'lmaydi.
 */
@Entity(tableName = "member_cursors", primaryKeys = ["chatId", "userId"])
data class MemberCursorEntity(
    val chatId: String,
    val userId: String,
    val readUpToSeq: Long,
    val deliveredUpToSeq: Long
)
