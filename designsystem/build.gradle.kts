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
            api(compose.runtime)
            api(compose.foundation)
            implementation(compose.material3)
            api(compose.ui)
            implementation(compose.components.uiToolingPreview)
        }
        val desktopTest by getting { dependencies { implementation(compose.desktop.uiTestJUnit4); implementation(kotlin("test")); implementation(compose.desktop.currentOs) } }
    }
}

android {
    namespace = "com.yhdista.designsystem"
    compileSdk = 37
    defaultConfig { minSdk = 27 }
}
