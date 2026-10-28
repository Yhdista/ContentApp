package features.workout.data.mapper

import features.workout.data.entity.WorkoutSessionEntity
import features.workout.data.entity.WorkoutSetEntity
import features.workout.data.entity.WorkoutTemplateEntity
import features.workout.data.entity.WorkoutTemplateExerciseEntity
import features.workout.domain.WorkoutSession
import features.workout.domain.WorkoutSet
import features.workout.domain.WorkoutSetType
import features.workout.domain.WorkoutTemplate
import features.workout.domain.WorkoutTemplateExercise

// WorkoutSession Mappers
internal fun WorkoutSessionEntity.toDomain(): WorkoutSession = WorkoutSession(
    id = id,
    title = title,
    startTimeEpochMs = startTimeEpochMs,
    endTimeEpochMs = endTimeEpochMs,
    isCompleted = isCompleted,
    totalVolumeKg = totalVolumeKg
)

internal fun WorkoutSession.toEntity(): WorkoutSessionEntity = WorkoutSessionEntity(
    id = id,
    title = title,
    startTimeEpochMs = startTimeEpochMs,
    endTimeEpochMs = endTimeEpochMs,
    isCompleted = isCompleted,
    totalVolumeKg = totalVolumeKg
)

// WorkoutSet Mappers
internal fun WorkoutSetEntity.toDomain(): WorkoutSet = WorkoutSet(
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

internal fun WorkoutSet.toEntity(): WorkoutSetEntity = WorkoutSetEntity(
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
internal fun WorkoutTemplateEntity.toDomain(exercises: List<WorkoutTemplateExercise> = emptyList()): WorkoutTemplate = WorkoutTemplate(
    id = id,
    name = name,
    description = description,
    exercises = exercises
)

internal fun WorkoutTemplate.toEntity(): WorkoutTemplateEntity = WorkoutTemplateEntity(
    id = id,
    name = name,
    description = description
)

internal fun WorkoutTemplateExerciseEntity.toDomain(): WorkoutTemplateExercise = WorkoutTemplateExercise(
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    targetSets = targetSets,
    defaultWeightKg = defaultWeightKg,
    defaultReps = defaultReps,
    orderIndex = orderIndex
)

internal fun WorkoutTemplateExercise.toEntity(templateId: String): WorkoutTemplateExerciseEntity = WorkoutTemplateExerciseEntity(
    templateId = templateId,
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    targetSets = targetSets,
    defaultWeightKg = defaultWeightKg,
    defaultReps = defaultReps,
    orderIndex = orderIndex
)
