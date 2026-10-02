package uz.relay.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.relay.core.common.result.AppResult
import uz.relay.domain.model.AuthState

/**
 * Avtorizatsiya: OTP so'rash/tekshirish, sessiya va chiqish.
 *
 * Nega interface: domain toza Kotlin moduli (Android'ga bog'liq emas) va faqat shartnomani belgilaydi,
 * amalga oshirish esa `data` modulida (Retrofit + Room). Shunda feature modullar data'ni bilmaydi,
 * use case'larni fake repository bilan oson test qilish mumkin (clean architecture, dependency inversion).
 */
interface AuthRepository {

    /** Joriy avtorizatsiya holati; sessiya o'zgarganda (masalan, token bekor bo'lganda) yangi qiymat keladi. */
    val authState: Flow<AuthState>

    suspend fun requestOtp(phone: String): AppResult<Unit>

    /** Muvaffaqiyatda sessiyani saqlaydi; qiymat — `isNewUser`. */
    suspend fun verifyOtp(phone: String, code: String): AppResult<Boolean>

    /** Yangi foydalanuvchi profilni to'ldirdi: endi ilovaning asosiy qismi ochiladi. */
    suspend fun completeProfileSetup()

    /**
     * Hisobdan chiqish: serverda refresh token bekor qilinadi, qurilmadagi sessiya va hisobning lokal
     * ma'lumoti (chatlar, xabarlar) o'chiriladi. Tarmoq bo'lmasa ham lokal chiqish baribir bajariladi.
     */
    suspend fun logout()
}
