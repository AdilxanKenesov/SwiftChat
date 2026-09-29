package uz.relay.data.repository_impl

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import uz.relay.core.common.result.AppResult
import uz.relay.core.common.result.map
import uz.relay.core.common.result.onSuccess
import uz.relay.data.mapper.toDomain
import uz.relay.data.mapper.toEntity
import uz.relay.data.mapper.toUser
import uz.relay.data.model.request.UpdateMeRequest
import uz.relay.data.source.local.SessionStorage
import uz.relay.data.source.local.database.dao.UserDao
import uz.relay.data.source.network.api.UserApi
import uz.relay.data.utils.safeApiCall
import uz.relay.domain.model.User
import uz.relay.domain.repository.UserRepository
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest
internal class UserRepositoryImpl @Inject constructor(
    private val userApi: UserApi,
    private val userDao: UserDao,
    private val sessionStorage: SessionStorage
) : UserRepository {

    /** Server javobi keshga ham yoziladi — profil ekrani va chatlar sarlavhasi darhol yangilansin. */
    override suspend fun updateProfile(displayName: String, username: String): AppResult<User> =
        safeApiCall { userApi.updateMe(UpdateMeRequest(displayName = displayName, username = username)) }
            .onSuccess { userDao.upsert(it.toEntity()) }
            .map { it.toUser() }

    override fun observeMe(): Flow<User?> = sessionStorage.session
        .map { it?.userId }
        .distinctUntilChanged()
        .flatMapLatest { me ->
            if (me == null) flowOf(null) else userDao.observe(me).map { it?.toDomain() }
        }

    override suspend fun refreshMe(): AppResult<User> =
        safeApiCall { userApi.getMe() }
            .onSuccess { userDao.upsert(it.toEntity()) }
            .map { it.toUser() }

    override fun observeUserNames(): Flow<Map<String, String>> = userDao.observeNames()
        .map { names -> names.associate { it.id to it.displayName } }
        .distinctUntilChanged()
}
