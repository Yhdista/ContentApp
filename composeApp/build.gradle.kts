plugins {
    id("contentapp.multiplatform")
    id("contentapp.compose")
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization)
}

kotlin {
    android { namespace = "com.yhdista.shared" }
    
    
    
    
    sourceSets {
        commonMain.dependencies {
            implementation(project(":foundation"))
            implementation(project(":designsystem"))
            implementation(project(":core:storage"))
            implementation(project(":core:presentation"))
            implementation(project(":feature:exercises:api"))
            implementation(project(":feature:exercises:impl"))
            implementation(project(":feature:workout:api"))
            implementation(project(":feature:workout:impl"))
            implementation(project(":feature:routines:api"))
            implementation(project(":feature:routines:impl"))
            implementation(project(":feature:trainingplans:api"))
            implementation(project(":feature:trainingplans:impl"))
            implementation(libs.jetbrains.navigation.compose)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.resources)
            implementation(libs.compose.ui.tooling.preview)
            
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.core)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.napier)
            
            implementation(libs.androidx.lifecycle.viewmodel)
            
            // Room & SQLite KMP
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
        }
        
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)

            implementation(libs.koin.android)
            
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            
            implementation(libs.compose.ui.tooling.preview)

        }
        

        val desktopMain by getting {
            dependencies {
                implementation(libs.compose.desktop)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.androidx.sqlite.bundled)

            }
        }

        val desktopTest by getting { dependencies { implementation(libs.compose.ui.test.junit4) } }


        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg)
            packageName = "com.yhdista.contentapp"
            packageVersion = "1.0.0"
        }
    }
}

dependencies {
    "kspAndroid"(libs.androidx.room.compiler)
    "kspDesktop"(libs.androidx.room.compiler)
    "kspIosSimulatorArm64"(libs.androidx.room.compiler)
    "kspIosArm64"(libs.androidx.room.compiler)
}

// Versioned schemas are reviewable artifacts; missing migrations must never erase local data.
ksp {
    arg("room.schemaLocation", projectDir.resolve("schemas").absolutePath)
}
