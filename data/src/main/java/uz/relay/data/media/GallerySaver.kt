package uz.relay.data.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import uz.relay.core.common.dispatcher.AppDispatchers
import uz.relay.domain.model.DownloadState
import uz.relay.domain.model.MediaKind
import uz.relay.domain.model.MessageMedia
import uz.relay.domain.repository.MediaRepository
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rasm/videoni telefon galereyasiga saqlash: avval faylni yuklab oladi (keshda yoki lokal nusxa bo'lsa —
 * tarmoqsiz), keyin MediaStore'ga yozadi. [MediaSaveWorker] ichidan chaqiriladi — progress uning
 * bildirishnomasiga uzatiladi.
 *
 * Nega MediaStore (Android 10+): xotiraga yozish ruxsati so'ralmaydi, fayl Pictures/SwiftChat yoki
 * Movies/SwiftChat'ga tushadi va galereya ilovasida darhol ko'rinadi.
 */
@Singleton
class GallerySaver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaRepository: MediaRepository,
    private val dispatchers: AppDispatchers
) {

    /**
     * @param onProgress yuklab olish progressi (bayt, jami) — bildirishnoma chizig'i uchun.
     * @return galereyadagi yangi yozuv (bildirishnomani bosganda ochiladi).
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun save(media: MessageMedia, fileName: String, onProgress: suspend (Long, Long) -> Unit): Uri {
        var source: String? = null
        mediaRepository.download(media, fileName).collect { state ->
            when (state) {
                is DownloadState.Progress -> onProgress(state.downloadedBytes, state.totalBytes)
                is DownloadState.Done -> source = state.path
            }
        }
        val path = source ?: throw IOException("Download finished without a file")
        return withContext(dispatchers.io) { insert(File(path), media, fileName) }
    }

    /**
     * `IS_PENDING` — yozib bo'lingunicha galereya yarim faylni ko'rsatmaydi. Xato bo'lsa yarim yozuv o'chiriladi:
     * galereyada "singan" rasm qolmasin.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun insert(file: File, media: MessageMedia, fileName: String): Uri {
        val isVideo = media.kind == MediaKind.VIDEO
        val collection = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, safeFileName(fileName))
            put(MediaStore.MediaColumns.MIME_TYPE, media.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, (if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES) + "/SwiftChat")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(collection, values) ?: throw IOException("MediaStore insert failed")
        try {
            resolver.openOutputStream(uri)?.use { output -> FileInputStream(file).use { it.copyTo(output) } }
                ?: throw IOException("Cannot open gallery output")
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (e: IOException) {
            resolver.delete(uri, null, null)
            throw e
        }
        return uri
    }
}

/** Fayl nomi boshqa odamdan keladi (`body`) — papka yo'lini buzadigan belgilar olib tashlanadi. */
internal fun safeFileName(name: String): String =
    name.replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001f]"), "_").trim().take(120).ifEmpty { "file" }
