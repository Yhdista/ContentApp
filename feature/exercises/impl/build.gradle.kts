plugins {
    alias(libs.plugins.kmp)
    alias(libs.plugins.android.library)
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
            api(project(":feature:exercises:api"))
            implementation(project(":core:storage"))
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
    namespace = "com.yhdista.feature.exercises.impl"
    compileSdk = 37
    defaultConfig { minSdk = 27 }
}
