package uz.relay.data.model.response

import kotlinx.serialization.Serializable

/* Bo'laklab (resumable) yuklash protokolining javob DTO'lari — [uz.relay.data.media.MediaUploader] ishlatadi. */

/** Upload sessiyasi ochildi: [uploadId] bo'laklarni yuborish uchun, [mediaId] xabarga biriktirish uchun. */
@Serializable
data class StartUploadResponse(
    val uploadId: String,
    val mediaId: String,
    /** Har bir PUT bo'lagining maksimal hajmi (hozir 512 KiB). */
    val chunkSize: Int
)

/** Bo'lak qabul qilindi. [mediaReady] — oxirgi bo'lak va sha256 to'g'ri: endi xabarga biriktirsa bo'ladi. */
@Serializable
data class ChunkAckResponse(
    val confirmedOffset: Long,
    val mediaReady: Boolean = false
)
