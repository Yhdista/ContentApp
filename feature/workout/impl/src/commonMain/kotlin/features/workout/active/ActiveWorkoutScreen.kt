package features.workout.active

import ui.designsystem.atoms.*
import ui.designsystem.organisms.*
import ui.designsystem.templates.*
import ui.designsystem.nucleus.AppStyle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import features.workout.domain.WorkoutSet
import features.workout.ui.components.ExerciseCard
import features.workout.ui.components.ExercisePickerSheet
import features.workout.ui.components.ExerciseStatsSheet
import features.workout.ui.components.PlateCalculatorSheet
import features.workout.ui.components.RestTimerBar
import features.workout.ui.components.WorkoutThumbBar
import ui.designsystem.nucleus.AppSpacing
import ui.designsystem.atoms.feedback.ErrorNotice
import ui.designsystem.templates.UniversalScreenTemplate

@Composable
internal fun ActiveWorkoutScreen(
    state: ActiveWorkoutContract.State,
    onIntent: (ActiveWorkoutContract.Intent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHost: @Composable () -> Unit = {}
) {
    val colorScheme = AppStyle.colors
    var isPlateCalculatorOpen by remember { mutableStateOf(false) }
    var selectedSetForCalculator by remember { mutableStateOf<WorkoutSet?>(null) }

    val elapsedMinutes = (state.workoutElapsedSeconds / 60).toString().padStart(2, '0')
    val elapsedSeconds = (state.workoutElapsedSeconds % 60).toString().padStart(2, '0')
    val timerText = "$elapsedMinutes:$elapsedSeconds"

    UniversalScreenTemplate(
        snackbarHost = snackbarHost,
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = {
                    Column {
                        AppText(
                            text = state.session?.title ?: "Trénink",
                            style = AppStyle.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onBackground
                        )
                        AppText(
                            text = state.session?.let { if(it.sourcePlan != null) "🗓️ Trénink z plánu · revize ${it.sourcePlan?.planRevision}" else if(it.sourceRoutineId != null) "📋 Samostatná rutina · revize ${it.sourceRoutineRevision}" else "🏋️ Samostatný trénink" } ?: "Trénink",
                            style = AppStyle.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = AppSpacing.sm)
                    ) {
                        // Tlačítko kalkulátoru kotoučů
                        AppSurfaceAction(onClick = {
                                    selectedSetForCalculator = null
                                    isPlateCalculatorOpen = true
                                }, enabled = true,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colorScheme.surfaceVariant)
                                .padding(horizontal = AppSpacing.sm, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AppText(
                                text = "🏋️ Kotouče",
                                style = AppStyle.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                        }

                        // Čas tréninku
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colorScheme.surfaceVariant)
                                .padding(horizontal = AppSpacing.md, vertical = 6.dp)
                        ) {
                            AppText(
                                text = "⏱ $timerText",
                                style = AppStyle.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.primary
                            )
                        }
                    }
                },
            )
        },
        timerBar = {
            RestTimerBar(
                isVisible = state.isRestTimerActive,
                remainingSeconds = state.restTimerRemainingSeconds,
                totalSeconds = state.restTimerTotalSeconds,
                onAddSeconds = { onIntent(ActiveWorkoutContract.Intent.AdjustRestTimer(it)) },
                onSkip = { onIntent(ActiveWorkoutContract.Intent.SkipRestTimer) }
            )
        },
        thumbActionZone = {
            WorkoutThumbBar(
                enabled = !state.isSaving && !state.isTerminated,
                onAddExercise = { onIntent(ActiveWorkoutContract.Intent.OpenExercisePicker(true)) },
                onFinishWorkout = { onIntent(ActiveWorkoutContract.Intent.FinishWorkout) },
                onCancelWorkout = { onIntent(ActiveWorkoutContract.Intent.CancelWorkout) }
            )
        },
        content = { contentPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentPadding = PaddingValues(
                    horizontal = AppSpacing.lg,
                    vertical = AppSpacing.md
                ),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
            ) {
                state.error?.let { error ->
                    item { ErrorNotice(error.asString(), onRetry = { onIntent(ActiveWorkoutContract.Intent.LoadWorkout) }) }
                }
                if (state.workoutExercises.any { it.orderReconstructed }) {
                    item { AppText("Pořadí cviků bylo obnoveno ze staršího záznamu.", style = AppStyle.typography.bodySmall) }
                }
                if (state.workoutExercises.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(colorScheme.surface)
                                .padding(AppSpacing.xl),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                            ) {
                                AppText(
                                    text = "🏋️",
                                    fontSize = 40.sp
                                )
                                AppText(
                                    text = "Žádné cviky v tréninku",
                                    style = AppStyle.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface
                                )
                                AppText(
                                    text = "Klikněte na '+ Přidat cvik' níže a začněte cvičit.",
                                    style = AppStyle.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(state.workoutExercises, key = { it.id }) { exercise ->
                        val exerciseSets = state.sets.filter { (it.workoutExerciseId ?: it.exerciseId) == exercise.id }
                        ExerciseCard(
                            exercise = features.exercises.api.Exercise(exercise.exerciseId,exercise.name,exercise.muscleGroup,exercise.restSeconds),
                            sets = exerciseSets,
                            onAddSet = { onIntent(ActiveWorkoutContract.Intent.AddSet(exercise.id)) },
                            onToggleSetComplete = { set ->
                                onIntent(ActiveWorkoutContract.Intent.ToggleSetComplete(set))
                            },
                            onWeightChange = { set, newWeight ->
                                onIntent(ActiveWorkoutContract.Intent.UpdateWeight(set, newWeight))
                            },
                            onRepsChange = { set, newReps ->
                                onIntent(ActiveWorkoutContract.Intent.UpdateReps(set, newReps))
                            },
                            onRpeChange = { set, newRpe ->
                                onIntent(ActiveWorkoutContract.Intent.UpdateRpe(set, newRpe))
                            },
                            onDeleteSet = { set ->
                                onIntent(ActiveWorkoutContract.Intent.DeleteSet(set))
                            },
                            onRemoveExercise = {
                                onIntent(ActiveWorkoutContract.Intent.RemoveExerciseFromWorkout(exercise.id))
                            },
                            onOpenPlateCalculator = { set ->
                                selectedSetForCalculator = set
                                isPlateCalculatorOpen = true
                            },
                            onShowStats = {
                                onIntent(ActiveWorkoutContract.Intent.OpenExerciseStats(features.exercises.api.Exercise(exercise.exerciseId,exercise.name,exercise.muscleGroup,exercise.restSeconds)))
                            },
                            onSetTypeChange = { set, newType ->
                                onIntent(ActiveWorkoutContract.Intent.ChangeSetType(set, newType))
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(AppSpacing.xxl))
                }
            }

            // Bottom sheet pro statistiky a graf cviku
            if (state.selectedStatsExercise != null) {
                ExerciseStatsSheet(
                    exercise = state.selectedStatsExercise,
                    stats = state.exerciseStats,
                    onDismiss = { onIntent(ActiveWorkoutContract.Intent.OpenExerciseStats(null)) }
                )
            }

            // Bottom sheet pro výběr cviku
            ExercisePickerSheet(
                isVisible = state.isPickerVisible,
                exercises = state.availableExercises,
                onSelectExercise = { onIntent(ActiveWorkoutContract.Intent.AddExerciseToWorkout(it)) },
                onCreateCustomExercise = { name, group, rest ->
                    onIntent(ActiveWorkoutContract.Intent.CreateCustomExercise(name, group, rest))
                },
                onDismissRequest = { onIntent(ActiveWorkoutContract.Intent.OpenExercisePicker(false)) }
            )

            // Bottom sheet pro kalkulátor kotoučů
            PlateCalculatorSheet(
                isVisible = isPlateCalculatorOpen,
                initialWeightKg = selectedSetForCalculator?.weightKg ?: 60.0,
                onDismissRequest = {
                    isPlateCalculatorOpen = false
                    selectedSetForCalculator = null
                },
                onApplyWeight = selectedSetForCalculator?.let { targetSet ->
                    { weight ->
                        onIntent(ActiveWorkoutContract.Intent.UpdateWeight(targetSet, weight))
                    }
                }
            )
        }
    )
}
