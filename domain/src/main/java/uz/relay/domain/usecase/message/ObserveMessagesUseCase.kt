package uz.relay.domain.usecase.message

import kotlinx.coroutines.flow.Flow
import uz.relay.domain.model.Message
import uz.relay.domain.repository.MessageRepository
import javax.inject.Inject

class ObserveMessagesUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    operator fun invoke(chatId: String): Flow<List<Message>> = repository.observeMessages(chatId)
}
