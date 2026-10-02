package uz.relay.data.source.local.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * O'zim yuborayotgan faylning yuklash holati. Xabar bilan 1:1 ([clientMessageId]).
 *
 * Nega bazada: yuklash ilova yopilganda ham to'xtagan joyidan davom etishi kerak. `uploadId` va
 * `confirmedBytes` shu yerda turadi; qayta ishga tushganda haqiqiy offset serverdan (HEAD) so'raladi —
 * lokal qiymat faqat progress ko'rsatish uchun.
 *
 * Xabar yuborilgandan keyin ham o'chirilmaydi: [localPath] orqali o'z rasmim/videom qayta yuklab olinmasdan
 * lokal nusxadan ko'rsatiladi.
 */
@Entity(tableName = "uploads", indices = [Index("chatId")])
data class UploadEntity(
    @PrimaryKey val clientMessageId: String,
    val chatId: String,
    /** Ilova papkasidagi nusxa (filesDir/media_outbox/...). */
    val localPath: String,
    /** Video uchun birinchi kadr (JPEG). */
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
    /** Server yuklash sessiyasi. `null` — hali boshlanmagan (yoki muddati o'tib, qaytadan boshlanadi). */
    val uploadId: String?,
    val mediaId: String?,
    val chunkSize: Int,
    val confirmedBytes: Long,
    /** Server `mediaReady: true` qaytardi — xabarni yuborsa bo'ladi. */
    val completed: Boolean
)
