package features.workout.active

import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import features.workout.ui.components.ExerciseCard
import features.workout.ui.components.ExercisePickerSheet
import features.workout.ui.components.RestTimerBar
import features.workout.ui.components.WorkoutThumbBar
import ui.designsystem.nucleus.AppColors
import ui.designsystem.nucleus.AppSpacing
import ui.designsystem.templates.UniversalScreenTemplate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    state: ActiveWorkoutContract.State,
    onIntent: (ActiveWorkoutContract.Intent) -> Unit,
    modifier: Modifier = Modifier
) {
    val elapsedMinutes = (state.workoutElapsedSeconds / 60).toString().padStart(2, '0')
    val elapsedSeconds = (state.workoutElapsedSeconds % 60).toString().padStart(2, '0')
    val timerText = "$elapsedMinutes:$elapsedSeconds"

    UniversalScreenTemplate(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(end = AppSpacing.lg),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = state.session?.title ?: "Trénink",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.OnBackground
                            )
                            Text(
                                text = "Aktivní sezení (Hevy styl)",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.OnSurfaceVariant
                            )
                        }

                        // Čas tréninku
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AppColors.SurfaceVariant)
                                .padding(horizontal = AppSpacing.md, vertical = 6.dp)
                        ) {
                            Text(
                                text = "⏱ $timerText",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.Primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Background
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
                if (state.workoutExercises.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(AppColors.Surface)
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
                                    color = AppColors.OnSurface
                                )
                                Text(
                                    text = "Klikněte na '+ Přidat cvik' níže a začněte cvičit.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.OnSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(state.workoutExercises, key = { it.id }) { exercise ->
                        val exerciseSets = state.sets.filter { it.exerciseId == exercise.id }
                        ExerciseCard(
                            exercise = exercise,
                            sets = exerciseSets,
                            onAddSet = { onIntent(ActiveWorkoutContract.Intent.AddSet(exercise.id)) },
                            onToggleSetComplete = { set -> onIntent(ActiveWorkoutContract.Intent.ToggleSetComplete(set)) },
                            onWeightChange = { set, weight -> onIntent(ActiveWorkoutContract.Intent.UpdateWeight(set, weight)) },
                            onRepsChange = { set, reps -> onIntent(ActiveWorkoutContract.Intent.UpdateReps(set, reps)) },
                            onRpeChange = { set, rpe -> onIntent(ActiveWorkoutContract.Intent.UpdateRpe(set, rpe)) },
                            onDeleteSet = { set -> onIntent(ActiveWorkoutContract.Intent.DeleteSet(set)) },
                            onRemoveExercise = { onIntent(ActiveWorkoutContract.Intent.RemoveExerciseFromWorkout(exercise.id)) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(AppSpacing.xxl))
                }
            }

            // Bottom sheet pro výběr cviku
            ExercisePickerSheet(
                isVisible = state.isPickerVisible,
                exercises = state.availableExercises,
                onSelectExercise = { onIntent(ActiveWorkoutContract.Intent.AddExerciseToWorkout(it)) },
                onDismissRequest = { onIntent(ActiveWorkoutContract.Intent.OpenExercisePicker(false)) }
            )
        }
    )
}
