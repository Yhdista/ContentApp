package features.trainingplans.data

import features.trainingplans.api.*
import features.workout.domain.PlanWorkoutSource

/** Pure transitions. The repository validates eligibility and persists with progress revision CAS. */
internal object PlanProgressRules {
    fun afterSave(plan: TrainingPlan, previous: PlanProgress?): PlanProgress = when {
        previous == null -> PlanProgress(plan.id, plan.slots.first().id, 1, false)
        previous.nextSlotId != null && plan.slots.none { it.id == previous.nextSlotId } ->
            previous.copy(nextSlotId = null, progressRevision = previous.progressRevision + 1, needsReview = true)
        else -> previous
    }

    fun next(plan: TrainingPlan, progress: PlanProgress): PlanProgress {
        val index = plan.slots.indexOfFirst { it.id == progress.nextSlotId }
        require(index >= 0)
        return progress.copy(nextSlotId = plan.slots[(index + 1) % plan.slots.size].id,
            progressRevision = progress.progressRevision + 1)
    }

    fun completed(plan: TrainingPlan, progress: PlanProgress, source: PlanWorkoutSource): ProgressOutcome = when {
        plan.archived -> ProgressOutcome.ArchivedUnchanged(progress)
        progress.needsReview || source.planId != plan.id || source.planRevision != plan.revision ||
            source.slotId != progress.nextSlotId || source.progressRevision != progress.progressRevision ||
            plan.slots.none { it.id == progress.nextSlotId } -> ProgressOutcome.ReviewRequired(
                if (progress.needsReview) progress else progress.copy(needsReview = true, progressRevision = progress.progressRevision + 1))
        else -> ProgressOutcome.Advanced(next(plan, progress))
    }
}
