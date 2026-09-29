package uz.relay.domain.usecase.chat

import kotlinx.coroutines.flow.Flow
import uz.relay.domain.repository.TypingRepository
import javax.inject.Inject

class ObserveTypingUseCase @Inject constructor(
    private val repository: TypingRepository
) {
    /** `chatId → hozir yozayotgan userId'lar`. */
    operator fun invoke(): Flow<Map<String, Set<String>>> = repository.typing
}
