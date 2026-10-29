plugins {
    id("contentapp.multiplatform")
}

kotlin {
    android { namespace = "com.yhdista.core.storage" }

    sourceSets {
        commonMain.dependencies {
            api(project(":foundation"))
            api(libs.androidx.room.runtime)
            api(libs.koin.core)
            implementation(libs.napier)
            implementation(libs.kotlinx.serialization.core)
        }
    }
}
