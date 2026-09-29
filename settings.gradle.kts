// Gradle plugin'lari qaysi repozitoriylardan yuklanadi.
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
// Foojay — kerakli JDK toolchain'ni avtomatik yuklab oladi.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
// Kutubxonalar faqat shu repozitoriylardan; modullarda alohida repo e'lon qilish taqiqlangan (FAIL_ON_PROJECT_REPOS).
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Swift Chat"
// Modullar: app — hamma narsani yig'adi; domain — biznes qoidalar (toza Kotlin); data — tarmoq va baza;
// core:* — umumiy kod (common, designsystem, navigation); feature:* — ekranlar (bir-biriga bog'liq emas).
include(":app")
include(":domain")
include(":data")
include(":core:common")
include(":core:designsystem")
include(":core:navigation")
include(":feature:auth")
include(":feature:chats")
include(":feature:conversation")
include(":feature:group")
include(":feature:profile")
