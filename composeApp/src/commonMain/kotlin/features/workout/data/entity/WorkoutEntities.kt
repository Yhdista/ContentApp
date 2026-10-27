package features.workout.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val muscleGroup: String,
    val defaultRestSeconds: Int
)

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val startTimeEpochMs: Long,
    val endTimeEpochMs: Long?,
    val isCompleted: Boolean,
    val totalVolumeKg: Double
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
    val setType: String = "NORMAL"
)

@Entity(tableName = "workout_templates")
data class WorkoutTemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String
)

@Entity(
    tableName = "workout_template_exercises",
    primaryKeys = ["templateId", "exerciseId", "orderIndex"]
)
data class WorkoutTemplateExerciseEntity(
    val templateId: String,
    val exerciseId: String,
    val exerciseName: String,
    val targetSets: Int,
    val defaultWeightKg: Double,
    val defaultReps: Int,
    val orderIndex: Int
)
