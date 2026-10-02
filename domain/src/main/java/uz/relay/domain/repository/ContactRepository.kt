package uz.relay.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.relay.core.common.result.AppResult
import uz.relay.domain.model.User

/**
 * Kontaktlar — Telegram'dagi "Kontaktlar" ro'yxatining o'rni.
 *
 * Relay API'da kontaktlar endpoint'i yo'q (faqat username bo'yicha qidiruv), shuning uchun ro'yxat faqat SHU
 * QURILMADA saqlanadi: boshqa qurilmaga sinxron qilinmaydi va logout'da tozalanadi (foydalanuvchi qarori).
 * Interfeys domain'da — keyin server kontaktlarni qo'llasa, faqat data qatlami o'zgaradi.
 */
interface ContactRepository {

    /** Kontaktlar (ism bo'yicha), profil ma'lumotlari keshdan — online holat ham o'zi yangilanadi. */
    fun observeContacts(): Flow<List<User>>

    /** Kontakt id'lari — qidiruv natijasida "qo'shilgan" belgisini ko'rsatish uchun. */
    fun observeContactIds(): Flow<Set<String>>

    /** Profil keshda bo'lmasa avval serverdan yuklanadi (ism ko'rinishi uchun), keyin kontakt qo'shiladi. */
    suspend fun add(userId: String): AppResult<Unit>

    suspend fun remove(userId: String)
}
