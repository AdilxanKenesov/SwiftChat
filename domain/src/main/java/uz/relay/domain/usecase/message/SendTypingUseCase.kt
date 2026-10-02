package uz.relay.domain.usecase.message

import uz.relay.domain.repository.MessageRepository
import javax.inject.Inject

/**
 * "Yozmoqda…" signalini yuboradi (faqat socket orqali, saqlanmaydi).
 *
 * Faqat repository'ga uzatadi — ViewModel repository'ni emas, aniq nomli amalni bilishi uchun (single responsibility, test'da oson almashtiriladi).
 */
class SendTypingUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    operator fun invoke(chatId: String) = repository.sendTyping(chatId)
}
