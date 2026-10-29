plugins {
    id("contentapp.multiplatform")
    id("contentapp.compose")
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization)
}

kotlin {
    android { namespace = "com.yhdista.feature.workout.impl" }

    sourceSets {
        commonMain.dependencies {
            api(project(":feature:workout:api"))
            implementation(project(":feature:exercises:api"))
            implementation(project(":core:storage"))
            implementation(project(":core:presentation"))
            implementation(project(":designsystem"))
            api(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.androidx.lifecycle.runtime.compose)
            api(libs.compose.ui)
            api(libs.androidx.room.runtime)
            api(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.napier)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.androidx.lifecycle.viewmodel)
        }
        val desktopTest by getting { dependencies { implementation(libs.compose.ui.test.junit4); implementation(libs.kotlin.test); implementation(libs.compose.desktop) } }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
