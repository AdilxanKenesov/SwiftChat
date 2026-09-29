package uz.relay.data.media

import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import uz.relay.core.common.dispatcher.AppDispatchers
import uz.relay.core.common.result.AppError
import uz.relay.core.common.result.AppResult
import uz.relay.core.common.result.ErrorCodes
import uz.relay.data.model.request.StartUploadRequest
import uz.relay.data.source.local.database.dao.UploadDao
import uz.relay.data.source.local.database.entity.UploadEntity
import uz.relay.data.source.network.api.MediaApi
import uz.relay.data.utils.safeApiCall
import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Faylni serverga bo'laklab yuklaydi va to'xtagan joyidan davom ettiradi (Guide: resumable upload).
 *
 * Qoidalar:
 *  - offset HECH QACHON taxmin qilinmaydi: davom ettirishda (ilova qayta ochildi, internet uzildi)
 *    serverdan `HEAD` bilan so'raladi, 409 OFFSET_MISMATCH'dan keyin ham shunday;
 *  - sessiya muddati o'tsa (UPLOAD_EXPIRED) yoki topilmasa (404) — yangi sessiya, noldan;
 *  - SHA256_MISMATCH — fayl buzilgan: sessiya tashlanadi, xabar FAILED bo'ladi (qayta urinish noldan boshlaydi);
 *  - har bo'lakdan keyin progress bazaga yoziladi — UI halqasi shundan chiziladi.
 *
 * Bekor qilish: har bo'lakdan oldin yuklash qatori hali bormi deb tekshiriladi. Foydalanuvchi ×ni bossa,
 * qator o'chiriladi va yuklash keyingi bo'lakda to'xtaydi.
 */
@Singleton
class MediaUploader @Inject constructor(
    private val mediaApi: MediaApi,
    private val uploadDao: UploadDao,
    private val dispatchers: AppDispatchers
) {

    /** Natija — xabarga biriktiriladigan `mediaId`. */
    suspend fun upload(initial: UploadEntity): AppResult<String> {
        if (initial.completed && initial.mediaId != null) return AppResult.Success(initial.mediaId)
        val file = File(initial.localPath)
        if (!file.exists()) return fileMissing()

        var restarts = 0
        var upload = initial
        var offset: Long

        // 1. Sessiya: yangisini ochish yoki mavjudining serverdagi offset'ini bilish.
        if (upload.uploadId == null) {
            upload = when (val started = startSession(upload)) {
                is AppResult.Success -> started.data
                is AppResult.Error -> return started
            }
            offset = 0
        } else {
            offset = when (val remote = remoteOffset(upload.uploadId!!)) {
                is AppResult.Success -> remote.data
                is AppResult.Error -> if (remote.error.isSessionGone()) {
                    upload = when (val started = startSession(upload)) {
                        is AppResult.Success -> started.data
                        is AppResult.Error -> return started
                    }
                    0
                } else return remote
            }
        }

        // 2. Bo'laklar.
        while (true) {
            if (uploadDao.get(upload.clientMessageId) == null) return canceled()
            val chunk = readChunk(file, offset, upload.chunkSize)
            val uploadId = upload.uploadId!!

            when (val result = safeApiCall { mediaApi.uploadChunk(uploadId, offset, chunk.toRequestBody(OCTET_STREAM)) }) {
                is AppResult.Success -> {
                    offset = result.data.confirmedOffset
                    if (result.data.mediaReady) {
                        uploadDao.markCompleted(upload.clientMessageId)
                        return AppResult.Success(upload.mediaId!!)
                    }
                    uploadDao.updateProgress(upload.clientMessageId, offset)
                    // Hamma bayt yetib bordi-yu, server tayyor demadi — kutilmagan holat; cheksiz aylanmaslik uchun.
                    if (offset >= upload.sizeBytes) return AppResult.Error(AppError.Api(0, ErrorCodes.MEDIA_NOT_READY, "Upload incomplete", retryable = true))
                }

                is AppResult.Error -> {
                    val error = result.error
                    when {
                        error.hasCode(ErrorCodes.OFFSET_MISMATCH) -> offset = when (val remote = remoteOffset(uploadId)) {
                            is AppResult.Success -> remote.data
                            is AppResult.Error -> return remote
                        }

                        error.isSessionGone() && restarts < MAX_RESTARTS -> {
                            restarts++
                            upload = when (val started = startSession(upload)) {
                                is AppResult.Success -> started.data
                                is AppResult.Error -> return started
                            }
                            offset = 0
                        }

                        error.hasCode(ErrorCodes.SHA256_MISMATCH) -> {
                            uploadDao.resetSession(upload.clientMessageId)
                            return result
                        }

                        else -> return result
                    }
                }
            }
        }
    }

    private suspend fun startSession(upload: UploadEntity): AppResult<UploadEntity> {
        val request = StartUploadRequest(
            kind = upload.kind,
            mimeType = upload.mimeType,
            sizeBytes = upload.sizeBytes,
            sha256 = upload.sha256,
            width = upload.width,
            height = upload.height,
            durationMs = upload.durationMs,
            thumbBase64 = upload.thumbBase64
        )
        return when (val result = safeApiCall { mediaApi.startUpload(request) }) {
            is AppResult.Success -> {
                val session = result.data
                uploadDao.startSession(upload.clientMessageId, session.uploadId, session.mediaId, session.chunkSize)
                AppResult.Success(
                    upload.copy(uploadId = session.uploadId, mediaId = session.mediaId, chunkSize = session.chunkSize, confirmedBytes = 0)
                )
            }
            is AppResult.Error -> result
        }
    }

    /** `HEAD` → `Upload-Offset` sarlavhasi. 404/403 — sessiya yo'q (chaqiruvchi yangisini ochadi). */
    private suspend fun remoteOffset(uploadId: String): AppResult<Long> {
        val result = safeApiCall { mediaApi.uploadOffset(uploadId) }
        if (result is AppResult.Error) return result
        val response = (result as AppResult.Success).data
        if (!response.isSuccessful) {
            return AppResult.Error(
                AppError.Api(response.code(), if (response.code() == 404) ErrorCodes.NOT_FOUND else "HTTP_${response.code()}", "HEAD failed", response.code() >= 500)
            )
        }
        val offset = response.headers()[UPLOAD_OFFSET]?.toLongOrNull()
            ?: return AppResult.Error(AppError.Api(response.code(), "NO_OFFSET", "Upload-Offset header missing", retryable = true))
        return AppResult.Success(offset)
    }

    private suspend fun readChunk(file: File, offset: Long, chunkSize: Int): ByteArray = withContext(dispatchers.io) {
        RandomAccessFile(file, "r").use { raf ->
            val size = minOf(chunkSize.toLong(), raf.length() - offset).toInt().coerceAtLeast(0)
            ByteArray(size).also {
                raf.seek(offset)
                raf.readFully(it)
            }
        }
    }

    private fun AppError.hasCode(code: String) = this is AppError.Api && this.code == code

    private fun AppError.isSessionGone() =
        hasCode(ErrorCodes.UPLOAD_EXPIRED) || (this is AppError.Api && (httpStatus == 404 || httpStatus == 403))

    private fun fileMissing(): AppResult<String> =
        AppResult.Error(AppError.Api(0, ErrorCodes.MEDIA_FILE_MISSING, "Local file is missing", retryable = false))

    /** Bekor qilingan: chaqiruvchi xabar ham o'chganini ko'radi va keyingisiga o'tadi. */
    private fun canceled(): AppResult<String> =
        AppResult.Error(AppError.Api(0, CANCELED, "Upload canceled", retryable = false))

    companion object {
        const val CANCELED = "UPLOAD_CANCELED"
        private const val UPLOAD_OFFSET = "Upload-Offset"
        private const val MAX_RESTARTS = 2
        private val OCTET_STREAM = "application/octet-stream".toMediaType()
    }
}
