package uz.relay.domain.usecase.chat

import uz.relay.core.common.result.AppResult
import uz.relay.domain.repository.ChatRepository
import javax.inject.Inject

class RefreshChatsUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(): AppResult<Unit> = repository.refresh()
}
