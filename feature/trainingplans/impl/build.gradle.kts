plugins {
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kmp)
    alias(libs.plugins.android.library)
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
            api(project(":feature:trainingplans:api"))
            implementation(project(":core:storage"))
            implementation(project(":core:presentation"))
            implementation(project(":designsystem"))
            implementation(compose.foundation)
            implementation(compose.components.uiToolingPreview)
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
            implementation(libs.koin.compose)
            implementation(project(":feature:routines:api"))
            implementation(libs.kotlinx.serialization.json)
            api(libs.androidx.room.runtime)
            api(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "com.yhdista.feature.trainingplans.impl"
    compileSdk = 37
    defaultConfig { minSdk = 27 }
}
