package uz.relay.domain.usecase.media

import uz.relay.domain.repository.MessageRepository
import javax.inject.Inject

class CancelUploadUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(clientMessageId: String) = repository.cancelUpload(clientMessageId)
}
