package uz.relay.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.relay.core.common.result.AppResult
import uz.relay.domain.model.User

interface UserRepository {

    suspend fun updateProfile(displayName: String, username: String): AppResult<User>

    /** Mening profilim (lokal keshdan). Hali yuklanmagan bo'lsa `null`. */
    fun observeMe(): Flow<User?>

    /** `GET /v1/users/me` ni yuklab, keshga yozadi. */
    suspend fun refreshMe(): AppResult<User>

    /**
     * Keshdagi hamma foydalanuvchilarning ismlari (`userId → displayName`). Guruhdagi "Malika: ..."
     * prefiksi va SYSTEM xabar matnlarini tuzish uchun kerak.
     */
    fun observeUserNames(): Flow<Map<String, String>>
}
