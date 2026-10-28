package features.exercises.data

import features.exercises.api.Exercise
import features.exercises.api.MuscleGroup

// Exercise Mappers
internal fun ExerciseEntity.toDomain(): Exercise = Exercise(
    id = id,
    name = name,
    muscleGroup = runCatching { MuscleGroup.valueOf(muscleGroup) }.getOrDefault(MuscleGroup.CHEST),
    defaultRestSeconds = defaultRestSeconds
)

internal fun Exercise.toEntity(): ExerciseEntity = ExerciseEntity(
    id = id,
    name = name,
    muscleGroup = muscleGroup.name,
    defaultRestSeconds = defaultRestSeconds
)
