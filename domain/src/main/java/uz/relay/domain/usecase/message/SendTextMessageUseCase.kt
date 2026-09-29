package uz.relay.domain.usecase.message

import uz.relay.domain.repository.MessageRepository
import javax.inject.Inject

class SendTextMessageUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(chatId: String, text: String, replyToClientMessageId: String?) {
        val body = text.trim()
        // Bo'sh xabar yuborilmaydi (server ham TEXT uchun matn talab qiladi).
        if (body.isNotEmpty()) repository.sendText(chatId, body, replyToClientMessageId)
    }
}
