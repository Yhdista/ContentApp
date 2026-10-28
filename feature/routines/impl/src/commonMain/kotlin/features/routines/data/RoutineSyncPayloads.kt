package features.routines.data

import features.routines.api.Routine
import features.workout.domain.WorkoutSetType
import kotlinx.serialization.Serializable

@Serializable
internal data class RoutineSyncPayload(val id: String, val name: String, val notes: String, val revision: Long, val archived: Boolean)

@Serializable
internal data class RoutineExerciseSyncPayload(val id: String, val routineId: String, val exerciseId: String, val restSeconds: Int, val orderIndex: Int)

@Serializable
internal data class RoutineSetSyncPayload(val id: String, val routineExerciseId: String, val weightKg: Double, val reps: Int, val setType: WorkoutSetType, val orderIndex: Int)

@Serializable
internal data class DeleteRoutineChildPayload(val id: String)

internal fun Routine.payload() = RoutineSyncPayload(id, name, notes, revision, archived)
