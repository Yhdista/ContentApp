package features.workout.domain

import features.exercises.api.MuscleGroup
import kotlinx.serialization.Serializable

/** Original target, inserted once at start. Its ID matches the initial actual set ID even after deletion.
 * Order is original (exercise zero-based, set one-based). Source IDs identify the routine children.
 * No target exists for manual work or legacy sessions whose original prescription is unknown.
 */
@Serializable
data class WorkoutPrescriptionSet(
    val id: String,
    val sessionId: String,
    val workoutExerciseId: String,
    val exerciseId: String,
    val name: String,
    val muscleGroup: MuscleGroup,
    val restSeconds: Int,
    val exerciseOrder: Int,
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val setType: WorkoutSetType,
    val sourceRoutineExerciseId: String,
    val sourceRoutineSetId: String?
)
