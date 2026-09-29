package uz.relay.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.relay.core.common.result.AppResult
import uz.relay.domain.model.AuthState

interface AuthRepository {

    val authState: Flow<AuthState>

    suspend fun requestOtp(phone: String): AppResult<Unit>

    /** Persists the session on success; the value is `isNewUser`. */
    suspend fun verifyOtp(phone: String, code: String): AppResult<Boolean>

    /** The new user filled in the profile: the main part of the app is open now. */
    suspend fun completeProfileSetup()

    /**
     * Hisobdan chiqish: serverda refresh token bekor qilinadi, qurilmadagi sessiya va hisobning lokal
     * ma'lumoti (chatlar, xabarlar) o'chiriladi. Tarmoq bo'lmasa ham lokal chiqish baribir bajariladi.
     */
    suspend fun logout()
}
