package uz.relay.domain.usecase.message

import uz.relay.domain.repository.MessageRepository
import javax.inject.Inject

class SendTypingUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    operator fun invoke(chatId: String) = repository.sendTyping(chatId)
}
