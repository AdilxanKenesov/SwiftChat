package uz.relay.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.relay.core.common.result.AppResult
import uz.relay.domain.model.DownloadState
import uz.relay.domain.model.MessageMedia

/**
 * Media fayllarni yuklab olish va galereyaga saqlash (yuborish esa [MessageRepository.sendMedia] da).
 *
 * Nega interface: domain toza Kotlin moduli (Android'ga bog'liq emas) va faqat shartnomani belgilaydi,
 * amalga oshirish esa `data` modulida (Retrofit + Room). Shunda feature modullar data'ni bilmaydi,
 * use case'larni fake repository bilan oson test qilish mumkin (clean architecture, dependency inversion).
 */
interface MediaRepository {

    /**
     * Faylni ilova keshiga yuklab oladi (hujjatni ochish uchun). Oldin yuklangan bo'lsa darhol [DownloadState.Done].
     * Xato bo'lsa Flow exception bilan tugaydi — chaqiruvchi `catch` qiladi.
     */
    fun download(media: MessageMedia, fileName: String): Flow<DownloadState>

    /** Rasm/videoni galereyaga (Pictures/Movies → SwiftChat) saqlash. */
    suspend fun saveToGallery(media: MessageMedia, fileName: String): AppResult<Unit>
}
