package uz.relay.data.repository_impl

import uz.relay.core.common.result.AppResult
import uz.relay.core.common.result.map
import uz.relay.data.mapper.toUser
import uz.relay.data.model.request.UpdateMeRequest
import uz.relay.data.source.network.api.UserApi
import uz.relay.data.utils.safeApiCall
import uz.relay.domain.model.User
import uz.relay.domain.repository.UserRepository
import javax.inject.Inject

internal class UserRepositoryImpl @Inject constructor(
    private val userApi: UserApi
) : UserRepository {

    override suspend fun updateProfile(displayName: String, username: String): AppResult<User> =
        safeApiCall { userApi.updateMe(UpdateMeRequest(displayName = displayName, username = username)) }
            .map { it.toUser() }
}
