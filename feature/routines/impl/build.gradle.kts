plugins {
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
            api(project(":feature:routines:api"))
            implementation(project(":core:storage"))
            implementation(project(":feature:exercises:api"))
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
    namespace = "com.yhdista.feature.routines.impl"
    compileSdk = 37
    defaultConfig { minSdk = 27 }
}
