package uz.relay.domain.usecase.chat

import uz.relay.core.common.result.AppResult
import uz.relay.domain.repository.ChatRepository
import javax.inject.Inject

class SetChatMutedUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(chatId: String, muted: Boolean): AppResult<Unit> = repository.setMuted(chatId, muted)
}
