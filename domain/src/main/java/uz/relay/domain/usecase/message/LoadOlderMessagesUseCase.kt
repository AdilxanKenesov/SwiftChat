package uz.relay.domain.usecase.message

import uz.relay.core.common.result.AppResult
import uz.relay.domain.repository.MessageRepository
import javax.inject.Inject

/**
 * Eskiroq xabarlar sahifasini yuklaydi (ro'yxat yuqoriga aylantirilganda, pagination).
 *
 * Faqat repository'ga uzatadi — ViewModel repository'ni emas, aniq nomli amalni bilishi uchun (single responsibility, test'da oson almashtiriladi).
 */
class LoadOlderMessagesUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    /** Qiymat — bundan ham eskiroq xabarlar bormi. */
    suspend operator fun invoke(chatId: String): AppResult<Boolean> = repository.loadOlder(chatId)
}
