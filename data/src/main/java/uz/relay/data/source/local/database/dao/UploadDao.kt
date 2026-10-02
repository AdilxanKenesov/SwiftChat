package uz.relay.data.source.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import uz.relay.data.source.local.database.entity.UploadEntity

/**
 * `uploads` jadvali uchun DAO — resumable (bo'lakli) media yuklash holati.
 * MediaUploader sessiya/progress'ni yozadi, suhbat ekrani progress va lokal nusxani kuzatadi.
 */
@Dao
interface UploadDao {

    @Query("SELECT * FROM uploads WHERE clientMessageId = :clientMessageId")
    suspend fun get(clientMessageId: String): UploadEntity?

    /** Suhbat ekrani xabarlar bilan birga kuzatadi: progress va lokal nusxa shu yerdan. */
    @Query("SELECT * FROM uploads WHERE chatId = :chatId")
    fun observeByChat(chatId: String): Flow<List<UploadEntity>>

    /** REPLACE: bir xabar uchun qayta tayyorlangan fayl eski yozuvni to'liq almashtiradi. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(upload: UploadEntity)

    /** Yangi sessiya: server bergan id'lar, offset 0 dan. */
    @Query(
        """
        UPDATE uploads SET uploadId = :uploadId, mediaId = :mediaId, chunkSize = :chunkSize,
               confirmedBytes = 0, completed = 0
        WHERE clientMessageId = :clientMessageId
        """
    )
    suspend fun startSession(clientMessageId: String, uploadId: String, mediaId: String, chunkSize: Int)

    /** Har bir bo'lak tasdiqlangandan keyin — UI'dagi progress uchun. */
    @Query("UPDATE uploads SET confirmedBytes = :confirmedBytes WHERE clientMessageId = :clientMessageId")
    suspend fun updateProgress(clientMessageId: String, confirmedBytes: Long)

    @Query("UPDATE uploads SET completed = 1, confirmedBytes = sizeBytes WHERE clientMessageId = :clientMessageId")
    suspend fun markCompleted(clientMessageId: String)

    /** Sessiya yaroqsiz (muddati o'tgan, sha256 mos kelmadi): keyingi urinish noldan boshlaydi. */
    @Query("UPDATE uploads SET uploadId = NULL, mediaId = NULL, confirmedBytes = 0, completed = 0 WHERE clientMessageId = :clientMessageId")
    suspend fun resetSession(clientMessageId: String)

    @Query("DELETE FROM uploads WHERE clientMessageId = :clientMessageId")
    suspend fun delete(clientMessageId: String)
}
