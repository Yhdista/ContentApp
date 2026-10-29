#!/bin/sh
set -eu
cd /Users/yhdista/AndroidApps/ContentApp
/Users/yhdista/.codex/plugins/cache/ai-toolbox/yhdi-dev/0.4.3/skills/yhdi-dev/scripts/gw \
  :feature:workout:impl:desktopTest \
  --tests '*ActiveWorkoutViewModelTest.observerFailureCannotUnlockPendingFinish*' \
  --tests '*ActiveWorkoutViewModelTest.addingExerciseUsesOneAtomicCommand*' \
  --tests '*ActiveWorkoutViewModelTest.failedFinishKeepsSessionAndExposesRecoverableError*' \
  --tests '*ActiveWorkoutViewModelTest.mutatingIntentsAreIgnoredWhileFinishIsPending*' \
  :feature:routines:impl:desktopTest \
  --tests '*RoutinePresentationTest.observerFailureCannotUnlockPendingDuplicate*' \
  --tests '*RoutinePresentationTest.failedDuplicateReleasesOnlyItsOwnLockAndAllowsRetry*' \
  :feature:trainingplans:impl:desktopTest \
  --tests '*PlanPresentationTest.observerFailureCannotUnlockPendingDuplicate*' \
  --tests '*PlanPresentationTest.failedDuplicateReleasesOnlyItsOwnLockAndAllowsRetry*' \
  :composeApp:desktopTest \
  --tests '*WorkoutPersistenceTest.addingExerciseRollsBackEntryWhenInitialSetOutboxFails*' \
  --tests '*WorkoutPersistenceTest.acceptedExerciseAndInitialSetSurviveCancellationAndKeepOccurrencesIndependent*' \
  --tests '*WorkoutPersistenceTest.previousPerformance*' \
  --tests '*TrainingWorkflowTest.repositoryCannotBypassApplicationCompletionWorkflow*' \
  --tests '*TrainingWorkflowTest.completionAdvancesExactlyOnceAndCancellationDoesNot*' \
  --tests '*TrainingWorkflowTest.progressFailureAndOutboxFailureAbortEarlierCompletionWrites*' \
  --tests '*TrainingWorkflowTest.prescriptionStartRetainsPreviousCompletedPerformance*'
