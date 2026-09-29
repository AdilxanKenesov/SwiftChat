package uz.relay.data.repository_impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uz.relay.core.common.result.AppError
import uz.relay.core.common.result.AppResult
import uz.relay.core.common.result.ErrorCodes
import uz.relay.data.mapper.toDomain
import uz.relay.data.source.local.cache.UserCache
import uz.relay.data.source.local.database.dao.ContactDao
import uz.relay.data.source.local.database.dao.UserDao
import uz.relay.data.source.local.database.entity.ContactEntity
import uz.relay.domain.model.User
import uz.relay.domain.repository.ContactRepository
import javax.inject.Inject

/** Lokal kontaktlar (Room). Nega lokal — [ContactRepository] izohiga qarang. */
internal class ContactRepositoryImpl @Inject constructor(
    private val contactDao: ContactDao,
    private val userDao: UserDao,
    private val userCache: UserCache
) : ContactRepository {

    override fun observeContacts(): Flow<List<User>> =
        contactDao.observeContacts().map { users -> users.map { it.toDomain() } }

    override fun observeContactIds(): Flow<Set<String>> = contactDao.observeIds().map { it.toSet() }

    /**
     * Kontakt ro'yxati `users` bilan JOIN qilinadi — profil keshda bo'lmasa kontakt ro'yxatda ko'rinmasdi.
     * Shuning uchun avval profil (yo'q bo'lsa) serverdan yuklanadi; topilmasa — xato.
     */
    override suspend fun add(userId: String): AppResult<Unit> {
        userDao.upsertAll(userCache.fetchMissing(listOf(userId)))
        if (userDao.existingIds(listOf(userId)).isEmpty()) {
            return AppResult.Error(AppError.Api(404, ErrorCodes.NOT_FOUND, "User profile could not be loaded", retryable = true))
        }
        contactDao.insert(ContactEntity(userId = userId, addedAt = System.currentTimeMillis()))
        return AppResult.Success(Unit)
    }

    override suspend fun remove(userId: String) = contactDao.delete(userId)
}
