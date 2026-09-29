package uz.relay.domain.usecase.message

import uz.relay.domain.repository.MessageRepository
import javax.inject.Inject

/**
 * Xato bilan qolgan xabarni qaytadan outbox navbatiga qo'yadi.
 *
 * Faqat repository'ga uzatadi — ViewModel repository'ni emas, aniq nomli amalni bilishi uchun (single responsibility, test'da oson almashtiriladi).
 */
class RetryMessageUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(clientMessageId: String) = repository.retry(clientMessageId)
}
