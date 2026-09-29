package uz.relay.data.source.local.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import uz.relay.data.source.local.database.entity.MemberCursorEntity

@Dao
interface MemberCursorDao {

    /** Chat a'zolarining kursorlari — xabarlar ✓✓ holatini hisoblash uchun. */
    @Query("SELECT * FROM member_cursors WHERE chatId = :chatId")
    fun observe(chatId: String): Flow<List<MemberCursorEntity>>

    @Query("SELECT * FROM member_cursors WHERE chatId = :chatId AND userId = :userId")
    suspend fun get(chatId: String, userId: String): MemberCursorEntity?

    @Upsert
    suspend fun upsert(cursor: MemberCursorEntity)

    /**
     * Kursorni ko'taradi (max-wins): kelgan qiymat kichik bo'lsa hech narsa o'zgarmaydi. Update'lar
     * tartibsiz yoki ikki marta kelishi mumkin — kursor baribir orqaga ketmasligi kerak.
     *
     * O'qish yetkazilishni ham bildiradi: o'qigan odamning qurilmasi xabarni albatta olgan.
     *
     * Nega SQL'dagi `ON CONFLICT DO UPDATE` emas: u SQLite 3.24+ talab qiladi, Android 8–9 (minSdk 26)
     * esa eskiroq SQLite bilan keladi. O'qish + yozish @Transaction ichida — natija bir xil va hamma
     * qurilmada ishlaydi.
     */
    @Transaction
    suspend fun raise(chatId: String, userId: String, readUpToSeq: Long = 0, deliveredUpToSeq: Long = 0) {
        val current = get(chatId, userId)
        val read = maxOf(current?.readUpToSeq ?: 0, readUpToSeq)
        val delivered = maxOf(current?.deliveredUpToSeq ?: 0, deliveredUpToSeq, read)
        upsert(MemberCursorEntity(chatId, userId, readUpToSeq = read, deliveredUpToSeq = delivered))
    }

    @Query("DELETE FROM member_cursors WHERE chatId = :chatId")
    suspend fun deleteByChat(chatId: String)

    @Query("DELETE FROM member_cursors")
    suspend fun deleteAll()
}
