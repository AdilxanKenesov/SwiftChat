package uz.relay.data.model.request

import kotlinx.serialization.Serializable

/**
 * `PATCH /v1/users/me` — o'z profilini yangilash. Faqat qiymat berilgan maydonlar yuboriladi
 * (`null` lar Json `explicitNulls = false` tufayli tashlab ketiladi), shuning uchun qisman yangilash mumkin.
 */
@Serializable
data class UpdateMeRequest(
    val displayName: String? = null,
    val username: String? = null
)
