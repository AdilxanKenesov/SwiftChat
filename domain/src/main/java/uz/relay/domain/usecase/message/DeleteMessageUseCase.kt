package uz.relay.domain.usecase.message

import uz.relay.core.common.result.AppResult
import uz.relay.domain.repository.MessageRepository
import javax.inject.Inject

class DeleteMessageUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(serverId: Long): AppResult<Unit> = repository.delete(serverId)
}
