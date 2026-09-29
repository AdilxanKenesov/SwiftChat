package uz.relay.domain.usecase.message

import uz.relay.domain.repository.MessageRepository
import javax.inject.Inject

class RetryMessageUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(clientMessageId: String) = repository.retry(clientMessageId)
}
