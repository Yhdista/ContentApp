plugins {
    id("contentapp.compose")
    id("contentapp.multiplatform")
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization)
}

kotlin {
    android { namespace = "com.yhdista.feature.trainingplans.impl" }

    sourceSets {
        commonMain.dependencies {
            api(project(":feature:trainingplans:api"))
            implementation(project(":core:storage"))
            implementation(project(":core:presentation"))
            implementation(project(":designsystem"))
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.koin.compose)
            implementation(project(":feature:routines:api"))
            implementation(libs.kotlinx.serialization.json)
            api(libs.androidx.room.runtime)
            api(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
