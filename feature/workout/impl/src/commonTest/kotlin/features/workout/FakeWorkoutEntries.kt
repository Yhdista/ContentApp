package features.workout

import features.workout.domain.WorkoutExercise
import features.workout.domain.WorkoutRepository
import features.exercises.api.MuscleGroup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Existing presentation fakes retain their compact set-only storage. Production reads saved entries. */
internal fun fakeWorkoutEntries(repository: WorkoutRepository, sessionId: String) = repository.getSetsForSession(sessionId).map { sets ->
    val catalogue=repository.getExercises().first().associateBy { it.id }
    sets.groupBy { it.workoutExerciseId ?: it.exerciseId }.map { (id,group) ->
        val set=group.first(); val exercise=catalogue[set.exerciseId]
        WorkoutExercise(id,sessionId,set.exerciseId,set.exerciseNameSnapshot ?: exercise?.name ?: "Neznámý cvik",
            set.muscleGroupSnapshot ?: exercise?.muscleGroup ?: MuscleGroup.CHEST,
            set.restSecondsSnapshot ?: exercise?.defaultRestSeconds ?: 90,set.exerciseOrder ?: 0)
    }
}
