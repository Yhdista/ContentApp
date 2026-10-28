package features.routines.api

import features.workout.domain.WorkoutSetType
import kotlinx.serialization.Serializable

@Serializable
data class Routine(
    val id: String,
    val name: String,
    val notes: String = "",
    val revision: Long = 0,
    val archived: Boolean = false,
    val exercises: List<RoutineExercise>
)

@Serializable
data class RoutineExercise(
    val id: String,
    val exerciseId: String,
    val restSeconds: Int,
    val sets: List<RoutineSet>
)

@Serializable
data class RoutineSet(
    val id: String,
    val weightKg: Double,
    val reps: Int,
    val setType: WorkoutSetType = WorkoutSetType.NORMAL
)
