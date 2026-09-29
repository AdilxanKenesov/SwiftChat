package uz.relay.domain.usecase.message

import uz.relay.core.common.result.AppResult
import uz.relay.domain.repository.MessageRepository
import javax.inject.Inject

class MarkChatReadUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(chatId: String): AppResult<Unit> = repository.markRead(chatId)
}
