import java.util.Properties

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
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release imzosi: keystore va parollar local.properties'dan (yoki CI'da xuddi shu nomli environment
    // o'zgaruvchilardan). Hech biri git'ga kirmaydi. Kalit topilmasa build yiqilmaydi — release imzosiz yig'iladi
    // (masalan, CI'da yoki boshqa kompyuterda); faqat imzolangan APK/AAB Play'ga yuklanadi.
    val releaseSigning = releaseSigningProperties()
    if (releaseSigning != null) {
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(releaseSigning.getValue("RELEASE_STORE_FILE"))
                storePassword = releaseSigning.getValue("RELEASE_STORE_PASSWORD")
                keyAlias = releaseSigning.getValue("RELEASE_KEY_ALIAS")
                keyPassword = releaseSigning.getValue("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        // Release'da kod qisqartirish/obfuskatsiya hozircha o'chiq.
        release {
            signingConfig = signingConfigs.findByName("release")
            optimization {
                enable = false
            }
            // Faqat haqiqiy telefonlar protsessorlari. ML Kit (qo'ng'iroqdagi orqa fon) va WebRTC native
            // kutubxonalari har bir ABI uchun ~20–35 MB — x86/x86_64 (faqat emulyatorlar) APK'ni ikki baravar
            // kattalashtirardi. Debug'da cheklov yo'q: emulyatorda ham ishlasin.
            ndk {
                abiFilters += listOf("arm64-v8a", "armeabi-v7a")
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
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
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
    // O'z "o'ngga surib orqaga" gesture'imiz tizimdagi predictive back bilan bir xil hodisalarni yuboradi.
    implementation(libs.androidx.navigationevent.compose)
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

    // Coil (rasm yuklash; App'dagi umumiy ImageLoader token bilan ishlaydi).
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Android 12+ uslubidagi splash screen (eski versiyalarda ham bir xil).
    implementation(libs.androidx.core.splashscreen)

    // Firebase Cloud Messaging — push xabarlar.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // Testlar.
    testImplementation(libs.junit)
    // Swipe-back gesture testi: Compose UI test Robolectric ustida (JVM).
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    // Faqat debug: Android Studio preview va Layout Inspector.
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// Ilova ichida til almashtiriladi (uz/ru/en). AAB'da Play tillarni telefon tiliga qarab kesib tashlasa, boshqa tilga
// o'tib bo'lmay qolardi — shuning uchun barcha tillar har bir o'rnatishda bo'ladi (matnlar hajmi juda kichik).
android {
    bundle {
        language {
            enableSplit = false
        }
    }
}

/**
 * Release imzolash uchun 4 ta qiymat: avval local.properties, bo'lmasa environment (CI uchun). Bittasi ham
 * yetishmasa yoki keystore fayli yo'q bo'lsa — `null` (imzosiz release).
 */
fun releaseSigningProperties(): Map<String, String>? {
    val keys = listOf("RELEASE_STORE_FILE", "RELEASE_STORE_PASSWORD", "RELEASE_KEY_ALIAS", "RELEASE_KEY_PASSWORD")
    val local = Properties().apply {
        rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
    }
    val values = keys.associateWith { key -> local.getProperty(key)?.takeIf { it.isNotBlank() } ?: System.getenv(key) }
    if (values.values.any { it.isNullOrBlank() }) return null
    if (!rootProject.file(values.getValue("RELEASE_STORE_FILE")!!).exists()) return null
    return values.mapValues { it.value!! }
}
