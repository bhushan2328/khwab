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
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        // Unity library local JARs (unity-classes.jar in unityLibrary/libs/)
        flatDir {
            dirs("../khwab-aura-unity/export/unityLibrary/libs")
        }
    }
}

rootProject.name = "Khwab"

include(":app")
include(":core")
include(":integration")

project(":core").projectDir = file("../khwab-core/core")


project(":integration").projectDir = file("../khwab-integration/integration")

include(":unityLibrary")
project(":unityLibrary").projectDir = file("../khwab-aura-unity/export/unityLibrary")
