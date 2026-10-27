package features.workout.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ExerciseSyncPayload(
    val name: String,
    val muscleGroup: String,
    val defaultRestSeconds: Int
)

@Serializable
data class StartSessionSyncPayload(
    val title: String,
    val startTimeEpochMs: Long
)

@Serializable
data class CompleteSessionSyncPayload(
    val isCompleted: Boolean,
    val endTimeEpochMs: Long,
    val totalVolumeKg: Double
)

@Serializable
data class InsertSetSyncPayload(
    val sessionId: String,
    val exerciseId: String,
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val setType: String
)

@Serializable
data class UpdateSetSyncPayload(
    val weightKg: Double,
    val reps: Int,
    val rpe: Double? = null,
    val isCompleted: Boolean,
    val isPersonalRecord: Boolean,
    val setType: String
)

@Serializable
data class CreateTemplateSyncPayload(
    val name: String,
    val exercisesCount: Int
)
