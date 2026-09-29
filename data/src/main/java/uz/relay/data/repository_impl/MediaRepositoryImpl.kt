package uz.relay.data.repository_impl

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import uz.relay.core.common.dispatcher.AppDispatchers
import uz.relay.core.common.result.AppError
import uz.relay.core.common.result.AppResult
import uz.relay.data.di.MediaClient
import uz.relay.data.media.MediaFiles
import uz.relay.domain.model.DownloadState
import uz.relay.domain.model.MediaKind
import uz.relay.domain.model.MessageMedia
import uz.relay.domain.repository.MediaRepository
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import javax.inject.Inject

internal class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @MediaClient private val client: OkHttpClient,
    private val mediaFiles: MediaFiles,
    private val dispatchers: AppDispatchers
) : MediaRepository {

    /**
     * Lokal nusxa (o'zim yuborgan) yoki oldin yuklab olingan fayl bo'lsa — tarmoqqa chiqilmaydi.
     * Aks holda oqim bilan vaqtinchalik faylga yoziladi va tugagach nomi o'zgartiriladi: yarim yuklangan
     * fayl hech qachon "tayyor" deb ochilmaydi.
     */
    override fun download(media: MessageMedia, fileName: String): Flow<DownloadState> = flow {
        media.localPath?.let(::File)?.takeIf { it.exists() }?.let {
            emit(DownloadState.Done(it.path))
            return@flow
        }
        val mediaId = media.mediaId ?: throw IOException("Media is not uploaded yet")
        val url = media.url ?: throw IOException("Media has no url")

        val target = File(mediaFiles.downloadsDir(mediaId), safeName(fileName))
        if (target.exists() && target.length() == media.sizeBytes) {
            emit(DownloadState.Done(target.path))
            return@flow
        }

        val temp = File(target.path + ".part")
        val call = client.newCall(Request.Builder().url(url).build())
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                val body = response.body
                val total = body.contentLength().takeIf { it > 0 } ?: media.sizeBytes
                body.byteStream().use { input ->
                    temp.outputStream().use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var downloaded = 0L
                        var lastEmitted = 0L
                        while (true) {
                            // Foydalanuvchi ekrandan chiqsa — yuklash to'xtaydi (call ham yopiladi).
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            // Har bayt uchun emas — UI'ni ortiqcha qayta chizmaslik uchun ~1% qadam bilan.
                            if (downloaded - lastEmitted >= total / 100 || downloaded == total) {
                                emit(DownloadState.Progress(downloaded, total))
                                lastEmitted = downloaded
                            }
                        }
                    }
                }
            }
            if (!temp.renameTo(target)) throw IOException("Cannot move downloaded file")
            emit(DownloadState.Done(target.path))
        } catch (e: CancellationException) {
            call.cancel()
            temp.delete()
            throw e
        } catch (e: IOException) {
            temp.delete()
            throw e
        }
    }.flowOn(dispatchers.io)

    /**
     * MediaStore orqali (Android 10+): ruxsat so'ralmaydi, fayl Pictures/SwiftChat yoki Movies/SwiftChat'ga
     * tushadi. `IS_PENDING` — yozib bo'lingunicha galereya yarim faylni ko'rsatmaydi.
     */
    override suspend fun saveToGallery(media: MessageMedia, fileName: String): AppResult<Unit> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return AppResult.Error(AppError.Unknown(UnsupportedOperationException("API < 29")))
        return try {
            val source = download(media, fileName).filterIsInstance<DownloadState.Done>().last()
            withContext(dispatchers.io) { insertIntoGallery(File(source.path), media, fileName) }
            AppResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            AppResult.Error(AppError.Network)
        } catch (e: NoSuchElementException) {
            AppResult.Error(AppError.Unknown(e))
        }
    }

    private fun insertIntoGallery(file: File, media: MessageMedia, fileName: String) {
        val isVideo = media.kind == MediaKind.VIDEO
        val collection = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, safeName(fileName))
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
    }

    /** Fayl nomi boshqa odamdan keladi (`body`) — papka yo'lini buzadigan belgilar olib tashlanadi. */
    private fun safeName(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001f]"), "_").trim().take(MAX_NAME).ifEmpty { "file" }

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
        const val MAX_NAME = 120
    }
}
