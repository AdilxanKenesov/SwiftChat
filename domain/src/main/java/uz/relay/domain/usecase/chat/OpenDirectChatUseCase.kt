package uz.relay.domain.usecase.chat

import uz.relay.core.common.result.AppResult
import uz.relay.domain.repository.ChatRepository
import javax.inject.Inject

class OpenDirectChatUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    /** Qiymat — chat id'si. */
    suspend operator fun invoke(peerUserId: String): AppResult<String> = repository.openDirect(peerUserId)
}
