package uz.relay.data.source.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Foydalanuvchi profillari keshi (o'zimniki ham shu yerda).
 *
 * Nega kerak: DIRECT chatda server `title` bermaydi, faqat `peerUserId` beradi. Ro'yxatda ism ko'rsatish
 * uchun profil lokal saqlanadi va har safar tarmoqqa chiqilmaydi (butun sinf bitta IP'dan
 * 300 so'rov/daqiqa limitini bo'lishadi).
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val username: String?,
    val avatarMediaId: String?,
    val avatarVersion: Int,
    val online: Boolean,
    val lastSeenAt: Long?,
    /** Faqat o'zimning profilimda bo'ladi — server boshqalarning raqamini bermaydi. */
    val phone: String? = null
)
