package features.workout.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import features.workout.domain.Exercise
import features.workout.domain.MuscleGroup
import features.workout.domain.WorkoutSession
import features.workout.domain.WorkoutSet

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val muscleGroup: String,
    val defaultRestSeconds: Int
) {
    fun toDomain(): Exercise = Exercise(
        id = id,
        name = name,
        muscleGroup = runCatching { MuscleGroup.valueOf(muscleGroup) }.getOrDefault(MuscleGroup.CHEST),
        defaultRestSeconds = defaultRestSeconds
    )

    companion object {
        fun fromDomain(exercise: Exercise): ExerciseEntity = ExerciseEntity(
            id = exercise.id,
            name = exercise.name,
            muscleGroup = exercise.muscleGroup.name,
            defaultRestSeconds = exercise.defaultRestSeconds
        )
    }
}

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val startTimeEpochMs: Long,
    val endTimeEpochMs: Long?,
    val isCompleted: Boolean,
    val totalVolumeKg: Double
) {
    fun toDomain(): WorkoutSession = WorkoutSession(
        id = id,
        title = title,
        startTimeEpochMs = startTimeEpochMs,
        endTimeEpochMs = endTimeEpochMs,
        isCompleted = isCompleted,
        totalVolumeKg = totalVolumeKg
    )

    companion object {
        fun fromDomain(session: WorkoutSession): WorkoutSessionEntity = WorkoutSessionEntity(
            id = session.id,
            title = session.title,
            startTimeEpochMs = session.startTimeEpochMs,
            endTimeEpochMs = session.endTimeEpochMs,
            isCompleted = session.isCompleted,
            totalVolumeKg = session.totalVolumeKg
        )
    }
}

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
    val previousPerformance: String?
) {
    fun toDomain(): WorkoutSet = WorkoutSet(
        id = id,
        sessionId = sessionId,
        exerciseId = exerciseId,
        setNumber = setNumber,
        weightKg = weightKg,
        reps = reps,
        rpe = rpe,
        isCompleted = isCompleted,
        previousPerformance = previousPerformance
    )

    companion object {
        fun fromDomain(set: WorkoutSet): WorkoutSetEntity = WorkoutSetEntity(
            id = set.id,
            sessionId = set.sessionId,
            exerciseId = set.exerciseId,
            setNumber = set.setNumber,
            weightKg = set.weightKg,
            reps = set.reps,
            rpe = set.rpe,
            isCompleted = set.isCompleted,
            previousPerformance = set.previousPerformance
        )
    }
}
