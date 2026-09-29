package uz.relay.domain.usecase.media

import kotlinx.coroutines.flow.Flow
import uz.relay.domain.model.DownloadState
import uz.relay.domain.model.MessageMedia
import uz.relay.domain.repository.MediaRepository
import javax.inject.Inject

class DownloadMediaUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    operator fun invoke(media: MessageMedia, fileName: String): Flow<DownloadState> = repository.download(media, fileName)
}
