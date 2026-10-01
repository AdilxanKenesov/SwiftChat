package uz.relay.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import android.webkit.MimeTypeMap
import androidx.core.graphics.scale
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.DigestInputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlinx.coroutines.withContext
import uz.relay.core.common.dispatcher.AppDispatchers

/**
 * Yuborishga tayyor fayl: nusxa, hash va serverga e'lon qilinadigan meta.
 * MessageRepositoryImpl undan UploadEntity va PENDING xabar qatorini yaratadi.
 */
data class PreparedMedia(
    val localPath: String,
    val posterPath: String?,
    /** IMAGE | VIDEO | FILE */
    val kind: String,
    val mimeType: String,
    val sizeBytes: Long,
    val sha256: String,
    val width: Int?,
    val height: Int?,
    val durationMs: Long?,
    val thumbBase64: String?,
    /** Foydalanuvchiga ko'rinadigan asl nom (FILE xabarida `body` sifatida yuboriladi). */
    val displayName: String
)

/** Fayl 100 MB dan katta — server baribir 413 qaytaradi, shuning uchun yuklashni boshlamaymiz ham. */
class MediaTooLargeException : IOException("Media is larger than the server limit")

/**
 * Tanlangan faylni yuborishga tayyorlaydi. Hammasi bir o'qishda: nusxalash bilan birga SHA-256 hisoblanadi
 * (katta videoni ikki marta o'qimaslik uchun).
 *
 * Nega nusxa: galereya/fayl tanlovchi bergan `content://` ruxsati vaqtinchalik — ilova qayta ochilganda
 * yoki WorkManager fonda ishlaganda u allaqachon yaroqsiz bo'lishi mumkin.
 *
 * Nega thumbnail klientda: server o'zi preview yasamaydi (protokol) — kichik (≤ 8 KB) JPEG'ni biz
 * yuboramiz, qabul qiluvchi uni asl fayl yuklanguncha xira preview sifatida ko'radi.
 * Chaqiruvchi: MessageRepositoryImpl (media xabar yuborilayotganda, outbox'ga qo'yishdan oldin).
 */
@Singleton
class MediaPreparer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaFiles: MediaFiles,
    private val dispatchers: AppDispatchers
) {

    /**
     * [uri] dagi faylni outbox papkasiga `clientMessageId` nomi bilan nusxalaydi va meta'sini yig'adi.
     * [asFile] = true — rasm/video bo'lsa ham hujjat sifatida (siqilmagan, preview'siz) yuboriladi.
     * @throws MediaTooLargeException fayl server chegarasidan katta.
     */
    suspend fun prepare(uri: String, asFile: Boolean, clientMessageId: String): PreparedMedia = withContext(dispatchers.io) {
        val source = uri.toUri()
        val resolver = context.contentResolver
        val displayName = queryDisplayName(source) ?: "file"
        // MIME: avval provider'dan, bo'lmasa kengaytmadan, oxirida umumiy binary tur.
        val mimeType = resolver.getType(source)
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(displayName.substringAfterLast('.', "").lowercase())
            ?: "application/octet-stream"

        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)?.let { ".$it" }.orEmpty()
        val target = File(mediaFiles.outboxDir, clientMessageId + extension)
        val sha256 = try {
            copyWithHash(source, target)
        } catch (e: Exception) {
            // Yarim yozilgan nusxa diskda qolib ketmasin.
            target.delete()
            throw e
        }
        val size = target.length()
        if (size > MAX_MEDIA_BYTES) {
            target.delete()
            throw MediaTooLargeException()
        }

        val kind = when {
            asFile -> "FILE"
            mimeType.startsWith("image/") -> "IMAGE"
            mimeType.startsWith("video/") -> "VIDEO"
            else -> "FILE"
        }
        val base = PreparedMedia(
            localPath = target.path,
            posterPath = null,
            kind = kind,
            mimeType = mimeType,
            sizeBytes = size,
            sha256 = sha256,
            width = null,
            height = null,
            durationMs = null,
            thumbBase64 = null,
            displayName = displayName
        )
        // Meta'ni o'qib bo'lmasa ham fayl yuboriladi — o'lcham/thumbnail ixtiyoriy.
        runCatching {
            when (kind) {
                "IMAGE" -> withImageMeta(base, target)
                "VIDEO" -> withVideoMeta(base, target, clientMessageId)
                else -> base
            }
        }.getOrDefault(base)
    }

    /** Faylni nusxalaydi va shu o'qishning o'zida SHA-256 ni hisoblaydi; hex satr qaytaradi. */
    private fun copyWithHash(source: Uri, target: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = context.contentResolver.openInputStream(source) ?: throw IOException("Cannot open $source")
        DigestInputStream(input, digest).use { hashing ->
            FileOutputStream(target).use { output -> hashing.copyTo(output, BUFFER_SIZE) }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * O'lcham EXIF burilishini hisobga olgan holda: telefonda tik olingan surat piksellarda yotiq saqlanadi,
     * bubble esa uni tik (to'g'ri nisbatda) ko'rsatishi kerak.
     */
    private fun withImageMeta(base: PreparedMedia, file: File): PreparedMedia {
        // inJustDecodeBounds — pikselni xotiraga yuklamasdan faqat o'lchamni o'qish (katta suratda OOM bo'lmasin).
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return base
        val rotated = runCatching {
            ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) in ROTATED_ORIENTATIONS
        }.getOrDefault(false)
        val (width, height) = if (rotated) bounds.outHeight to bounds.outWidth else bounds.outWidth to bounds.outHeight

        // Thumbnail uchun kichraytirib decode qilamiz — to'liq o'lchamli bitmap kerak emas.
        val sample = BitmapFactory.Options().apply { inSampleSize = sampleSizeFor(max(bounds.outWidth, bounds.outHeight), THUMB_SIDE) }
        val thumb = BitmapFactory.decodeFile(file.path, sample)?.let { encodeThumb(it) }
        return base.copy(width = width, height = height, thumbBase64 = thumb)
    }

    /** Video: o'lcham (burilish bilan), davomiylik va birinchi kadr — u bubble'dagi poster ham, thumbnail ham. */
    private fun withVideoMeta(base: PreparedMedia, file: File, clientMessageId: String): PreparedMedia {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.path)
            val rawWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
            val rawHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            val (width, height) = if (rotation == 90 || rotation == 270) rawHeight to rawWidth else rawWidth to rawHeight

            val frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            val posterPath = frame?.let { savePoster(it, clientMessageId) }
            val thumb = frame?.let { encodeThumb(it) }
            return base.copy(width = width, height = height, durationMs = duration, posterPath = posterPath, thumbBase64 = thumb)
        } finally {
            retriever.release()
        }
    }

    /** Video poster'ini lokal JPEG qilib saqlaydi — yuboruvchining o'zi bubble'da darhol ko'rishi uchun. */
    private fun savePoster(frame: Bitmap, clientMessageId: String): String {
        val scaled = frame.scaledToFit(POSTER_SIDE)
        val file = File(mediaFiles.outboxDir, "$clientMessageId.poster.jpg")
        FileOutputStream(file).use { scaled.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        return file.path
    }

    /** ≤ 8 KB (server cheklovi): sifat pasaytirib boriladi, baribir sig'masa — thumbnail yuborilmaydi. */
    private fun encodeThumb(bitmap: Bitmap): String? {
        val small = bitmap.scaledToFit(THUMB_SIDE)
        for (quality in intArrayOf(70, 50, 30)) {
            val bytes = ByteArrayOutputStream().also { small.compress(Bitmap.CompressFormat.JPEG, quality, it) }.toByteArray()
            if (bytes.size <= MAX_THUMB_BYTES) return Base64.encodeToString(bytes, Base64.NO_WRAP)
        }
        return null
    }

    /** Nisbatni saqlagan holda uzun tomonini [maxSide] gacha kichraytiradi (kattalashtirmaydi). */
    private fun Bitmap.scaledToFit(maxSide: Int): Bitmap {
        val longest = max(width, height)
        if (longest <= maxSide) return this
        val ratio = maxSide.toFloat() / longest
        return scale((width * ratio).toInt().coerceAtLeast(1), (height * ratio).toInt().coerceAtLeast(1))
    }

    /** BitmapFactory uchun 2 ning darajasi bo'lgan eng katta inSampleSize (natija [target] dan kichik bo'lmasin). */
    private fun sampleSizeFor(longestSide: Int, target: Int): Int {
        var sample = 1
        while (longestSide / (sample * 2) >= target) sample *= 2
        return sample
    }

    /** Faylning foydalanuvchiga ko'rinadigan nomi; provider bermasa — URI'ning oxirgi qismi. */
    private fun queryDisplayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment

    private companion object {
        /** Server cheklovi (`/v1/server/info` → maxMediaSizeBytes). */
        const val MAX_MEDIA_BYTES = 100L * 1024 * 1024
        /** Server `thumbBase64` uchun qabul qiladigan maksimal hajm (dekodlangan baytlarda). */
        const val MAX_THUMB_BYTES = 8 * 1024
        const val THUMB_SIDE = 96
        const val POSTER_SIDE = 720
        const val BUFFER_SIZE = 64 * 1024
        /** EXIF'da eni va bo'yi almashadigan (90/270 gradus) yo'nalishlar. */
        val ROTATED_ORIENTATIONS = setOf(
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_ROTATE_270,
            ExifInterface.ORIENTATION_TRANSPOSE,
            ExifInterface.ORIENTATION_TRANSVERSE
        )
    }
}
