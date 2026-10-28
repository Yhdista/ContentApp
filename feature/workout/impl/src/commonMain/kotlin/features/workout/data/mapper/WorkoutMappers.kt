package features.workout.data.mapper

import features.workout.data.entity.WorkoutSessionEntity
import features.workout.data.entity.WorkoutSetEntity
import features.workout.domain.WorkoutSession
import features.workout.domain.WorkoutSet
import features.workout.domain.WorkoutSetType

// WorkoutSession Mappers
internal fun WorkoutSessionEntity.toDomain(): WorkoutSession = WorkoutSession(
    id = id,
    title = title,
    startTimeEpochMs = startTimeEpochMs,
    endTimeEpochMs = endTimeEpochMs,
    isCompleted = isCompleted,
    totalVolumeKg = totalVolumeKg,
    sourceRoutineId = sourceRoutineId, sourceRoutineRevision = sourceRoutineRevision,
    sourcePlan = sourcePlanId?.let { features.workout.domain.PlanWorkoutSource(it, sourcePlanRevision!!, sourceSlotId!!, sourceProgressRevision!!) }
)

internal fun WorkoutSession.toEntity(): WorkoutSessionEntity = WorkoutSessionEntity(
    id = id,
    title = title,
    startTimeEpochMs = startTimeEpochMs,
    endTimeEpochMs = endTimeEpochMs,
    isCompleted = isCompleted,
    totalVolumeKg = totalVolumeKg,
    sourceRoutineId = sourceRoutineId, sourceRoutineRevision = sourceRoutineRevision,
    sourcePlanId = sourcePlan?.planId, sourcePlanRevision = sourcePlan?.planRevision,
    sourceSlotId = sourcePlan?.slotId, sourceProgressRevision = sourcePlan?.progressRevision
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
    setType = runCatching { WorkoutSetType.valueOf(setType) }.getOrDefault(WorkoutSetType.NORMAL),
    workoutExerciseId = workoutExerciseId, prescribedWeightKg = prescribedWeightKg,
    prescribedReps = prescribedReps, prescribedSetType = prescribedSetType?.let { WorkoutSetType.valueOf(it) }
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
    setType = setType.name,
    workoutExerciseId = workoutExerciseId, prescribedWeightKg = prescribedWeightKg,
    prescribedReps = prescribedReps, prescribedSetType = prescribedSetType?.name
)

internal fun features.workout.data.entity.WorkoutExerciseEntity.toDomain() = features.workout.domain.WorkoutExercise(
    id, sessionId, exerciseId, name, runCatching { features.exercises.api.MuscleGroup.valueOf(muscleGroup) }.getOrDefault(features.exercises.api.MuscleGroup.CHEST), restSeconds, orderIndex, orderReconstructed)
internal fun features.workout.domain.WorkoutExercise.toEntity() = features.workout.data.entity.WorkoutExerciseEntity(
    id, sessionId, exerciseId, name, muscleGroup.name, restSeconds, orderIndex, orderReconstructed)

internal fun features.workout.data.entity.WorkoutPrescriptionSetEntity.toDomain() = features.workout.domain.WorkoutPrescriptionSet(
    id, sessionId, workoutExerciseId, exerciseId, name, features.exercises.api.MuscleGroup.valueOf(muscleGroup),
    restSeconds, exerciseOrder, setNumber, weightKg, reps, WorkoutSetType.valueOf(setType), sourceRoutineExerciseId, sourceRoutineSetId)
internal fun features.workout.domain.WorkoutPrescriptionSet.toEntity() = features.workout.data.entity.WorkoutPrescriptionSetEntity(
    id, sessionId, workoutExerciseId, exerciseId, name, muscleGroup.name, restSeconds, exerciseOrder,
    setNumber, weightKg, reps, setType.name, sourceRoutineExerciseId, sourceRoutineSetId)
