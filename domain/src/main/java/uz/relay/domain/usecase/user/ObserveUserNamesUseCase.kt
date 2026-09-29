package uz.relay.domain.usecase.user

import kotlinx.coroutines.flow.Flow
import uz.relay.domain.repository.UserRepository
import javax.inject.Inject

/**
 * `userId → displayName` xaritasini kuzatadi — guruhdagi "Ali: ..." prefiksi va SYSTEM xabar matnlari uchun.
 *
 * Faqat repository'ga uzatadi — ViewModel repository'ni emas, aniq nomli amalni bilishi uchun (single responsibility, test'da oson almashtiriladi).
 */
class ObserveUserNamesUseCase @Inject constructor(
    private val repository: UserRepository
) {
    operator fun invoke(): Flow<Map<String, String>> = repository.observeUserNames()
}
