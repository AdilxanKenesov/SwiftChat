package uz.relay.domain.model

/** Media turi (server `MediaMeta.kind`). Noma'lum tur FILE sifatida ko'rsatiladi — yuklab olish baribir ishlaydi. */
enum class MediaKind { IMAGE, VIDEO, FILE }

/**
 * Xabarga biriktirilgan bitta media.
 *
 * Ikki manba bitta modelda: serverdagi media ([mediaId]/[url]) va o'zim yuborayotgan faylning qurilmadagi
 * nusxasi ([localPath]). O'zim yuborgan rasm server javobidan keyin ham lokal nusxadan ko'rsatiladi —
 * qayta yuklab olish shart emas va ekranda "miltillash" bo'lmaydi.
 */
data class MessageMedia(
    /** Server id'si. Fayl hali serverga to'liq yuklanmagan bo'lsa `null`. */
    val mediaId: String?,
    val kind: MediaKind,
    val mimeType: String,
    val sizeBytes: Long,
    val width: Int?,
    val height: Int?,
    val durationMs: Long?,
    /** `GET /v1/media/{id}` — faqat token bilan ochiladi (Coil/ExoPlayer authorized klient orqali). */
    val url: String?,
    /** O'zim yuborgan faylning qurilmadagi nusxasi. */
    val localPath: String? = null,
    /** O'zim yuborgan videoning birinchi kadri (server thumbnail bermaydi). */
    val posterPath: String? = null
)

/** Yuborilayotgan faylning qancha qismi serverga yetib bordi. */
data class UploadProgress(val sentBytes: Long, val totalBytes: Long) {
    val fraction: Float get() = if (totalBytes <= 0) 0f else (sentBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
}

/**
 * Yuborish uchun tanlangan fayl.
 * @param uri `content://` manzil (galereya, kamera yoki fayl tanlovchi).
 * @param asFile `true` — "Fayl" orqali tanlangan: rasm bo'lsa ham siqilmagan hujjat sifatida yuboriladi.
 */
data class Attachment(val uri: String, val asFile: Boolean)

/** Faylni qurilmaga yuklab olish holati (hujjat ochish uchun). */
sealed interface DownloadState {
    data class Progress(val downloadedBytes: Long, val totalBytes: Long) : DownloadState
    data class Done(val path: String) : DownloadState
}
