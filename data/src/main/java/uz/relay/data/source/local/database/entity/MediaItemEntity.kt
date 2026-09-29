package uz.relay.data.source.local.database.entity

import androidx.room.TypeConverter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Xabar ichidagi media meta'si. Alohida jadval emas, xabar qatorida JSON ustun sifatida saqlanadi:
 * u xabardan ajralmas (xabar bilan birga keladi va o'chadi), alohida so'ralmaydi.
 */
@Serializable
data class MediaItemEntity(
    val mediaId: String,
    val kind: String,
    val mimeType: String,
    val sizeBytes: Long,
    val width: Int? = null,
    val height: Int? = null,
    val durationMs: Long? = null
)

/**
 * Room TypeConverter: `List<MediaItemEntity>` ↔ JSON matn.
 * Tiplar aniq yozilgan: KSP (Room) serialization plugin yaratadigan tiplarni hali ko'rmaydi.
 */
class MediaConverters {
    // ignoreUnknownKeys: keyinroq qo'shilgan maydonlar eski versiyani yiqitmasin.
    private val json: Json = Json { ignoreUnknownKeys = true }
    private val serializer: KSerializer<List<MediaItemEntity>> = ListSerializer(MediaItemEntity.serializer())

    @TypeConverter
    fun fromList(items: List<MediaItemEntity>): String = json.encodeToString(serializer, items)

    /** Buzuq qiymat butun xabarlar ro'yxatini yiqitmasin — bo'sh ro'yxat. */
    @TypeConverter
    fun toList(raw: String): List<MediaItemEntity> = runCatching { json.decodeFromString(serializer, raw) }.getOrDefault(emptyList())
}
