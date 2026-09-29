package uz.relay.domain.usecase.group

import uz.relay.core.common.result.AppResult
import uz.relay.domain.repository.GroupRepository
import javax.inject.Inject

class LeaveGroupUseCase @Inject constructor(
    private val repository: GroupRepository
) {
    suspend operator fun invoke(chatId: String): AppResult<Unit> = repository.leave(chatId)
}
