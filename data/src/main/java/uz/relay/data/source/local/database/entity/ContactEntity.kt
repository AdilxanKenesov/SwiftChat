package uz.relay.data.source.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Qurilmadagi kontakt — faqat id va qo'shilgan vaqt. Ism/username/online holat `users` jadvalidan JOIN bilan
 * olinadi: profil bir joyda saqlanadi va presence yangilansa kontaktlar ro'yxati ham o'zi yangilanadi.
 */
@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val userId: String,
    val addedAt: Long
)
