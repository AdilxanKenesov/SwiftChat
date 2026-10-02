package uz.relay.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.relay.core.common.result.AppResult
import uz.relay.domain.model.User

/**
 * Foydalanuvchilar: mening profilim, boshqalar profili, qidiruv va ismlar keshi.
 *
 * Nega interface: domain toza Kotlin moduli (Android'ga bog'liq emas) va faqat shartnomani belgilaydi,
 * amalga oshirish esa `data` modulida (Retrofit + Room). Shunda feature modullar data'ni bilmaydi,
 * use case'larni fake repository bilan oson test qilish mumkin (clean architecture, dependency inversion).
 */
interface UserRepository {

    /** Ism va username'ni serverda yangilaydi; javob keshga ham yoziladi. */
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

    /** Username prefiksi bo'yicha qidiruv (serverda). Topilganlar keshga ham yoziladi. */
    suspend fun search(query: String): AppResult<List<User>>

    /** Boshqa foydalanuvchining profili (lokal keshdan). Online holati socket'dagi `presence` bilan yangilanadi. */
    fun observeUser(userId: String): Flow<User?>

    /** `GET /v1/users/{id}` — profilni (ism, username, online holati) yangilab, keshga yozadi. */
    suspend fun refreshUser(userId: String): AppResult<User>

    /** Keshdagi tanish foydalanuvchilar (o'zimdan tashqari) — guruhga a'zo tanlash uchun. */
    fun observeKnownUsers(): Flow<List<User>>
}
