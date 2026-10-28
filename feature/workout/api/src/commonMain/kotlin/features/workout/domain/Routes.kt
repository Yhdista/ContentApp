package features.workout.domain

import kotlinx.serialization.Serializable

@Serializable data object WorkoutHistoryRoute
@Serializable data object ActiveWorkoutRoute
@Serializable data class WorkoutDetailRoute(val sessionId: String)
