// Gradle plugin'lari: modul turi va kod generatsiya vositalari.
plugins {
    // core:common — toza Kotlin/JVM moduli: hamma modul (domain ham) ishlatadigan umumiy turlar (AppResult, AppError,
    // AppDispatchers). Android'ga bog'liq emas, shuning uchun domain ham toza qoladi.
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

dependencies {
    // Coroutines va javax.inject (@Inject, @Qualifier) — Android'siz; `api` — ulagan modullarga ham ko'rinadi.
    api(libs.kotlinx.coroutines.core)
    api(libs.javax.inject)
    // Testlar.
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
