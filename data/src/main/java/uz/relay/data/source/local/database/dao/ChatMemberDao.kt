package uz.relay.data.source.local.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import uz.relay.data.source.local.database.entity.ChatMemberEntity

/** A'zo + profil ma'lumotlari (ism, online, oxirgi marta) — bitta so'rovda. */
data class MemberItem(
    @Embedded val member: ChatMemberEntity,
    // LEFT JOIN: profil hali keshda bo'lmasa NULL keladi.
    val displayName: String?,
    val online: Boolean?,
    val lastSeenAt: Long?
)

@Dao
interface ChatMemberDao {

    /** Tartib: egasi, adminlar, keyin a'zolar; har guruh ichida qo'shilgan vaqti bo'yicha. */
    @Query(
        """
        SELECT m.*, u.displayName AS displayName, u.online AS online, u.lastSeenAt AS lastSeenAt
        FROM chat_members m
        LEFT JOIN users u ON u.id = m.userId
        WHERE m.chatId = :chatId
        ORDER BY CASE m.role WHEN 'OWNER' THEN 0 WHEN 'ADMIN' THEN 1 ELSE 2 END, m.joinedAt
        """
    )
    fun observe(chatId: String): Flow<List<MemberItem>>

    @Query("SELECT * FROM chat_members WHERE chatId = :chatId")
    suspend fun getAll(chatId: String): List<ChatMemberEntity>

    @Query("SELECT * FROM chat_members WHERE chatId = :chatId AND userId = :userId")
    suspend fun get(chatId: String, userId: String): ChatMemberEntity?

    @Query("SELECT COUNT(*) FROM chat_members WHERE chatId = :chatId")
    suspend fun count(chatId: String): Int

    @Upsert
    suspend fun upsert(member: ChatMemberEntity)

    @Upsert
    suspend fun upsertAll(members: List<ChatMemberEntity>)

    /** SYSTEM xabardan: rol noma'lum bo'lgani uchun mavjud qatorning (aniqroq) rolini bosib ketmaymiz. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(members: List<ChatMemberEntity>)

    @Query("DELETE FROM chat_members WHERE chatId = :chatId AND userId = :userId")
    suspend fun delete(chatId: String, userId: String)

    @Query("DELETE FROM chat_members WHERE chatId = :chatId")
    suspend fun deleteByChat(chatId: String)

    /** Serverdan kelgan to'liq ro'yxat bilan almashtirish — ro'yxatda yo'qlar (chiqib ketganlar) o'chadi. */
    @Transaction
    suspend fun replace(chatId: String, members: List<ChatMemberEntity>) {
        deleteByChat(chatId)
        upsertAll(members)
    }
}
