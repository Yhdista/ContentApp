package features.workout.domain

import kotlinx.serialization.Serializable

@Serializable
data class WorkoutTemplate(
    val id: String,
    val name: String,
    val description: String = "",
    val exercises: List<WorkoutTemplateExercise> = emptyList()
)

@Serializable
data class WorkoutTemplateExercise(
    val exerciseId: String,
    val exerciseName: String,
    val targetSets: Int = 3,
    val defaultWeightKg: Double = 60.0,
    val defaultReps: Int = 10,
    val orderIndex: Int = 0
)
