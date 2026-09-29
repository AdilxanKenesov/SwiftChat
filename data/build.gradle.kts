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
    namespace = "uz.relay.data"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        // Android 8.0+ (java.time va adaptive icon'lar tayyor).
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Server manzillari BuildConfig orqali (kodga qattiq yozilmaydi, build turiga qarab almashtirish oson).
        buildConfigField("String", "BASE_URL", "\"https://relay.zokirov-mob-dev.uz/\"")
        buildConfigField("String", "WS_URL", "\"wss://relay.zokirov-mob-dev.uz/v1/ws\"")
    }
    // Java 11 — barcha modullarda bir xil.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    // Loyiha modullari.
    implementation(project(":domain"))
    implementation(project(":core:common"))

    // AndroidX Core — Kotlin extension'lar.
    implementation(libs.androidx.core.ktx)

    // Hilt — DI (ksp generatsiya qiladi).
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Tarmoq: Retrofit (REST), OkHttp (HTTP va WebSocket, logging), kotlinx.serialization (JSON).
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)

    // Coroutines (Android Main dispatcher bilan).
    implementation(libs.kotlinx.coroutines.android)

    // Room — lokal baza (offline-first, UI uchun yagona haqiqat manbai); ksp DAO kodini generatsiya qiladi.
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)

    // DataStore — sozlamalar va sessiya; Tink — tokenlarni shifrlab saqlash.
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.tink.android)
    // Paging — Room'dan sahifalab o'qish.
    implementation(libs.androidx.paging.runtime)

    // WorkManager — outbox'ni fonda, internet qaytganda yuborish; hilt-work — worker'larga inject.
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // ProcessLifecycleOwner — ilova old/orqa planda ekanini bilish (WebSocket ulash/uzish).
    implementation(libs.androidx.lifecycle.process)

    // Media: rasmlarni token bilan yuklovchi ImageLoader va video oqimi uchun DataSource (ikkalasi ham
    // authorized OkHttp klient ustida — token eskirsa o'zi yangilanadi).
    implementation(libs.coil.network.okhttp)
    implementation(libs.media3.datasource.okhttp)

    // Firebase Cloud Messaging — push (data-only); play-services — Task'ni coroutine'da kutish.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.kotlinx.coroutines.play.services)

    // Testlar.
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
