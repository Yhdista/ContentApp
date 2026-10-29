plugins {
    alias(libs.plugins.kmp)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlin.compose)
}
kotlin {
    jvm("desktop")
    sourceSets.commonMain.dependencies { implementation(project(":designsystem")) }
}
