// Gradle plugin'lari: modul turi va kod generatsiya vositalari.
plugins {
    // Domain — toza Kotlin/JVM moduli, Android'ga bog'liq emas: use case, model va repository interfeyslari
    // framework'siz yoziladi va oddiy JUnit bilan tez test qilinadi. Feature'lar data'ga emas, faqat domain'ga
    // bog'lanadi (clean architecture).
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
    // `api`: AppResult/AppError domain'ning ochiq API'sida — domain'ni ulagan modul ularni ham ko'radi.
    api(project(":core:common"))

    // Paging common — Android'siz PagingData turi (toza Kotlin).
    api(libs.androidx.paging.common)
}
