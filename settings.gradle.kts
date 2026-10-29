pluginManagement {
    includeBuild("build-logic")
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

rootProject.name = "ContentApp"
include(":androidApp", ":composeApp", ":foundation", ":designsystem")

include(":core:storage", ":core:presentation", ":feature:exercises:api", ":feature:exercises:impl", ":feature:workout:api", ":feature:workout:impl")

include(":feature:routines:api", ":feature:routines:impl")

include(":feature:trainingplans:api", ":feature:trainingplans:impl")

// Compile-only external consumer: deliberately has no domain or Material dependency.
include(":verification:designsystem-consumer")
