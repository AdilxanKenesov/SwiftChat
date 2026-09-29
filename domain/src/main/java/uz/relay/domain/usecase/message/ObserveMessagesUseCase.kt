package uz.relay.domain.usecase.message

import kotlinx.coroutines.flow.Flow
import uz.relay.domain.model.Message
import uz.relay.domain.repository.MessageRepository
import javax.inject.Inject

/**
 * Chat xabarlarini lokal bazadan kuzatadi (single source of truth).
 *
 * Faqat repository'ga uzatadi — ViewModel repository'ni emas, aniq nomli amalni bilishi uchun (single responsibility, test'da oson almashtiriladi).
 */
class ObserveMessagesUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    operator fun invoke(chatId: String): Flow<List<Message>> = repository.observeMessages(chatId)
}
