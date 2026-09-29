package uz.relay.data.source.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import uz.relay.data.source.local.database.entity.MessageEntity

@Dao
interface MessageDao {

    /**
     * Eng yangisi birinchi (UI `reverseLayout` bilan pastdan yuqoriga chizadi).
     * Yuborilmagan xabarlar (serverSeq = NULL) eng yangi hisoblanadi — ular hali server tartibiga kirmagan,
     * shuning uchun ekranda eng pastda turadi.
     */
    @Query(
        """
        SELECT * FROM messages
        WHERE chatId = :chatId
        ORDER BY (serverSeq IS NULL) DESC, serverSeq DESC, createdAt DESC
        """
    )
    fun observeMessages(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE clientMessageId = :clientMessageId")
    suspend fun get(clientMessageId: String): MessageEntity?

    @Upsert
    suspend fun upsertAll(messages: List<MessageEntity>)

    @Insert
    suspend fun insert(message: MessageEntity)

    @Query("SELECT MIN(serverSeq) FROM messages WHERE chatId = :chatId")
    suspend fun minSeq(chatId: String): Long?

    @Query("SELECT MAX(serverSeq) FROM messages WHERE chatId = :chatId")
    suspend fun maxSeq(chatId: String): Long?

    /** Max-wins: eski (yoki takroriy) tahrir yangisini bosib ketmaydi — `editVersion` faqat oshadi. */
    @Query(
        """
        UPDATE messages SET body = :body, editVersion = :editVersion, editedAt = :editedAt
        WHERE serverId = :serverId AND editVersion < :editVersion
        """
    )
    suspend fun applyEdit(serverId: Long, body: String, editVersion: Int, editedAt: Long)

    /** Tombstone: qator o'chmaydi (serverSeq "teshigi" bo'lmasin), faqat `deletedAt` qo'yiladi. */
    @Query("UPDATE messages SET deletedAt = :deletedAt WHERE serverId = :serverId AND deletedAt IS NULL")
    suspend fun applyDelete(serverId: Long, deletedAt: Long)

    /** Server tasdiqlagan xabarlarni o'chiradi; outbox (PENDING/FAILED) tegilmaydi. */
    @Query("DELETE FROM messages WHERE chatId = :chatId AND status = 'SENT'")
    suspend fun deleteSynced(chatId: String)

    /** To'liq resync'da: lokal nusxa eskirgan bo'lishi mumkin, chat ochilganda serverdan qayta yuklanadi. */
    @Query("DELETE FROM messages WHERE status = 'SENT'")
    suspend fun deleteAllSynced()

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteByChat(chatId: String)

    // ---------------- Outbox ----------------

    /** Eng eski kutayotgan xabar: xabarlar foydalanuvchi yozgan tartibda yuborilishi kerak. */
    @Query("SELECT * FROM messages WHERE status = 'PENDING' ORDER BY createdAt ASC LIMIT 1")
    suspend fun nextPending(): MessageEntity?

    @Query(
        """
        UPDATE messages
        SET serverId = :serverId, serverSeq = :serverSeq, createdAt = :serverCreatedAt,
            status = 'SENT', sendError = NULL
        WHERE clientMessageId = :clientMessageId
        """
    )
    suspend fun markSent(clientMessageId: String, serverId: Long, serverSeq: Long, serverCreatedAt: Long)

    @Query("UPDATE messages SET status = 'FAILED', sendError = :error WHERE clientMessageId = :clientMessageId")
    suspend fun markFailed(clientMessageId: String, error: String)

    @Query(
        """
        UPDATE messages SET status = 'PENDING', sendError = NULL
        WHERE clientMessageId = :clientMessageId AND status = 'FAILED'
        """
    )
    suspend fun markPendingAgain(clientMessageId: String)
}
