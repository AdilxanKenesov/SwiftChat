package uz.relay.data.source.local.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import uz.relay.data.source.local.database.entity.ChatEntity

/** Chat qatori + (DIRECT bo'lsa) suhbatdosh ismi/holati + a'zolar kursorlari — bitta so'rovda. */
data class ChatListItem(
    @Embedded val chat: ChatEntity,
    // LEFT JOIN: profil hali keshda bo'lmasa, bu ustunlar NULL keladi.
    val peerDisplayName: String?,
    val peerOnline: Boolean?,
    /** Mendan boshqa a'zolarning eng katta o'qish/yetkazilish kursori — oxirgi xabarim uchun ✓✓. */
    val peerReadUpToSeq: Long?,
    val peerDeliveredUpToSeq: Long?
)

@Dao
interface ChatDao {

    /**
     * Flow: jadvallar o'zgarishi bilan (sync, keyinroq WebSocket) ro'yxat o'zi yangilanadi —
     * ViewModel qayta so'rov yuborishi shart emas.
     *
     * Kursorlar: o'zimdan boshqa a'zolarning MAX qiymati. Guruhda kamida bittasi o'qigan bo'lsa ✓✓
     * (Telegram'dagidek). Room bu Flow'ni `member_cursors` va `users` o'zgarganda ham qayta ishga tushiradi.
     */
    @Query(
        """
        SELECT c.*, u.displayName AS peerDisplayName, u.online AS peerOnline,
               mc.maxRead AS peerReadUpToSeq, mc.maxDelivered AS peerDeliveredUpToSeq
        FROM chats c
        LEFT JOIN users u ON u.id = c.peerUserId
        LEFT JOIN (
            SELECT chatId, MAX(readUpToSeq) AS maxRead, MAX(deliveredUpToSeq) AS maxDelivered
            FROM member_cursors WHERE userId != :myUserId GROUP BY chatId
        ) mc ON mc.chatId = c.id
        ORDER BY c.lastActivityAt DESC
        """
    )
    fun observeChatList(myUserId: String): Flow<List<ChatListItem>>

    @Query("SELECT * FROM chats WHERE id = :chatId")
    suspend fun getChat(chatId: String): ChatEntity?

    /**
     * Mening o'qish kursorim (boshqa qurilmamda o'qigan bo'lsam ham keladi). Serverdagi kabi max-wins:
     * kursor hech qachon orqaga ketmaydi. O'qilmaganlar soni faqat chat oxirigacha o'qilganda nolga
     * tushadi — aniq sonni server keyingi to'liq sync'da beradi.
     */
    @Query(
        """
        UPDATE chats
        SET unreadCount = CASE WHEN :upToSeq >= topSeq THEN 0 ELSE unreadCount END,
            readUpToSeq = MAX(readUpToSeq, :upToSeq)
        WHERE id = :chatId
        """
    )
    suspend fun markRead(chatId: String, upToSeq: Long)

    @Upsert
    suspend fun upsert(chat: ChatEntity)

    @Upsert
    suspend fun upsertAll(chats: List<ChatEntity>)

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun delete(chatId: String)

    @Query("DELETE FROM chats")
    suspend fun deleteAll()

    /**
     * To'liq snapshot bilan almashtirish (full resync).
     * Nega avval o'chiramiz: snapshot'da yo'q chat (chiqib ketilgan guruh) lokal bazada "osilib" qolmasin.
     * @Transaction — UI oraliqdagi bo'sh ro'yxatni ko'rmaydi.
     */
    @Transaction
    suspend fun replaceAll(chats: List<ChatEntity>) {
        deleteAll()
        upsertAll(chats)
    }
}
