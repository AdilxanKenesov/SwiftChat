package uz.relay.domain.usecase.group

import uz.relay.core.common.result.AppResult
import uz.relay.domain.repository.GroupRepository
import javax.inject.Inject

class CreateGroupUseCase @Inject constructor(
    private val repository: GroupRepository
) {
    /** Qiymat — yangi chat id'si. */
    suspend operator fun invoke(title: String, memberIds: List<String>): AppResult<String> =
        repository.createGroup(title.trim(), memberIds)
}
