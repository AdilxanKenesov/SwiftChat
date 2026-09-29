package uz.relay.data.source.local.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import uz.relay.data.source.local.database.entity.SyncStateEntity

/**
 * Bitta qatorli `sync_state` jadvali uchun DAO — update oqimining `updateSeq` kursori.
 * SyncEngine kursorni o'qiydi va update qo'llangan tranzaksiya ichida yangilaydi.
 */
@Dao
interface SyncStateDao {

    /** `null` — hali bootstrap qilinmagan (yangi o'rnatish yoki boshqa hisobga kirilgandan keyin). */
    @Query("SELECT updateSeq FROM sync_state WHERE id = ${SyncStateEntity.SINGLE_ROW_ID}")
    suspend fun getCursor(): Long?

    @Query("SELECT updateSeq FROM sync_state WHERE id = ${SyncStateEntity.SINGLE_ROW_ID}")
    /** Kursor Flow'i — UI bootstrap tugaganini (null'dan qiymatga o'tish) kuzatadi. */
    fun observeCursor(): Flow<Long?>

    @Upsert
    suspend fun setState(state: SyncStateEntity)

    suspend fun setCursor(updateSeq: Long) = setState(SyncStateEntity(updateSeq = updateSeq))
}
