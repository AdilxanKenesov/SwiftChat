package uz.relay.domain.usecase.auth

import uz.relay.core.common.result.AppResult
import uz.relay.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Telefon raqamiga OTP kod so'raydi (kod Telegram bot orqali keladi).
 *
 * Faqat repository'ga uzatadi — ViewModel repository'ni emas, aniq nomli amalni bilishi uchun (single responsibility, test'da oson almashtiriladi).
 */
class RequestOtpUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(phone: String): AppResult<Unit> = repository.requestOtp(phone)
}
