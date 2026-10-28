#!/usr/bin/env python3
"""Reproduce four audit probes using already-built desktop production artifacts.
No source tree edits or downloads. Requires the baseline desktop jars and cached Kotlin 2.2.10 compiler.
Usage: python3 contentapp-run-architecture-probes.py /path/to/ContentApp [--skip-classpath]
Keep this script next to ContentAppArchitectureProbe.kt.
"""
from pathlib import Path
import subprocess
import sys

root = Path(sys.argv[1]).resolve()
artifacts = Path(__file__).resolve().parent
cpfile = artifacts / "contentapp-audit-classpath.txt"
init = artifacts / "contentapp-audit-classpath.gradle"
init.write_text('''gradle.projectsEvaluated {
    def app = gradle.rootProject.project(":composeApp")
    app.tasks.register("auditClasspath") {
        doLast {
            def t = app.tasks.named("desktopTest").get()
            new File(System.getProperty("audit.classpath.output")).text = t.classpath.asPath
        }
    }
}
''')
if "--skip-classpath" not in sys.argv:
    with (artifacts / "contentapp-audit-gradle-help.log").open("w") as log:
        subprocess.run([
            str(root / "gradlew"), "-I", str(init), ":composeApp:auditClasspath",
            "--no-configuration-cache", "--console=plain", "--offline",
            "-Daudit.classpath.output=" + str(cpfile),
        ], cwd=root, stdout=log, stderr=subprocess.STDOUT, check=True)

cp = cpfile.read_text()
cache = Path.home() / ".gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin"
def jar(module):
    return next((cache / module / "2.2.10").glob("*/" + module + "-2.2.10.jar"))
compiler_cp = ":".join([str(jar("kotlin-compiler-embeddable")), str(jar("kotlin-script-runtime")), cp])
friend = next(p for p in cp.split(":") if "/feature/workout/impl/build/libs/" in p)
classes = artifacts / "contentapp-audit-probe-classes"
with (artifacts / "contentapp-astra-probe-compile.log").open("w") as log:
    subprocess.run([
        "java", "-cp", compiler_cp, "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
        "-no-stdlib", "-no-reflect", "-Xfriend-paths=" + friend,
        "-classpath", cp, "-d", str(classes), str(artifacts / "ContentAppArchitectureProbe.kt"),
    ], cwd=root, stdout=log, stderr=subprocess.STDOUT, check=True)
result = subprocess.run([
    "java", "-cp", str(classes) + ":" + cp, "ContentAppArchitectureProbeKt",
], cwd=root, capture_output=True, text=True, timeout=60)
(artifacts / "contentapp-astra-runtime-probe-2026-10-29.log").write_text(result.stdout + result.stderr)
print(result.stdout, end="")
if result.returncode:
    print(result.stderr, file=sys.stderr)
result.check_returncode()
