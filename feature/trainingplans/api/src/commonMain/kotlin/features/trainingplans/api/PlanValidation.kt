package features.trainingplans.api

/** Live aggregates only; incomplete local drafts bypass publication validation. */
object PlanValidation {
    fun validate(plan: TrainingPlan): PlanError? {
        if (plan.id.isBlank() || plan.name.trim().length !in 1..100 || plan.notes.length > 2000 ||
            plan.slots.size !in 1..31) return PlanError.VALIDATION
        val ids = mutableSetOf<String>()
        for (slot in plan.slots) {
            if (slot.id.isBlank() || !ids.add(slot.id) || slot.routineId.isBlank() ||
                slot.label.trim().length !in 1..100) return PlanError.VALIDATION
        }
        return null
    }
}
