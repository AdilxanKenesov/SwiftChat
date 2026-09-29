package uz.relay.domain.usecase.group

import uz.relay.core.common.result.AppResult
import uz.relay.domain.repository.GroupRepository
import javax.inject.Inject

class RemoveMemberUseCase @Inject constructor(
    private val repository: GroupRepository
) {
    suspend operator fun invoke(chatId: String, userId: String): AppResult<Unit> =
        repository.removeMember(chatId, userId)
}
