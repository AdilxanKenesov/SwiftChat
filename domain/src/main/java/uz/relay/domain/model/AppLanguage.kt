package uz.relay.domain.model

/**
 * Ilova tili. [tag] — BCP-47 til kodi (Android resurs papkalari: `values` = o'zbekcha, `values-ru`, `values-en`).
 *
 * Foydalanuvchi tanlamagan bo'lsa, til telefonga qarab aniqlanadi: ruscha/inglizcha telefonda — o'sha til,
 * qolgan hamma holatda — o'zbekcha.
 */
enum class AppLanguage(val tag: String) {
    UZ("uz"),
    RU("ru"),
    EN("en");

    companion object {
        /** Qo'llanmaydigan til (masalan, "de") o'zbekchaga tushadi — bu ilovaning asosiy tili. */
        fun fromTag(tag: String?): AppLanguage = entries.firstOrNull { it.tag == tag?.substringBefore('-')?.lowercase() } ?: UZ
    }
}
