package uz.relay.domain.usecase.group

import uz.relay.core.common.result.AppResult
import uz.relay.domain.repository.GroupRepository
import javax.inject.Inject

class RenameGroupUseCase @Inject constructor(
    private val repository: GroupRepository
) {
    suspend operator fun invoke(chatId: String, title: String): AppResult<Unit> =
        repository.rename(chatId, title.trim())
}
