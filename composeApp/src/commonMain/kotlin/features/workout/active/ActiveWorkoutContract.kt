package features.workout.active

import core.mvi.UiEffect
import core.mvi.UiIntent
import core.mvi.UiState
import features.workout.domain.Exercise
import features.workout.domain.MuscleGroup
import features.workout.domain.WorkoutSession
import features.workout.domain.WorkoutSet

class ActiveWorkoutContract {
    data class State(
        val session: WorkoutSession? = null,
        val availableExercises: List<Exercise> = emptyList(),
        val workoutExercises: List<Exercise> = emptyList(),
        val sets: List<WorkoutSet> = emptyList(),
        val isPickerVisible: Boolean = false,
        val isRestTimerActive: Boolean = false,
        val restTimerRemainingSeconds: Int = 0,
        val restTimerTotalSeconds: Int = 90,
        val workoutElapsedSeconds: Long = 0L,
        val selectedStatsExercise: Exercise? = null,
        val exerciseStats: features.workout.domain.ExerciseStats? = null,
        val isLoading: Boolean = false,
        val error: String? = null
    ) : UiState

    sealed class Intent : UiIntent {
        data object LoadWorkout : Intent()
        data class OpenExercisePicker(val isOpen: Boolean) : Intent()
        data class OpenExerciseStats(val exercise: Exercise?) : Intent()
        data class AddExerciseToWorkout(val exercise: Exercise) : Intent()
        data class CreateCustomExercise(
            val name: String,
            val muscleGroup: MuscleGroup,
            val defaultRestSeconds: Int = 90
        ) : Intent()
        data class RemoveExerciseFromWorkout(val exerciseId: String) : Intent()
        data class AddSet(val exerciseId: String) : Intent()
        data class ToggleSetComplete(val set: WorkoutSet) : Intent()
        data class UpdateWeight(val set: WorkoutSet, val weightKg: Double) : Intent()
        data class UpdateReps(val set: WorkoutSet, val reps: Int) : Intent()
        data class UpdateRpe(val set: WorkoutSet, val rpe: Double?) : Intent()
        data class DeleteSet(val set: WorkoutSet) : Intent()
        data class StartRestTimer(val seconds: Int = 90) : Intent()
        data class AdjustRestTimer(val deltaSeconds: Int) : Intent()
        data object SkipRestTimer : Intent()
        data object FinishWorkout : Intent()
        data object CancelWorkout : Intent()
    }

    sealed class Effect : UiEffect {
        data class ShowSnackbar(val message: String) : Effect()
        data object WorkoutFinished : Effect()
        data object WorkoutCancelled : Effect()
    }
}
