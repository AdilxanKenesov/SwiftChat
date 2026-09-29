package uz.relay.domain.usecase.message

import uz.relay.domain.model.Message
import uz.relay.domain.repository.MessageRepository
import javax.inject.Inject

/**
 * Chat ichida lokal qidiruv (faqat qurilmadagi xabarlar).
 *
 * Kichik mantiq: so'rov trim qilinadi; bo'sh so'rovda bazaga murojaat qilinmaydi — darhol bo'sh ro'yxat.
 */
class SearchMessagesUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(chatId: String, query: String): List<Message> {
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) emptyList() else repository.search(chatId, trimmed)
    }
}
