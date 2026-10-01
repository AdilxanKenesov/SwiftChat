// Gradle plugin'lari: modul turi va kod generatsiya vositalari.
plugins {
    // Android kutubxona moduli — app moduliga qo'shiladi.
    alias(libs.plugins.android.library)
    // Compose kompilyator plugin'i (@Composable funksiyalar uchun).
    alias(libs.plugins.kotlin.compose)
    // KSP — annotation processor: Hilt/Room kodini generatsiya qiladi (kapt'dan tezroq).
    alias(libs.plugins.ksp)
    // Hilt — dependency injection.
    alias(libs.plugins.hilt)
    // Screenshot testlari: `recordRoborazziDebug` (golden yozish) / `verifyRoborazziDebug` (solishtirish).
    alias(libs.plugins.roborazzi)
}

// Android sozlamalari: namespace, SDK versiyalari, Java versiyasi.
android {
    namespace = "uz.relay.feature.auth"
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
    // Loyiha modullari.
    implementation(project(":domain"))
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))

    implementation(libs.androidx.core.ktx)

    // Hilt — DI (ksp generatsiya qiladi).
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    // hiltViewModel() — Compose ekranida Hilt ViewModel olish.
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    // Orbit — MVI (State + SideEffect; Contract/ViewModel uslubi).
    implementation(libs.orbit.core)
    implementation(libs.orbit.viewmodel)
    implementation(libs.orbit.compose)

    // Faqat debug: Android Studio preview va Layout Inspector.
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Testlar.
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    // ViewModel testlari: Orbit test DSL va repository fake'lari (domain testFixtures).
    testImplementation(libs.orbit.test)
    testImplementation(testFixtures(project(":domain")))
    // Compose UI testlari: emulyatorsiz, Robolectric ustida (JVM).
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
