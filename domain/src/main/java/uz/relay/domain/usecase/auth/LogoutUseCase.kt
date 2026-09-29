package uz.relay.domain.usecase.auth

import uz.relay.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Hisobdan chiqish: sessiya va hisobning lokal ma'lumoti o'chiriladi (tafsiloti [AuthRepository.logout] da).
 *
 * Faqat repository'ga uzatadi — ViewModel repository'ni emas, aniq nomli amalni bilishi uchun (single responsibility, test'da oson almashtiriladi).
 */
class LogoutUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke() = repository.logout()
}
