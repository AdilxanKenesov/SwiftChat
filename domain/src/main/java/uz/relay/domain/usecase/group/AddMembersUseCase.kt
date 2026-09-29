package uz.relay.domain.usecase.group

import uz.relay.core.common.result.AppResult
import uz.relay.domain.repository.GroupRepository
import javax.inject.Inject

class AddMembersUseCase @Inject constructor(
    private val repository: GroupRepository
) {
    suspend operator fun invoke(chatId: String, userIds: List<String>): AppResult<Unit> =
        repository.addMembers(chatId, userIds)
}
