package uz.relay.domain.usecase.group

import uz.relay.core.common.result.AppResult
import uz.relay.domain.model.MemberRole
import uz.relay.domain.repository.GroupRepository
import javax.inject.Inject

class ChangeMemberRoleUseCase @Inject constructor(
    private val repository: GroupRepository
) {
    suspend operator fun invoke(chatId: String, userId: String, role: MemberRole): AppResult<Unit> =
        repository.changeRole(chatId, userId, role)
}
