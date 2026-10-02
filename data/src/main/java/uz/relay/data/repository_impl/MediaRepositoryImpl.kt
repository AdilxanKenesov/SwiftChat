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
import uz.relay.data.media.MediaSaveScheduler
import uz.relay.data.media.safeFileName
import uz.relay.domain.model.DownloadState
import uz.relay.domain.model.MediaKind
import uz.relay.domain.model.MessageMedia
import uz.relay.domain.repository.MediaRepository
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import javax.inject.Inject

/**
 * [MediaRepository] implementatsiyasi: xabardagi faylni yuklab olish (progress bilan) va galereyaga saqlash.
 *
 * Retrofit emas, to'g'ridan-to'g'ri OkHttp ishlatiladi: katta faylni xotiraga to'liq olmasdan oqim bilan
 * diskka yozish va har bo'lakda progress chiqarish kerak. `@MediaClient` — token qo'shadigan, lekin body
 * logging'siz klient (BODY logger katta faylni xotiraga to'liq o'qib olardi).
 * Suhbat ekrani (fayl ochish), media ko'ruvchi (saqlash) ishlatadi.
 */
internal class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @MediaClient private val client: OkHttpClient,
    private val mediaFiles: MediaFiles,
    private val mediaSaveScheduler: MediaSaveScheduler,
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

        val target = File(mediaFiles.downloadsDir(mediaId), safeFileName(fileName))
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
     * Saqlash o'zi shu yerda bajarilmaydi — [MediaSaveWorker] (foreground service, bildirishnomada progress)
     * navbatga qo'yiladi va darhol qaytiladi: ekran yopilsa ham saqlash davom etadi.
     * Android 10 dan past versiyalarda MediaStore ruxsatsiz yozishni qo'llamaydi — xato qaytadi (UI tugmani yashiradi).
     */
    override suspend fun saveToGallery(media: MessageMedia, fileName: String): AppResult<Unit> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return AppResult.Error(AppError.Unknown(UnsupportedOperationException("API < 29")))
        mediaSaveScheduler.schedule(media, fileName)
        return AppResult.Success(Unit)
    }

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
    }
}
