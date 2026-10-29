#!/bin/sh
set -eu
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:/opt/homebrew/bin:/Users/yhdista/.local/bin:/usr/bin:/bin:/usr/sbin:/sbin"
cd /Users/yhdista/.yhdi-mesh/worktrees/contentapp-atomic-build
YHDISTA_FULL=1 /Users/yhdista/ai-toolbox/plugins/yhdi-dev/skills/yhdi-dev/scripts/gw \
 :feature:workout:impl:desktopTest --tests '*WorkoutDetailAccessibilityTest.featureBadgePairsMeetContrastOnActualDetailLayers' --tests '*WorkoutDetailAccessibilityTest.doubleDigitBadgeFitsActualDetailAtDoubleFontScale' \
 :designsystem:desktopTest --tests '*DesignSystemAccessibilityTest.publicControlsRemainOperableAtLargeFont' \
 --continue > .yhdista/review-ui-red.log 2>&1
