package features.workout.domain

import kotlinx.serialization.Serializable

@Serializable
data class WorkoutSession(
    val id: String,
    val title: String,
    val startTimeEpochMs: Long,
    val endTimeEpochMs: Long? = null,
    val isCompleted: Boolean = false,
    val totalVolumeKg: Double = 0.0,
    val sourceRoutineId: String? = null,
    val sourceRoutineRevision: Long? = null,
    val sourcePlan: PlanWorkoutSource? = null
)
