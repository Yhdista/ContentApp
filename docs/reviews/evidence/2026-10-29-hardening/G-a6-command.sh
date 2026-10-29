#!/bin/sh
set -eu
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:/opt/homebrew/bin:/Users/yhdista/.local/bin:/usr/bin:/bin:/usr/sbin:/sbin"
cd /Users/yhdista/.yhdi-mesh/worktrees/contentapp-atomic-build
YHDISTA_FULL=1 /Users/yhdista/ai-toolbox/plugins/yhdi-dev/skills/yhdi-dev/scripts/gw \
 compileCommonMainKotlinMetadata compileKotlinDesktop compileTestKotlinDesktop compileAndroidMain compileAndroidHostTest \
 :androidApp:compileDebugKotlin :androidApp:compileDebugUnitTestKotlin :androidApp:compileDebugAndroidTestKotlin \
 :composeApp:compileKotlinIosSimulatorArm64 \
 :designsystem:desktopTest \
 :feature:workout:impl:desktopTest --tests '*SetRowAccessibilityTest*' \
 :composeApp:desktopTest --tests 'app.navigation.*' \
 verifyArchitecture testArchitectureGuard :verification:designsystem-consumer:compileKotlinDesktop \
 lintAndroidMain :androidApp:lintDebug :androidApp:assembleDebug --continue \
 > .yhdista/G-a6.log 2>&1
