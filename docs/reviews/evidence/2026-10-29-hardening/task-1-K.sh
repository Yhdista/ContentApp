#!/bin/sh
set -eu
cd /Users/yhdista/AndroidApps/ContentApp
/Users/yhdista/.codex/plugins/cache/ai-toolbox/yhdi-dev/0.4.3/skills/yhdi-dev/scripts/gw \
  :feature:workout:api:compileCommonMainKotlinMetadata \
  :feature:workout:impl:compileCommonMainKotlinMetadata \
  :feature:routines:api:compileCommonMainKotlinMetadata \
  :feature:routines:impl:compileCommonMainKotlinMetadata \
  :feature:trainingplans:api:compileCommonMainKotlinMetadata \
  :feature:trainingplans:impl:compileCommonMainKotlinMetadata \
  :composeApp:compileCommonMainKotlinMetadata \
  :feature:workout:api:compileTestKotlinDesktop \
  :feature:workout:impl:compileTestKotlinDesktop \
  :feature:routines:api:compileTestKotlinDesktop \
  :feature:routines:impl:compileTestKotlinDesktop \
  :feature:trainingplans:api:compileTestKotlinDesktop \
  :feature:trainingplans:impl:compileTestKotlinDesktop \
  :composeApp:compileTestKotlinDesktop \
  :feature:workout:api:compileDebugUnitTestKotlinAndroid \
  :feature:workout:impl:compileDebugUnitTestKotlinAndroid \
  :feature:routines:api:compileDebugUnitTestKotlinAndroid \
  :feature:routines:impl:compileDebugUnitTestKotlinAndroid \
  :feature:trainingplans:api:compileDebugUnitTestKotlinAndroid \
  :feature:trainingplans:impl:compileDebugUnitTestKotlinAndroid \
  :composeApp:compileDebugUnitTestKotlinAndroid \
  :composeApp:compileKotlinDesktop \
  :composeApp:compileDebugKotlinAndroid
