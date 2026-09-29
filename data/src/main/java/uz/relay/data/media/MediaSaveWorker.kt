package uz.relay.data.media

import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import uz.relay.data.mapper.toMediaKind
import uz.relay.domain.model.MediaKind
import uz.relay.domain.model.MessageMedia
import java.io.IOException

/**
 * Rasm/videoni galereyaga saqlaydi — FOREGROUND rejimda.
 *
 * Nega WorkManager + foreground (oddiy coroutine emas):
 *  - katta videoni yuklab olish bir necha daqiqa davom etishi mumkin; foydalanuvchi ko'ruvchidan yoki ilovadan
 *    chiqib ketsa ham ish to'xtamasligi kerak (ViewModel scope'i ekran bilan birga o'ladi);
 *  - foreground service'ni tizim fonda "o'ldirmaydi", bildirishnoma esa jarayonni tepada ko'rsatib turadi
 *    (Android talabi ham shu: uzoq fon ishi foydalanuvchiga ko'rinishi shart);
 *  - internet uzilsa WorkManager keyinroq o'zi qayta urinadi (Result.retry).
 */
@HiltWorker
class MediaSaveWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val gallerySaver: GallerySaver,
    private val notifications: MediaSaveNotifications
) : CoroutineWorker(context, params) {

    /** Har bir ish o'z bildirishnomasiga ega — bir nechta fayl parallel saqlansa aralashmasin. */
    private val notificationId: Int get() = id.hashCode()

    private val media: MessageMedia
        get() = MessageMedia(
            mediaId = inputData.getString(KEY_MEDIA_ID),
            kind = inputData.getString(KEY_KIND).orEmpty().toMediaKind(),
            mimeType = inputData.getString(KEY_MIME).orEmpty(),
            sizeBytes = inputData.getLong(KEY_SIZE, 0),
            width = null,
            height = null,
            durationMs = null,
            url = inputData.getString(KEY_URL),
            localPath = inputData.getString(KEY_LOCAL_PATH)
        )

    private val fileName: String get() = inputData.getString(KEY_FILE_NAME).orEmpty()

    /** Expedited ish Android 11 va pastda shu orqali darhol foreground bo'ladi. */
    override suspend fun getForegroundInfo(): ForegroundInfo =
        notifications.foregroundInfo(notificationId, media.kind == MediaKind.VIDEO, fileName, progress = null)

    override suspend fun doWork(): Result {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return Result.failure()
        val media = media
        val isVideo = media.kind == MediaKind.VIDEO
        setForeground(notifications.foregroundInfo(notificationId, isVideo, fileName, progress = null))

        var lastPercent = -1
        var lastUpdate = 0L
        return try {
            val uri = gallerySaver.save(media, fileName) { done, total ->
                val percent = if (total > 0) (done * 100 / total).toInt() else 0
                val now = SystemClock.elapsedRealtime()
                // Bildirishnoma har baytda emas: foizi o'zgarganda va ko'pi bilan ~2 marta/soniya (tizim cheklovi).
                if (percent != lastPercent && now - lastUpdate >= UPDATE_INTERVAL_MS) {
                    lastPercent = percent
                    lastUpdate = now
                    setForeground(notifications.foregroundInfo(notificationId, isVideo, fileName, percent))
                }
            }
            notifications.showDone(notificationId + 1, isVideo, uri, media.mimeType)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            if (runAttemptCount < MAX_ATTEMPTS - 1) {
                Result.retry()
            } else {
                notifications.showFailed(notificationId + 1)
                Result.failure()
            }
        }
    }

    companion object {
        private const val KEY_MEDIA_ID = "mediaId"
        private const val KEY_KIND = "kind"
        private const val KEY_MIME = "mimeType"
        private const val KEY_SIZE = "sizeBytes"
        private const val KEY_URL = "url"
        private const val KEY_LOCAL_PATH = "localPath"
        private const val KEY_FILE_NAME = "fileName"
        private const val UPDATE_INTERVAL_MS = 500L
        private const val MAX_ATTEMPTS = 3

        /** Worker'ga faqat oddiy qiymatlar uzatiladi (WorkManager Data — kichik key-value, obyekt emas). */
        fun inputData(media: MessageMedia, fileName: String) = workDataOf(
            KEY_MEDIA_ID to media.mediaId,
            KEY_KIND to media.kind.name,
            KEY_MIME to media.mimeType,
            KEY_SIZE to media.sizeBytes,
            KEY_URL to media.url,
            KEY_LOCAL_PATH to media.localPath,
            KEY_FILE_NAME to fileName
        )
    }
}
