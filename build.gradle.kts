// Ildiz build fayli: hamma modullar uchun umumiy sozlamalar.
// Plugin'lar shu yerda `apply false` bilan e'lon qilinadi — versiya bir marta aniqlanadi,
// har bir modul esa faqat kerakli plugin'ni o'zi qo'llaydi.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    // Google Services — Firebase (FCM push) konfiguratsiyasi uchun.
    alias(libs.plugins.google.services) apply false
    // Toza Kotlin/JVM modullar (domain, core:common) uchun.
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    // Screenshot testlari (Robolectric ustida): golden rasmlarni yozish va solishtirish.
    alias(libs.plugins.roborazzi) apply false
}
