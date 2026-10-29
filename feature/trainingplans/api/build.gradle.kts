plugins {
    id("contentapp.multiplatform")
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization)
}

kotlin {
    android { namespace = "com.yhdista.feature.trainingplans.api" }

    sourceSets {
        commonMain.dependencies {
            api(project(":foundation"))
            api(project(":feature:workout:api"))
            api(libs.kotlinx.serialization.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
