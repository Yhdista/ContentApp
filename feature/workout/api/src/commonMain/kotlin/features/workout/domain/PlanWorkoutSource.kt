package features.workout.domain

import kotlinx.serialization.Serializable

/** Neutral provenance captured when a workout starts; completion evaluates this original plan. */
@Serializable
data class PlanWorkoutSource(val planId: String, val planRevision: Long, val slotId: String, val progressRevision: Long)
