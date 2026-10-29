plugins {
    id("contentapp.multiplatform")
}

kotlin {
    android { namespace = "com.yhdista.feature.exercises.impl" }

    sourceSets {
        commonMain.dependencies {
            api(project(":feature:exercises:api"))
            implementation(project(":core:storage"))
            implementation(libs.kotlinx.serialization.json)
            api(libs.androidx.room.runtime)
            api(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
