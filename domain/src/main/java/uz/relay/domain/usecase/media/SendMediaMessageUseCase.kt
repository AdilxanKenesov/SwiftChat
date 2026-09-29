package uz.relay.domain.usecase.media

import uz.relay.core.common.result.AppResult
import uz.relay.domain.model.Attachment
import uz.relay.domain.repository.MessageRepository
import javax.inject.Inject

/**
 * Rasm/video/fayl xabar yuboradi (outbox orqali, bo'laklab yuklanadi).
 *
 * Kichik mantiq: izoh trim qilinadi, bo'sh bo'lsa `null` ga aylanadi — server bo'sh caption olmasligi uchun.
 */
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
