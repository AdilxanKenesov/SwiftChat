package uz.relay.domain.usecase.auth

import uz.relay.core.common.result.AppResult
import uz.relay.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * OTP kodni tekshiradi; muvaffaqiyatda sessiya saqlanadi.
 *
 * Faqat repository'ga uzatadi — ViewModel repository'ni emas, aniq nomli amalni bilishi uchun (single responsibility, test'da oson almashtiriladi).
 */
class VerifyOtpUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    /** Qiymat — `isNewUser`: `true` bo'lsa, foydalanuvchi yangi va profilni to'ldirishi kerak. */
    suspend operator fun invoke(phone: String, code: String): AppResult<Boolean> =
        repository.verifyOtp(phone, code)
}
