package uz.relay.domain.usecase.message

import uz.relay.core.common.result.AppResult
import uz.relay.domain.repository.MessageRepository
import javax.inject.Inject

/**
 * Xabarni o'chiradi (serverda tombstone bo'lib qoladi).
 *
 * Faqat repository'ga uzatadi — ViewModel repository'ni emas, aniq nomli amalni bilishi uchun (single responsibility, test'da oson almashtiriladi).
 */
class DeleteMessageUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(serverId: Long): AppResult<Unit> = repository.delete(serverId)
}
