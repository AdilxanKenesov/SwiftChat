package uz.relay.domain.usecase.chat

import kotlinx.coroutines.flow.Flow
import uz.relay.domain.model.ChatSummary
import uz.relay.domain.repository.ChatRepository
import javax.inject.Inject

class ObserveChatUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(chatId: String): Flow<ChatSummary?> = repository.observeChat(chatId)
}
