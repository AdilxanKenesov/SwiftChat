package uz.relay.domain.usecase.user

import kotlinx.coroutines.flow.Flow
import uz.relay.domain.model.User
import uz.relay.domain.repository.UserRepository
import javax.inject.Inject

/**
 * Keshdagi tanish foydalanuvchilarni kuzatadi — guruhga a'zo tanlash uchun.
 *
 * Faqat repository'ga uzatadi — ViewModel repository'ni emas, aniq nomli amalni bilishi uchun (single responsibility, test'da oson almashtiriladi).
 */
class ObserveKnownUsersUseCase @Inject constructor(
    private val repository: UserRepository
) {
    operator fun invoke(): Flow<List<User>> = repository.observeKnownUsers()
}
