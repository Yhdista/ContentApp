plugins {
    alias(libs.plugins.kmp)
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlin.compose)
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
            api(project(":foundation"))
            api(project(":core:storage"))
            api(libs.androidx.lifecycle.viewmodel)
            api(compose.runtime)
            api("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
            api(libs.koin.core)
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
            implementation(libs.koin.compose)
            implementation(libs.napier)
        }
        androidMain.dependencies { implementation(libs.androidx.activity.compose) }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "com.yhdista.core.presentation"
    compileSdk = 37
    defaultConfig { minSdk = 27 }
}
