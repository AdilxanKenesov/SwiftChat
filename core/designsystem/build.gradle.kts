// Gradle plugin'lari: modul turi va kod generatsiya vositalari.
plugins {
    // Android kutubxona moduli — app moduliga qo'shiladi.
    alias(libs.plugins.android.library)
    // Compose kompilyator plugin'i (@Composable funksiyalar uchun).
    alias(libs.plugins.kotlin.compose)
}

// Android sozlamalari: namespace, SDK versiyalari, Java versiyasi.
android {
    namespace = "uz.relay.core.designsystem"
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
    buildFeatures {
        compose = true
    }
    // Compose UI testlari JVM'da (Robolectric) — resurslar (matnlar, ikonkalar) testga ham kirsin.
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    // AndroidX Core — Kotlin extension'lar.
    implementation(libs.androidx.core.ktx)

    // Compose `api` bilan: designsystem'ni ulagan feature'lar Compose'ni alohida qo'shmaydi. BOM — hamma Compose kutubxonalari mos versiyada.
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.ui.tooling.preview)
    // Faqat debug: Android Studio preview va Layout Inspector.
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Coil — rasm yuklash (avatar/rasm komponentlari uchun).
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Testlar.
    testImplementation(libs.junit)
    // Compose UI testlari: emulyatorsiz, Robolectric ustida (JVM).
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
