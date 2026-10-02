package uz.relay.domain.usecase.media

import uz.relay.domain.repository.MessageRepository
import javax.inject.Inject

/**
 * Yuklanayotgan media xabarni bekor qiladi: xabar va uning lokal fayli o'chiriladi.
 *
 * Faqat repository'ga uzatadi — ViewModel repository'ni emas, aniq nomli amalni bilishi uchun (single responsibility, test'da oson almashtiriladi).
 */
class CancelUploadUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(clientMessageId: String) = repository.cancelUpload(clientMessageId)
}
