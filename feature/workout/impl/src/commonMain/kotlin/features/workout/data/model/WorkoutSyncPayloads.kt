package features.workout.data.model

import kotlinx.serialization.Serializable

@Serializable
internal data class ExerciseSyncPayload(
    val name: String,
    val muscleGroup: String,
    val defaultRestSeconds: Int
)

@Serializable
internal data class StartSessionSyncPayload(
    val title: String,
    val startTimeEpochMs: Long
)

@Serializable
internal data class InsertSetSyncPayload(
    val sessionId: String,
    val exerciseId: String,
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val setType: String
)

@Serializable
internal data class UpdateSetSyncPayload(
    val weightKg: Double,
    val reps: Int,
    val rpe: Double? = null,
    val isCompleted: Boolean,
    val isPersonalRecord: Boolean,
    val setType: String,
    val setNumber: Int
)
