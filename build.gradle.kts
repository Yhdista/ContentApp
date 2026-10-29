import groovy.json.JsonOutput
import org.gradle.api.artifacts.ProjectDependency

plugins {
    base
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.devtools.ksp) apply false
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization) apply false
    alias(libs.plugins.kmp) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
}

abstract class ArchitectureInventory : DefaultTask() {
    @get:Input
    abstract val entries: ListProperty<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun write() {
        val output = outputFile.get().asFile
        output.parentFile.mkdirs()
        output.writeText("[" + entries.get().joinToString(",") + "]")
    }
}

val architectureInventory = tasks.register<ArchitectureInventory>("architectureInventory") {
    outputFile.set(layout.buildDirectory.file("architecture/dependencies.json"))
}

// Collect declared edges from actual Gradle configurations; no resolution or
// source-text assumptions about catalog aliases, formatting or convention deps.
gradle.projectsEvaluated {
    architectureInventory.configure {
        entries.set(allprojects.flatMap { module ->
            module.configurations.flatMap { configuration ->
                configuration.dependencies.map { dependency ->
                    JsonOutput.toJson(mapOf(
                        "module" to module.path,
                        "configuration" to configuration.name,
                        "target" to if (dependency is ProjectDependency) dependency.path
                            else "${dependency.group}:${dependency.name}"
                    ))
                }
            }
        }.distinct().sorted())
    }
}

val verifyArchitecture = tasks.register<Exec>("verifyArchitecture") {
    group = "verification"
    description = "Checks physical module, atomic UI and platform ownership boundaries."
    dependsOn(architectureInventory)
    workingDir(rootDir)
    commandLine("python3", "tools/architecture/verify.py", "--inventory", "build/architecture/dependencies.json")
}

val testArchitectureGuard = tasks.register<Exec>("testArchitectureGuard") {
    group = "verification"
    description = "Proves positive fixtures pass and every forbidden boundary fails."
    workingDir(rootDir)
    commandLine("python3", "-m", "unittest", "discover", "-s", "tools/architecture", "-p", "test_*.py", "-v")
}

tasks.named("check") { dependsOn(verifyArchitecture, testArchitectureGuard) }
subprojects {
    tasks.matching { it.name == "check" }.configureEach { dependsOn(verifyArchitecture) }
}
