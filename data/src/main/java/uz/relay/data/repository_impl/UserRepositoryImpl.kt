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

/**
 * [UserRepository] implementatsiyasi: o'z profilim, boshqa foydalanuvchilar, qidiruv va ismlar xaritasi.
 *
 * Har bir server javobi Room'dagi [UserDao] keshiga yoziladi, UI esa faqat keshni kuzatadi — shu bois
 * profil, chat sarlavhasi va a'zolar ro'yxati bitta manbadan bir vaqtda yangilanadi.
 * Profil, qidiruv, kontakt tanlash va suhbat ekranlari ishlatadi.
 */
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

    /** Joriy foydalanuvchi profilini keshdan kuzatadi; hisob almashsa avtomatik yangisiga o'tadi. */
    override fun observeMe(): Flow<User?> = sessionStorage.session
        .map { it?.userId }
        .distinctUntilChanged()
        .flatMapLatest { me ->
            if (me == null) flowOf(null) else userDao.observe(me).map { it?.toDomain() }
        }

    /** O'z profilimni serverdan qayta yuklab keshga yozadi. */
    override suspend fun refreshMe(): AppResult<User> =
        safeApiCall { userApi.getMe() }
            .onSuccess { userDao.upsert(it.toEntity()) }
            .map { it.toUser() }

    /** Foydalanuvchi profilini keshdan kuzatadi (online/lastSeen presence update'lari bilan yangilanadi). */
    override fun observeUser(userId: String): Flow<User?> = userDao.observe(userId).map { it?.toDomain() }

    /** Kesh bo'lsa ham qayta yuklanadi: profil ochilganda ism/username va online holat eng yangisi bo'lsin. */
    override suspend fun refreshUser(userId: String): AppResult<User> =
        safeApiCall { userApi.getUser(userId) }
            .map { response ->
                val entity = response.toEntity()
                userDao.upsert(entity)
                entity.toDomain()
            }

    /** Username/ism bo'yicha server qidiruvi; o'zim natijadan chiqarib tashlanaman. */
    override suspend fun search(query: String): AppResult<List<User>> =
        safeApiCall { userApi.search(query) }
            .map { response ->
                val entities = response.users.map { it.toEntity() }
                // Topilganlar keshga yoziladi: chat ochilganda ism darhol ko'rinsin, tarmoqqa qayta chiqilmasin.
                userDao.upsertAll(entities)
                val me = sessionStorage.current()?.userId
                entities.filter { it.id != me }.map { it.toDomain() }
            }

    /** Keshdagi barcha foydalanuvchilar (o'zimdan tashqari) — guruhga a'zo tanlash ro'yxati uchun. */
    override fun observeKnownUsers(): Flow<List<User>> = sessionStorage.session
        .map { it?.userId }
        .distinctUntilChanged()
        .flatMapLatest { me ->
            if (me == null) flowOf(emptyList()) else userDao.observeAllExcept(me).map { users -> users.map { it.toDomain() } }
        }

    /** userId → ism xaritasi: SYSTEM xabarlar matni va guruhdagi yuboruvchi ismini chizish uchun. */
    override fun observeUserNames(): Flow<Map<String, String>> = userDao.observeNames()
        .map { names -> names.associate { it.id to it.displayName } }
        .distinctUntilChanged()
}
