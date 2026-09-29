package uz.relay.domain.usecase.message

import uz.relay.core.common.result.AppResult
import uz.relay.domain.repository.MessageRepository
import javax.inject.Inject

class LoadOlderMessagesUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    /** Qiymat — bundan ham eskiroq xabarlar bormi. */
    suspend operator fun invoke(chatId: String): AppResult<Boolean> = repository.loadOlder(chatId)
}
