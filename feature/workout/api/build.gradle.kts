plugins {
    id("contentapp.multiplatform")
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization)
}

kotlin {
    android { namespace = "com.yhdista.feature.workout.api" }

    sourceSets {
        commonMain.dependencies {
            api(project(":foundation"))
            api(project(":feature:exercises:api"))
            api(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
