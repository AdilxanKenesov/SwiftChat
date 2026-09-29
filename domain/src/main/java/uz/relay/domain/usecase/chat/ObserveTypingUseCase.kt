package uz.relay.domain.usecase.chat

import kotlinx.coroutines.flow.Flow
import uz.relay.domain.repository.TypingRepository
import javax.inject.Inject

/**
 * Kim qaysi chatda "yozmoqda…" ekanini kuzatadi (faqat xotirada, saqlanmaydi).
 *
 * Faqat repository'ga uzatadi — ViewModel repository'ni emas, aniq nomli amalni bilishi uchun (single responsibility, test'da oson almashtiriladi).
 */
class ObserveTypingUseCase @Inject constructor(
    private val repository: TypingRepository
) {
    /** `chatId → hozir yozayotgan userId'lar`. */
    operator fun invoke(): Flow<Map<String, Set<String>>> = repository.typing
}
