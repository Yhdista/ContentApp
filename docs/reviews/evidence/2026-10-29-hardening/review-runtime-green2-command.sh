#!/bin/sh
set -eu
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:/opt/homebrew/bin:/Users/yhdista/.local/bin:/usr/bin:/bin:/usr/sbin:/sbin"
cd /Users/yhdista/.yhdi-mesh/worktrees/contentapp-atomic-build
YHDISTA_FULL=1 /Users/yhdista/ai-toolbox/plugins/yhdi-dev/skills/yhdi-dev/scripts/gw \
 compileCommonMainKotlinMetadata compileKotlinDesktop compileTestKotlinDesktop compileAndroidMain compileAndroidHostTest \
 :androidApp:compileDebugKotlin :androidApp:compileDebugUnitTestKotlin :androidApp:compileDebugAndroidTestKotlin \
 :verification:designsystem-consumer:compileKotlinDesktop \
 :feature:workout:impl:desktopTest --tests '*ActiveWorkoutViewModelTest.successfulFinishIgnoresLaterTerminalIntentsBeforeEffectConsumption' --tests '*ActiveWorkoutViewModelTest.successfulCancelIgnoresLaterTerminalIntentsBeforeEffectConsumption' --tests '*ActiveWorkoutViewModelTest.failedFinishAllowsRetryUntilFirstSuccess' --tests '*ActiveWorkoutViewModelTest.failedCancelAllowsRetryUntilFirstSuccess' --tests '*ActiveWorkoutViewModelTest.observerFailureCannotUnlockPendingFinish' \
 --continue > .yhdista/review-runtime-green2.log 2>&1
