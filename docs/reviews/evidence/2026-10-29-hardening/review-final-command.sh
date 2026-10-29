#!/bin/sh
set -eu
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:/opt/homebrew/bin:/Users/yhdista/.local/bin:/usr/bin:/bin:/usr/sbin:/sbin"
cd /Users/yhdista/.yhdi-mesh/worktrees/contentapp-atomic-build
YHDISTA_FULL=1 /Users/yhdista/ai-toolbox/plugins/yhdi-dev/skills/yhdi-dev/scripts/gw \
 compileCommonMainKotlinMetadata compileKotlinDesktop compileTestKotlinDesktop compileAndroidMain compileAndroidHostTest \
 :androidApp:compileDebugKotlin :androidApp:compileDebugUnitTestKotlin :androidApp:compileDebugAndroidTestKotlin \
 :composeApp:compileKotlinIosArm64 :composeApp:compileKotlinIosSimulatorArm64 \
 :feature:workout:api:desktopTest :feature:workout:api:testAndroidHostTest \
 :feature:workout:impl:desktopTest :feature:workout:impl:testAndroidHostTest \
 :composeApp:desktopTest :designsystem:desktopTest :androidApp:testDebugUnitTest \
 verifyArchitecture testArchitectureGuard :verification:designsystem-consumer:compileKotlinDesktop \
 lintAndroidMain :androidApp:lintDebug :androidApp:assembleDebug --continue \
 > .yhdista/review-final-verify.log 2>&1
