package uz.relay.data.source.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Update oqimidagi kursor — "qaysi updateSeq gacha qo'llab bo'ldim". Bitta qatorli jadval.
 *
 * Nega DataStore'da emas, Room'da: update'ni qo'llash va kursorni oshirish BITTA tranzaksiyada bo'lishi
 * shart. Aks holda ilova o'rtada o'ldirilsa, kursor oshgan-u ma'lumot yozilmagan (yoki aksincha)
 * holat qolardi — ya'ni hodisa jimgina yo'qolardi.
 */
@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val updateSeq: Long
) {
    companion object {
        // Jadvalda doim bitta qator — id doim shu qiymat, upsert o'sha qatorni yangilaydi.
        const val SINGLE_ROW_ID = 0
    }
}
