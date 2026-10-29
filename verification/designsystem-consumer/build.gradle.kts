plugins {
    id("contentapp.desktop-consumer")
    id("contentapp.compose")
}
kotlin {
    sourceSets.commonMain.dependencies { implementation(project(":designsystem")) }
}
