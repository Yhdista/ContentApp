package features.workout.data.mapper

import features.workout.data.entity.ExerciseEntity
import features.workout.data.entity.WorkoutSessionEntity
import features.workout.data.entity.WorkoutSetEntity
import features.workout.data.entity.WorkoutTemplateEntity
import features.workout.data.entity.WorkoutTemplateExerciseEntity
import features.workout.domain.Exercise
import features.workout.domain.MuscleGroup
import features.workout.domain.WorkoutSession
import features.workout.domain.WorkoutSet
import features.workout.domain.WorkoutSetType
import features.workout.domain.WorkoutTemplate
import features.workout.domain.WorkoutTemplateExercise

// Exercise Mappers
fun ExerciseEntity.toDomain(): Exercise = Exercise(
    id = id,
    name = name,
    muscleGroup = runCatching { MuscleGroup.valueOf(muscleGroup) }.getOrDefault(MuscleGroup.CHEST),
    defaultRestSeconds = defaultRestSeconds
)

fun Exercise.toEntity(): ExerciseEntity = ExerciseEntity(
    id = id,
    name = name,
    muscleGroup = muscleGroup.name,
    defaultRestSeconds = defaultRestSeconds
)

// WorkoutSession Mappers
fun WorkoutSessionEntity.toDomain(): WorkoutSession = WorkoutSession(
    id = id,
    title = title,
    startTimeEpochMs = startTimeEpochMs,
    endTimeEpochMs = endTimeEpochMs,
    isCompleted = isCompleted,
    totalVolumeKg = totalVolumeKg
)

fun WorkoutSession.toEntity(): WorkoutSessionEntity = WorkoutSessionEntity(
    id = id,
    title = title,
    startTimeEpochMs = startTimeEpochMs,
    endTimeEpochMs = endTimeEpochMs,
    isCompleted = isCompleted,
    totalVolumeKg = totalVolumeKg
)

// WorkoutSet Mappers
fun WorkoutSetEntity.toDomain(): WorkoutSet = WorkoutSet(
    id = id,
    sessionId = sessionId,
    exerciseId = exerciseId,
    setNumber = setNumber,
    weightKg = weightKg,
    reps = reps,
    rpe = rpe,
    isCompleted = isCompleted,
    previousPerformance = previousPerformance,
    isPersonalRecord = isPersonalRecord,
    setType = runCatching { WorkoutSetType.valueOf(setType) }.getOrDefault(WorkoutSetType.NORMAL)
)

fun WorkoutSet.toEntity(): WorkoutSetEntity = WorkoutSetEntity(
    id = id,
    sessionId = sessionId,
    exerciseId = exerciseId,
    setNumber = setNumber,
    weightKg = weightKg,
    reps = reps,
    rpe = rpe,
    isCompleted = isCompleted,
    previousPerformance = previousPerformance,
    isPersonalRecord = isPersonalRecord,
    setType = setType.name
)

// WorkoutTemplate Mappers
fun WorkoutTemplateEntity.toDomain(exercises: List<WorkoutTemplateExercise> = emptyList()): WorkoutTemplate = WorkoutTemplate(
    id = id,
    name = name,
    description = description,
    exercises = exercises
)

fun WorkoutTemplate.toEntity(): WorkoutTemplateEntity = WorkoutTemplateEntity(
    id = id,
    name = name,
    description = description
)

fun WorkoutTemplateExerciseEntity.toDomain(): WorkoutTemplateExercise = WorkoutTemplateExercise(
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    targetSets = targetSets,
    defaultWeightKg = defaultWeightKg,
    defaultReps = defaultReps,
    orderIndex = orderIndex
)

fun WorkoutTemplateExercise.toEntity(templateId: String): WorkoutTemplateExerciseEntity = WorkoutTemplateExerciseEntity(
    templateId = templateId,
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    targetSets = targetSets,
    defaultWeightKg = defaultWeightKg,
    defaultReps = defaultReps,
    orderIndex = orderIndex
)
