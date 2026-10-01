// Gradle plugin'lari: modul turi va kod generatsiya vositalari.
plugins {
    // Domain — toza Kotlin/JVM moduli, Android'ga bog'liq emas: use case, model va repository interfeyslari
    // framework'siz yoziladi va oddiy JUnit bilan tez test qilinadi. Feature'lar data'ga emas, faqat domain'ga
    // bog'lanadi (clean architecture).
    id("java-library")
    // Repository fake'lari (src/testFixtures) — feature modullar testlarida `testFixtures(project(":domain"))` bilan.
    `java-test-fixtures`
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

    // Sof Kotlin modul — testlar oddiy JUnit bilan, Android'siz va tez ishlaydi.
    testImplementation(libs.junit)
    // Fake'lar va MainDispatcherRule uchun (testFixtures'ni ulagan modul testlariga ham o'tadi).
    testFixturesApi(libs.junit)
    testFixturesApi(libs.kotlinx.coroutines.test)
}
