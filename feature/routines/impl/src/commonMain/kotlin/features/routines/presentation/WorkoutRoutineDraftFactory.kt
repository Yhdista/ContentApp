package features.routines.presentation

import core.storage.OrderedDrafts
import features.routines.api.RoutineDraftFactory
import features.workout.domain.WorkoutRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Conversion is a local editable proposal. Only the editor's Save creates the aggregate/outbox. */
internal class WorkoutRoutineDraftFactory(private val workouts: WorkoutRepository, private val drafts: OrderedDrafts) : RoutineDraftFactory {
    override suspend fun fromWorkout(sessionId: String): String {
        val id = "workout:$sessionId"
        val session = drafts.open("routines", id).await()
        if(session.payload == null) {
            val workout = workouts.getSessionById(sessionId) ?: error("Missing workout")
            val sets = workouts.getSetsForSession(sessionId).first()
            val entries = workouts.getWorkoutExercises(sessionId).first().sortedBy { it.orderIndex }
            val draft = RoutineDraft(name = workout.title, notes = "Vytvořeno ze skutečného výkonu", exercises = entries.map { entry ->
                EntryDraft(exerciseId = entry.exerciseId, rest = entry.restSeconds.toString(), sets =
                    sets.filter { it.workoutExerciseId == entry.id }.sortedBy { it.setNumber }.map { set ->
                        SetDraft(weight = set.weightKg.toString(), reps = set.reps.toString(), type = set.setType)
                    })
            })
            session.put(Json.encodeToString(draft)).await()
        }
        return id
    }
}
