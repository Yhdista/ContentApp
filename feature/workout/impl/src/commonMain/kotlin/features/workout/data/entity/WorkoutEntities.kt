package features.workout.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val startTimeEpochMs: Long,
    val endTimeEpochMs: Long?,
    val isCompleted: Boolean,
    val totalVolumeKg: Double,
    val sourceRoutineId: String? = null,
    val sourceRoutineRevision: Long? = null,
    val sourcePlanId: String? = null,
    val sourcePlanRevision: Long? = null,
    val sourceSlotId: String? = null,
    val sourceProgressRevision: Long? = null
)

@Entity(tableName = "workout_sets")
data class WorkoutSetEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val exerciseId: String,
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val rpe: Double?,
    val isCompleted: Boolean,
    val previousPerformance: String?,
    val isPersonalRecord: Boolean = false,
    val setType: String = "NORMAL",
    val workoutExerciseId: String? = null,
    val prescribedWeightKg: Double? = null,
    val prescribedReps: Int? = null,
    val prescribedSetType: String? = null
)

@Entity(tableName = "workout_exercises")
data class WorkoutExerciseEntity(
    @PrimaryKey val id: String, val sessionId: String, val exerciseId: String,
    val name: String, val muscleGroup: String, val restSeconds: Int,
    val orderIndex: Int, val orderReconstructed: Boolean = false
)
