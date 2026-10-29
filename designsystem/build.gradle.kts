plugins {
    id("contentapp.multiplatform")
    id("contentapp.compose")
}

kotlin {
    android { namespace = "com.yhdista.designsystem" }

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            implementation(libs.compose.material3)
            api(libs.compose.ui)
            implementation(libs.compose.ui.tooling.preview)
        }
        val desktopTest by getting { dependencies { implementation(libs.compose.ui.test.junit4); implementation(libs.kotlin.test); implementation(libs.compose.desktop) } }
    }
}
