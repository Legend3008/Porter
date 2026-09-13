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

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "PorterShipper"

include(":app")

// Core modules
include(":core:common")
include(":core:designsystem")
include(":core:ui")
include(":core:network")
include(":core:auth")
include(":core:database")
include(":core:notifications")
include(":core:permissions")

// Domain modules
include(":domain:model")
include(":domain:repository")
include(":domain:usecase")

// Data modules
include(":data:api")
include(":data:database")
include(":data:repository")

// Feature modules
include(":feature:auth")
include(":feature:home")
include(":feature:booking")
include(":feature:tracking")
include(":feature:documents")
include(":feature:payments")
include(":feature:notifications")
include(":feature:profile")
