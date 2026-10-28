package features.workout.active

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ActiveWorkoutScreen(
    state: ActiveWorkoutContract.State,
    onIntent: (ActiveWorkoutContract.Intent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHost: @Composable () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    var isPlateCalculatorOpen by remember { mutableStateOf(false) }
    var selectedSetForCalculator by remember { mutableStateOf<WorkoutSet?>(null) }

    val elapsedMinutes = (state.workoutElapsedSeconds / 60).toString().padStart(2, '0')
    val elapsedSeconds = (state.workoutElapsedSeconds % 60).toString().padStart(2, '0')
    val timerText = "$elapsedMinutes:$elapsedSeconds"

    UniversalScreenTemplate(
        snackbarHost = snackbarHost,
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.session?.title ?: "Trénink",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onBackground
                        )
                        Text(
                            text = state.session?.let { if(it.sourcePlan != null) "🗓️ Trénink z plánu · revize ${it.sourcePlan?.planRevision}" else if(it.sourceRoutineId != null) "📋 Samostatná rutina · revize ${it.sourceRoutineRevision}" else "🏋️ Samostatný trénink" } ?: "Trénink",
                            style = MaterialTheme.typography.labelSmall,
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
                        Box(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colorScheme.surfaceVariant)
                                .clickable(role = Role.Button) {
                                    selectedSetForCalculator = null
                                    isPlateCalculatorOpen = true
                                }
                                .padding(horizontal = AppSpacing.sm, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🏋️ Kotouče",
                                style = MaterialTheme.typography.labelSmall,
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
                            Text(
                                text = "⏱ $timerText",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.background
                )
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
                enabled = !state.isSaving,
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
                    item { Text("Pořadí cviků bylo obnoveno ze staršího záznamu.", style = MaterialTheme.typography.bodySmall) }
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
                                Text(
                                    text = "🏋️",
                                    fontSize = 40.sp
                                )
                                Text(
                                    text = "Žádné cviky v tréninku",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "Klikněte na '+ Přidat cvik' níže a začněte cvičit.",
                                    style = MaterialTheme.typography.bodySmall,
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
