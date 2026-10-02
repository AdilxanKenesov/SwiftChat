package uz.relay.domain.usecase.user

import kotlinx.coroutines.flow.Flow
import uz.relay.domain.model.User
import uz.relay.domain.repository.UserRepository
import javax.inject.Inject

/**
 * Boshqa foydalanuvchi profilini lokal keshdan kuzatadi (online holati bilan).
 *
 * Faqat repository'ga uzatadi — ViewModel repository'ni emas, aniq nomli amalni bilishi uchun (single responsibility, test'da oson almashtiriladi).
 */
class ObserveUserUseCase @Inject constructor(
    private val repository: UserRepository
) {
    operator fun invoke(userId: String): Flow<User?> = repository.observeUser(userId)
}
