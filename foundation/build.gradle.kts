plugins {
    id("contentapp.multiplatform")
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization)
}

kotlin {
    android { namespace = "com.yhdista.foundation" }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
