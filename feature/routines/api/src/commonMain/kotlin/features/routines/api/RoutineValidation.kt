package features.routines.api

/** Checks a publishable aggregate. Durable drafts deliberately bypass this validation. */
object RoutineValidation {
    fun validate(routine: Routine): RoutineError? {
        if (routine.id.isBlank() || routine.name.trim().length !in 1..100 || routine.notes.length > 2000 ||
            routine.exercises.size !in 1..50) return RoutineError.VALIDATION
        val entryIds = mutableSetOf<String>()
        val setIds = mutableSetOf<String>()
        for (entry in routine.exercises) {
            if (entry.id.isBlank() || !entryIds.add(entry.id) || entry.exerciseId.isBlank() ||
                entry.restSeconds !in 0..3600 || entry.sets.size !in 1..20) return RoutineError.VALIDATION
            for (set in entry.sets) {
                if (set.id.isBlank() || !setIds.add(set.id) || !set.weightKg.isFinite() ||
                    set.weightKg < 0 || set.reps !in 1..999) return RoutineError.VALIDATION
            }
        }
        return null
    }
}
