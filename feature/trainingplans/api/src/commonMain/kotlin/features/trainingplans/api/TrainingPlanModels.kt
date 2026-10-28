package features.trainingplans.api

import features.workout.domain.PlanWorkoutSource
import kotlinx.serialization.Serializable

@Serializable
data class TrainingPlan(val id: String, val name: String, val notes: String = "", val revision: Long = 0,
    val archived: Boolean = false, val slots: List<PlanSlot>)

@Serializable
data class PlanSlot(val id: String, val label: String, val routineId: String)

@Serializable
data class PlanProgress(val planId: String, val nextSlotId: String?, val progressRevision: Long, val needsReview: Boolean)

data class PlanStartContext(val plan: TrainingPlan, val slot: PlanSlot, val source: PlanWorkoutSource)

sealed interface ProgressOutcome {
    val progress: PlanProgress
    data class Advanced(override val progress: PlanProgress) : ProgressOutcome
    data class ReviewRequired(override val progress: PlanProgress) : ProgressOutcome
    data class ArchivedUnchanged(override val progress: PlanProgress) : ProgressOutcome
}
