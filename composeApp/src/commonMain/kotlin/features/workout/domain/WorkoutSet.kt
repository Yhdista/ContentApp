package features.workout.domain

import kotlinx.serialization.Serializable

@Serializable
data class WorkoutSet(
    val id: String,
    val sessionId: String,
    val exerciseId: String,
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val rpe: Double? = null,
    val isCompleted: Boolean = false,
    val previousPerformance: String? = null,
    val isPersonalRecord: Boolean = false
)
