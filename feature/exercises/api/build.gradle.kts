plugins {
    id("contentapp.multiplatform")
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization)
}

kotlin {
    android { namespace = "com.yhdista.feature.exercises.api" }

    sourceSets {
        commonMain.dependencies {
            api(project(":foundation"))
            api(libs.kotlinx.serialization.core)
        }
    }
}
