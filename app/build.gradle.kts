// Gradle plugin'lari: modul turi va kod generatsiya vositalari.
plugins {
    // Android ilova (APK) moduli.
    alias(libs.plugins.android.application)
    // Compose kompilyator plugin'i (@Composable funksiyalar uchun).
    alias(libs.plugins.kotlin.compose)
    // kotlinx.serialization — @Serializable klasslar uchun serializer generatsiyasi.
    alias(libs.plugins.kotlin.serialization)
    // KSP — annotation processor: Hilt/Room kodini generatsiya qiladi (kapt'dan tezroq).
    alias(libs.plugins.ksp)
    // Hilt — dependency injection.
    alias(libs.plugins.hilt)
}

// Android sozlamalari: namespace, SDK versiyalari, Java versiyasi.
android {
    namespace = "uz.relay.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        // uz.relay.app bo'lib qolishi shart: Relay Firebase loyihasi faqat shu package uchun ro'yxatdan o'tgan.
        applicationId = "uz.relay.app"
        // Android 8.0+ (java.time va adaptive icon'lar tayyor).
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        // Release'da kod qisqartirish/obfuskatsiya hozircha o'chiq.
        release {
            optimization {
                enable = false
            }
        }
    }
    // Java 11 — barcha modullarda bir xil.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Loyiha modullari.
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:chats"))
    implementation(project(":feature:conversation"))
    implementation(project(":feature:group"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:calls"))



    // Compose (BOM — hamma Compose kutubxonalari mos versiyada) va Activity/Lifecycle.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Navigation 3: back stack oddiy ro'yxat, NavKey'lar @Serializable (process death'da tiklanadi).
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.json)

    // Hilt — DI: @HiltAndroidApp ildiz komponenti shu modulda yaratiladi.
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    // hiltViewModel() — Compose ekranida Hilt ViewModel olish.
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    // Outbox worker'larini Hilt yaratishi uchun (App'dagi HiltWorkerFactory).
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.work.runtime.ktx)

    // Orbit — MVI (State + SideEffect; Contract/ViewModel uslubi).
    implementation(libs.orbit.core)
    implementation(libs.orbit.viewmodel)
    implementation(libs.orbit.compose)

    // Paging va Coil (rasm yuklash; App'dagi umumiy ImageLoader token bilan ishlaydi).
    implementation(libs.androidx.paging.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Android 12+ uslubidagi splash screen (eski versiyalarda ham bir xil).
    implementation(libs.androidx.core.splashscreen)

    // Firebase Cloud Messaging — push xabarlar.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // Testlar.
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    // Faqat debug: Android Studio preview va Layout Inspector.
    debugImplementation(libs.androidx.compose.ui.tooling)
}
