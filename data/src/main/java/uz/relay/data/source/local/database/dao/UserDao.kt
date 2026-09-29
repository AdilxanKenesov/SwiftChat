package uz.relay.data.source.local.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import uz.relay.data.source.local.database.entity.UserEntity

/** `userId → displayName` juftligi (ismlar xaritasi uchun). */
data class UserName(
    val id: String,
    val displayName: String
)

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE id = :id")
    fun observe(id: String): Flow<UserEntity?>

    @Query("SELECT id, displayName FROM users")
    fun observeNames(): Flow<List<UserName>>

    /** Berilganlardan qaysilari keshda bor — faqat yo'qlarini tarmoqdan so'rash uchun. */
    @Query("SELECT id FROM users WHERE id IN (:ids)")
    suspend fun existingIds(ids: List<String>): List<String>

    @Upsert
    suspend fun upsert(user: UserEntity)

    @Upsert
    suspend fun upsertAll(users: List<UserEntity>)
}
