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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SwiftChat"
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
