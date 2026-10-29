plugins {
    alias(libs.plugins.kmp)
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization)
}

kotlin {
    androidTarget()
    jvm("desktop")
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            api(project(":feature:workout:api"))
            implementation(project(":feature:exercises:api"))
            implementation(project(":core:storage"))
            implementation(project(":core:presentation"))
            implementation(project(":designsystem"))
            api(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.components.uiToolingPreview)
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
            api(compose.ui)
            api(libs.androidx.room.runtime)
            api(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.napier)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.androidx.lifecycle.viewmodel)
        }
        val desktopTest by getting { dependencies { implementation(compose.desktop.uiTestJUnit4); implementation(kotlin("test")); implementation(compose.desktop.currentOs) } }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "com.yhdista.feature.workout.impl"
    compileSdk = 37
    defaultConfig { minSdk = 27 }
}
