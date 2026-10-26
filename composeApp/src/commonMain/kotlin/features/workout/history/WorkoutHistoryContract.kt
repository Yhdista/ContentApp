package features.workout.history

import core.mvi.UiEffect
import core.mvi.UiIntent
import core.mvi.UiState
import features.workout.domain.WorkoutSession
import features.workout.domain.WorkoutTemplate

class WorkoutHistoryContract {
    data class State(
        val completedSessions: List<WorkoutSession> = emptyList(),
        val activeSession: WorkoutSession? = null,
        val templates: List<WorkoutTemplate> = emptyList(),
        val isTemplatePickerOpen: Boolean = false,
        val totalVolumeAllTimeKg: Double = 0.0,
        val totalWorkoutsCount: Int = 0,
        val pendingSyncCount: Int = 0,
        val isLoading: Boolean = false,
        val error: String? = null
    ) : UiState

    sealed class Intent : UiIntent {
        data object LoadHistory : Intent()
        data object StartNewWorkout : Intent()
        data object OpenTemplatePicker : Intent()
        data object CloseTemplatePicker : Intent()
        data class StartWorkoutFromTemplate(val templateId: String) : Intent()
        data object ResumeActiveWorkout : Intent()
        data class WorkoutClicked(val sessionId: String) : Intent()
    }

    sealed class Effect : UiEffect {
        data object NavigateToActiveWorkout : Effect()
        data class NavigateToDetail(val sessionId: String) : Effect()
        data class ShowSnackbar(val message: String) : Effect()
    }
}
