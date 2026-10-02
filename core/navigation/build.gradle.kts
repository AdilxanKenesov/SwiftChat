// Gradle plugin'lari: modul turi va kod generatsiya vositalari.
plugins {
    // Android kutubxona moduli — app moduliga qo'shiladi.
    alias(libs.plugins.android.library)
    // kotlinx.serialization — @Serializable klasslar uchun serializer generatsiyasi.
    alias(libs.plugins.kotlin.serialization)
    // KSP — annotation processor: Hilt/Room kodini generatsiya qiladi (kapt'dan tezroq).
    alias(libs.plugins.ksp)
    // Hilt — dependency injection.
    alias(libs.plugins.hilt)
}

// Android sozlamalari: namespace, SDK versiyalari, Java versiyasi.
android {
    namespace = "uz.relay.core.navigation"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        // Android 8.0+ (java.time va adaptive icon'lar tayyor).
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    // Java 11 — barcha modullarda bir xil.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Loyiha modullari.
    implementation(project(":core:common"))

    implementation(libs.androidx.core.ktx)

    // Navigation 3 NavKey va kotlinx.serialization `api` bilan: kalitlar shu modulda, feature'lar ham ularni ko'rishi kerak.
    api(libs.androidx.navigation3.runtime)
    api(libs.kotlinx.serialization.json)

    // Hilt — DI (ksp generatsiya qiladi).
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Channel/Flow — navigatsiya event bus'i uchun.
    implementation(libs.kotlinx.coroutines.core)

    // Testlar.
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
