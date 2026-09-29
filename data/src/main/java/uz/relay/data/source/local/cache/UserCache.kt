package uz.relay.data.source.local.cache

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import uz.relay.core.common.result.AppResult
import uz.relay.data.mapper.toEntity
import uz.relay.data.source.local.database.dao.UserDao
import uz.relay.data.source.local.database.entity.UserEntity
import uz.relay.data.source.network.api.UserApi
import uz.relay.data.utils.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Boshqa foydalanuvchilar profillari keshini to'ldiradi.
 *
 * - Faqat keshda YO'Qlari so'raladi: butun sinf bitta IP'dan 300 so'rov/daqiqa limitini bo'lishadi.
 * - Parallel, lekin bir vaqtda ko'pi bilan 4 ta — limitni bir zumda yeb qo'ymaslik uchun.
 * - Bitta profil yuklanmasa xato otilmaydi: ism keyingi sync'da to'ldiriladi, ro'yxat esa baribir chiziladi.
 */
@Singleton
class UserCache @Inject constructor(
    private val userDao: UserDao,
    private val userApi: UserApi
) {
    /**
     * Yuklaydi, lekin bazaga YOZMAYDI — chaqiruvchi ularni o'z tranzaksiyasida yozadi
     * (tarmoq kutish tranzaksiyadan tashqarida bo'lishi kerak, aks holda baza uzoq qulflanadi).
     */
    suspend fun fetchMissing(ids: Collection<String>): List<UserEntity> {
        val unique = ids.distinct()
        if (unique.isEmpty()) return emptyList()

        val missing = unique - userDao.existingIds(unique).toSet()
        val semaphore = Semaphore(permits = MAX_PARALLEL_REQUESTS)
        return coroutineScope {
            missing.map { id ->
                async { semaphore.withPermit { safeApiCall { userApi.getUser(id) } } }
            }.awaitAll()
        }.mapNotNull { (it as? AppResult.Success)?.data?.toEntity() }
    }

    private companion object {
        const val MAX_PARALLEL_REQUESTS = 4
    }
}
