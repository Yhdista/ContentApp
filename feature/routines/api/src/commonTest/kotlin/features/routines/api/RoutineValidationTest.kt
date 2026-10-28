package features.routines.api

import kotlin.test.Test
import kotlin.test.assertEquals

class RoutineValidationTest {
    private fun routine(entries: Int = 1, sets: Int = 1): Routine = Routine(
        "routine", "Routine", exercises = List(entries) { e ->
            RoutineExercise("entry-$e", "same-exercise", 90,
                List(sets) { s -> RoutineSet("set-$e-$s", 0.0, 1) })
        }
    )
    private fun valid(routine: Routine) = assertEquals(null, RoutineValidation.validate(routine))
    private fun invalid(routine: Routine) = assertEquals(RoutineError.VALIDATION, RoutineValidation.validate(routine))

    @Test fun exerciseEntryBoundaries() {
        invalid(routine(entries = 0)); valid(routine(entries = 1))
        valid(routine(entries = 50)); invalid(routine(entries = 51))
    }
    @Test fun setBoundaries() {
        invalid(routine(sets = 0)); valid(routine(sets = 1))
        valid(routine(sets = 20)); invalid(routine(sets = 21))
    }
    @Test fun repetitionBoundaries() {
        for ((reps, accepted) in listOf(0 to false, 1 to true, 999 to true, 1000 to false)) {
            val r = routine().let { r -> r.copy(exercises = r.exercises.map { e -> e.copy(sets = e.sets.map { it.copy(reps = reps) }) }) }
            if (accepted) valid(r) else invalid(r)
        }
    }
    @Test fun weightMustBeFiniteAndNonnegative() {
        for (weight in listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            val r = routine().let { r -> r.copy(exercises = r.exercises.map { e -> e.copy(sets = e.sets.map { it.copy(weightKg = weight) }) }) }
            invalid(r)
        }
        valid(routine())
    }
    @Test fun restBoundaries() {
        for ((rest, accepted) in listOf(-1 to false, 0 to true, 3600 to true, 3601 to false)) {
            val r = routine().let { r -> r.copy(exercises = r.exercises.map { it.copy(restSeconds = rest) }) }
            if (accepted) valid(r) else invalid(r)
        }
    }
    @Test fun nameIsTrimmedBeforeLengthValidation() {
        invalid(routine().copy(name = " \t\n")); valid(routine().copy(name = "a"))
        valid(routine().copy(name = " " + "a".repeat(100) + " "))
        invalid(routine().copy(name = "a".repeat(101)))
    }
    @Test fun notesBoundaries() {
        valid(routine().copy(notes = "a".repeat(2000)))
        invalid(routine().copy(notes = "a".repeat(2001)))
    }
    @Test fun stableIdsAreRequiredAndUniqueButCatalogueIdsMayRepeat() {
        valid(routine(entries = 2))
        invalid(routine().copy(id = " "))
        val r = routine(entries = 2)
        invalid(r.copy(exercises = r.exercises.map { it.copy(id = "same") }))
        invalid(r.copy(exercises = r.exercises.map { e -> e.copy(sets = e.sets.map { it.copy(id = "same") }) }))
        invalid(r.copy(exercises = r.exercises.map { it.copy(id = "") }))
        invalid(r.copy(exercises = r.exercises.map { e -> e.copy(sets = e.sets.map { it.copy(id = " ") }) }))
        invalid(r.copy(exercises = r.exercises.map { it.copy(exerciseId = "") }))
    }
}
