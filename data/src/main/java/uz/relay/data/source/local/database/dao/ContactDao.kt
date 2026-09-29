package uz.relay.data.source.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import uz.relay.data.source.local.database.entity.ContactEntity
import uz.relay.data.source.local.database.entity.UserEntity

@Dao
interface ContactDao {

    /** Kontaktlar profillari bilan, ism bo'yicha (katta-kichik harf farqsiz). Profili keshda yo'qlari tushib qoladi. */
    @Query(
        """
        SELECT u.* FROM contacts c
        INNER JOIN users u ON u.id = c.userId
        ORDER BY u.displayName COLLATE NOCASE
        """
    )
    fun observeContacts(): Flow<List<UserEntity>>

    @Query("SELECT userId FROM contacts")
    fun observeIds(): Flow<List<String>>

    /** IGNORE: qayta qo'shish birinchi qo'shilgan vaqtni o'zgartirmaydi. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(contact: ContactEntity)

    @Query("DELETE FROM contacts WHERE userId = :userId")
    suspend fun delete(userId: String)
}
