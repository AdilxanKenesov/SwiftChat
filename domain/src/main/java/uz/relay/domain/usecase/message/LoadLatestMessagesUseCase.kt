package uz.relay.domain.usecase.message

import uz.relay.core.common.result.AppResult
import uz.relay.domain.repository.MessageRepository
import javax.inject.Inject

class LoadLatestMessagesUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    /** Qiymat — eskiroq xabarlar ham bormi. */
    suspend operator fun invoke(chatId: String): AppResult<Boolean> = repository.loadLatest(chatId)
}
