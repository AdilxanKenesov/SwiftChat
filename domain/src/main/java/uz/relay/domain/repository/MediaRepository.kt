package uz.relay.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.relay.core.common.result.AppResult
import uz.relay.domain.model.DownloadState
import uz.relay.domain.model.MessageMedia

interface MediaRepository {

    /**
     * Faylni ilova keshiga yuklab oladi (hujjatni ochish uchun). Oldin yuklangan bo'lsa darhol [DownloadState.Done].
     * Xato bo'lsa Flow exception bilan tugaydi — chaqiruvchi `catch` qiladi.
     */
    fun download(media: MessageMedia, fileName: String): Flow<DownloadState>

    /** Rasm/videoni galereyaga (Pictures/Movies → SwiftChat) saqlash. */
    suspend fun saveToGallery(media: MessageMedia, fileName: String): AppResult<Unit>
}
