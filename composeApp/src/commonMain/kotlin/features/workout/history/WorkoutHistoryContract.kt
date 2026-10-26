package features.workout.history

import core.mvi.UiEffect
import core.mvi.UiIntent
import core.mvi.UiState
import features.workout.domain.WorkoutSession

class WorkoutHistoryContract {
    data class State(
        val completedSessions: List<WorkoutSession> = emptyList(),
        val activeSession: WorkoutSession? = null,
        val totalVolumeAllTimeKg: Double = 0.0,
        val totalWorkoutsCount: Int = 0,
        val isLoading: Boolean = false,
        val error: String? = null
    ) : UiState

    sealed class Intent : UiIntent {
        data object LoadHistory : Intent()
        data object StartNewWorkout : Intent()
        data object ResumeActiveWorkout : Intent()
    }

    sealed class Effect : UiEffect {
        data object NavigateToActiveWorkout : Effect()
        data class ShowSnackbar(val message: String) : Effect()
    }
}
