package uz.relay.feature.profile.util

private const val UZ_PREFIX = "+998"

/**
 * "+998901234567" → "+998 90 123 45 67". Boshqa formatdagi raqam o'zgarishsiz qaytadi — serverda
 * faqat +998 raqamlar bor, lekin ko'rsatishda xato qilgandan ko'ra xom holini ko'rsatgan yaxshi.
 */
internal fun formatPhone(phone: String): String {
    val local = phone.removePrefix(UZ_PREFIX)
    if (local.length != 9 || !phone.startsWith(UZ_PREFIX)) return phone
    return "$UZ_PREFIX ${local.substring(0, 2)} ${local.substring(2, 5)} ${local.substring(5, 7)} ${local.substring(7)}"
}
