package uz.relay.data.source.local.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import uz.relay.data.source.local.database.entity.SyncStateEntity

@Dao
interface SyncStateDao {

    /** `null` — hali bootstrap qilinmagan (yangi o'rnatish yoki boshqa hisobga kirilgandan keyin). */
    @Query("SELECT updateSeq FROM sync_state WHERE id = ${SyncStateEntity.SINGLE_ROW_ID}")
    suspend fun getCursor(): Long?

    @Query("SELECT updateSeq FROM sync_state WHERE id = ${SyncStateEntity.SINGLE_ROW_ID}")
    fun observeCursor(): Flow<Long?>

    @Upsert
    suspend fun setState(state: SyncStateEntity)

    suspend fun setCursor(updateSeq: Long) = setState(SyncStateEntity(updateSeq = updateSeq))
}
