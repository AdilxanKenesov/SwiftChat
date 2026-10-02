package uz.relay.domain.usecase.auth

import uz.relay.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Yangi foydalanuvchi profilini to'ldirib bo'lganini belgilaydi — shundan keyin ilovaning asosiy qismi ochiladi.
 *
 * Faqat repository'ga uzatadi — ViewModel repository'ni emas, aniq nomli amalni bilishi uchun (single responsibility, test'da oson almashtiriladi).
 */
class CompleteProfileSetupUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke() = repository.completeProfileSetup()
}
