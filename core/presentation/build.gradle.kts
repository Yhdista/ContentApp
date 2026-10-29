plugins {
    id("contentapp.multiplatform")
    id("contentapp.compose")
}

kotlin {
    android { namespace = "com.yhdista.core.presentation" }

    sourceSets {
        commonMain.dependencies {
            api(project(":foundation"))
            api(project(":core:storage"))
            api(libs.androidx.lifecycle.viewmodel)
            api(libs.compose.runtime)
            api(libs.androidx.lifecycle.viewmodel.compose)
            api(libs.koin.core)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.koin.compose)
            implementation(libs.napier)
        }
        androidMain.dependencies { implementation(libs.androidx.activity.compose) }
    }
}
