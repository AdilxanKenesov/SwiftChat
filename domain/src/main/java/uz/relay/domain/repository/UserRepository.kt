package uz.relay.domain.repository

import uz.relay.core.common.result.AppResult
import uz.relay.domain.model.User

interface UserRepository {
    suspend fun updateProfile(displayName: String, username: String): AppResult<User>
}
