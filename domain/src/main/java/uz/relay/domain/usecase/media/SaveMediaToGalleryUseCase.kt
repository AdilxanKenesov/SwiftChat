package uz.relay.domain.usecase.media

import uz.relay.core.common.result.AppResult
import uz.relay.domain.model.MessageMedia
import uz.relay.domain.repository.MediaRepository
import javax.inject.Inject

/**
 * Rasm/videoni telefon galereyasiga saqlaydi.
 *
 * Faqat repository'ga uzatadi — ViewModel repository'ni emas, aniq nomli amalni bilishi uchun (single responsibility, test'da oson almashtiriladi).
 */
class SaveMediaToGalleryUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(media: MessageMedia, fileName: String): AppResult<Unit> = repository.saveToGallery(media, fileName)
}
