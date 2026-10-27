package features.workout.domain

import kotlinx.serialization.Serializable

@Serializable
enum class WorkoutSetType(
    val code: String,
    val displayName: String,
    val shortLabel: String
) {
    NORMAL("1", "Normální", "N"),
    WARMUP("W", "Zahřívací", "W"),
    DROPSET("D", "Dropset", "D"),
    FAILURE("F", "Do selhání", "F")
}

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
    val isPersonalRecord: Boolean = false,
    val setType: WorkoutSetType = WorkoutSetType.NORMAL
)
