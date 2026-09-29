package uz.relay.domain.usecase.chat

import uz.relay.core.common.result.AppResult
import uz.relay.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Server bilan sinxronlash (bootstrap yoki catch-up). Natija [ObserveChatsUseCase] orqali keladi.
 *
 * Faqat repository'ga uzatadi — ViewModel repository'ni emas, aniq nomli amalni bilishi uchun (single responsibility, test'da oson almashtiriladi).
 */
class RefreshChatsUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(): AppResult<Unit> = repository.refresh()
}
