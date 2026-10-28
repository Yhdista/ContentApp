#!/usr/bin/env python3
"""Reproduce the baseline public dependency gap without editing production sources."""
from pathlib import Path
import subprocess
import sys
import tempfile

root = Path(sys.argv[1]).resolve()
work = Path(tempfile.mkdtemp(prefix="contentapp-consumer-audit-"))
source = work / "src/commonMain/kotlin/ConsumeDesignSystem.kt"
source.parent.mkdir(parents=True)
source.write_text("""import androidx.compose.runtime.Composable
import ui.designsystem.atoms.feedback.StatusBadge
import core.model.ItemSyncStatus
@Composable fun ConsumerBadge() { StatusBadge(ItemSyncStatus.LOCAL) }
""")
build = work / "build.gradle.kts"
baseline = """plugins {
    alias(libs.plugins.kmp)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlin.compose)
}
kotlin {
    jvm("desktop")
    sourceSets.commonMain.dependencies { implementation(project(":designsystem")) }
}
"""
build.write_text(baseline)
init = work / "consumer.init.gradle"
quoted_path = "'" + str(work).replace("\\", "\\\\").replace("'", "\\'") + "'"
init.write_text("settingsEvaluated { settings ->\n"
                "    settings.include(':auditConsumer')\n"
                "    settings.project(':auditConsumer').projectDir = new File(" + quoted_path + ")\n"
                "}\n")
command = [str(root / "gradlew"), "-I", str(init), ":auditConsumer:compileKotlinDesktop",
           "--no-configuration-cache", "--console=plain", "--offline"]
red = subprocess.run(command, cwd=root, capture_output=True, text=True)
red_output = red.stdout + red.stderr
(work / "red.log").write_text(red_output)
assert red.returncode != 0 and "Unresolved reference 'ItemSyncStatus'" in red_output, (
    "Expected the baseline dependency failure; inspect " + str(work / "red.log"))
build.write_text(baseline.replace('implementation(project(":designsystem"))',
    'implementation(project(":designsystem")); implementation(project(":foundation"))'))
green = subprocess.run(command, cwd=root, capture_output=True, text=True)
(work / "green.log").write_text(green.stdout + green.stderr)
assert green.returncode == 0, "Control compilation failed; inspect " + str(work / "green.log")
print("PROVEN: designsystem-only consumer fails; explicit foundation control compiles.")
print("Inputs and logs:", work)
