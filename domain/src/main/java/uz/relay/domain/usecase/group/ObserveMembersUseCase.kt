package uz.relay.domain.usecase.group

import kotlinx.coroutines.flow.Flow
import uz.relay.domain.model.ChatMember
import uz.relay.domain.repository.GroupRepository
import javax.inject.Inject

class ObserveMembersUseCase @Inject constructor(
    private val repository: GroupRepository
) {
    operator fun invoke(chatId: String): Flow<List<ChatMember>> = repository.observeMembers(chatId)
}
