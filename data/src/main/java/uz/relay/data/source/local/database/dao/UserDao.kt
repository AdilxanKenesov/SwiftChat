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

/**
 * `users` (profillar keshi) jadvali uchun DAO. Ism va online holat chat ro'yxati, a'zolar ro'yxati
 * va suhbat sarlavhasida JOIN orqali ishlatiladi; [UserCache], UserRepositoryImpl va UpdateApplier yozadi.
 */
@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE id = :id")
    fun observe(id: String): Flow<UserEntity?>

    /** Keshdagi hamma foydalanuvchilar (o'zimdan tashqari) — guruh yaratishda "tanishlar" ro'yxati. */
    @Query("SELECT * FROM users WHERE id != :exceptUserId ORDER BY displayName COLLATE NOCASE")
    fun observeAllExcept(exceptUserId: String): Flow<List<UserEntity>>

    /** Hamma ismlar — UI'da id o'rniga ism ko'rsatish uchun (masalan, chat ichidagi qidiruvda). */
    @Query("SELECT id, displayName FROM users")
    fun observeNames(): Flow<List<UserName>>

    /** Berilganlardan qaysilari keshda bor — faqat yo'qlarini tarmoqdan so'rash uchun. */
    @Query("SELECT id FROM users WHERE id IN (:ids)")
    suspend fun existingIds(ids: List<String>): List<String>

    /**
     * WebSocket `presence` frame'idan. Keshda yo'q foydalanuvchi uchun hech narsa qilmaydi — uning profili
     * kerak bo'lganda yuklanadi va o'shanda joriy holat ham keladi.
     */
    @Query("UPDATE users SET online = :online, lastSeenAt = :lastSeenAt WHERE id = :userId")
    suspend fun updatePresence(userId: String, online: Boolean, lastSeenAt: Long?)

    @Upsert
    suspend fun upsert(user: UserEntity)

    @Upsert
    suspend fun upsertAll(users: List<UserEntity>)
}
