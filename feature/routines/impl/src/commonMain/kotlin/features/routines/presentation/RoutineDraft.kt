package features.routines.presentation

import features.routines.api.*
import features.workout.domain.WorkoutSetType
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
internal fun newId() = Uuid.random().toString()

@Serializable
internal data class RoutineDraft(val id: String = newId(), val expectedRevision: Long? = null,
    val name: String = "", val notes: String = "", val exercises: List<EntryDraft> = emptyList(), val version: Int = 1) {
    fun aggregate(copy: Boolean = false): Routine? {
        val entries = exercises.map { entry ->
            val rest = entry.rest.toIntOrNull() ?: return null
            val sets = entry.sets.map { set ->
                RoutineSet(if(copy) newId() else set.id, set.weight.toDoubleOrNull() ?: return null,
                    set.reps.toIntOrNull() ?: return null, set.type)
            }
            RoutineExercise(if(copy) newId() else entry.id, entry.exerciseId, rest, sets, entry.legacyValues)
        }
        return Routine(if(copy) newId() else id, name, notes, exercises = entries)
            .takeIf { RoutineValidation.validate(it) == null }
    }
}
@Serializable
internal data class EntryDraft(val id: String = newId(), val exerciseId: String, val rest: String = "90",
    val sets: List<SetDraft> = listOf(SetDraft()), val legacyValues: LegacyRoutineValues? = null)
@Serializable
internal data class SetDraft(val id: String = newId(), val weight: String = "0", val reps: String = "10",
    val type: WorkoutSetType = WorkoutSetType.NORMAL)
internal fun Routine.toDraft() = RoutineDraft(id, revision, name, notes, exercises.map { e ->
    EntryDraft(e.id,e.exerciseId,e.restSeconds.toString(),e.sets.map { SetDraft(it.id,it.weightKg.toString(),it.reps.toString(),it.setType) },e.legacyValues)
})
