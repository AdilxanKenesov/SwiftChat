package uz.relay.domain.usecase.media

import uz.relay.core.common.result.AppResult
import uz.relay.domain.model.Attachment
import uz.relay.domain.repository.MessageRepository
import javax.inject.Inject

class SendMediaMessageUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(
        chatId: String,
        attachment: Attachment,
        caption: String?,
        replyToClientMessageId: String?
    ): AppResult<Unit> = repository.sendMedia(chatId, attachment, caption?.trim()?.takeIf { it.isNotEmpty() }, replyToClientMessageId)
}
