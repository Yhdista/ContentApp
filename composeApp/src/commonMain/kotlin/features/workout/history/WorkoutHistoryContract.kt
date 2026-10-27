package features.workout.history

import core.mvi.UiEffect
import core.mvi.UiIntent
import core.mvi.UiState
import features.workout.domain.Exercise
import features.workout.domain.ExerciseStats
import features.workout.domain.WeeklyActivitySummary
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
        val error: String? = null,
        val allExercises: List<Exercise> = emptyList(),
        val isExerciseCatalogOpen: Boolean = false,
        val isExerciseStatsOpen: Boolean = false,
        val selectedStatsExercise: Exercise? = null,
        val exerciseStats: ExerciseStats? = null,
        val isExportDialogOpen: Boolean = false,
        val exportCsvData: String = "",
        val exportJsonData: String = "",
        val exportSessionsCount: Int = 0,
        val exportSetsCount: Int = 0,
        val weeklyActivity: WeeklyActivitySummary? = null,
        val searchQuery: String = "",
        val sessionExerciseNames: Map<String, List<String>> = emptyMap()
    ) : UiState {
        val filteredCompletedSessions: List<WorkoutSession>
            get() {
                if (searchQuery.isBlank()) return completedSessions
                val query = searchQuery.trim().lowercase()
                return completedSessions.filter { session ->
                    session.title.lowercase().contains(query) ||
                    sessionExerciseNames[session.id]?.any { it.lowercase().contains(query) } == true
                }
            }
    }

    sealed class Intent : UiIntent {
        data object LoadHistory : Intent()
        data class UpdateSearchQuery(val query: String) : Intent()
        data object ClearSearchQuery : Intent()
        data object StartNewWorkout : Intent()
        data object OpenTemplatePicker : Intent()
        data object CloseTemplatePicker : Intent()
        data class StartWorkoutFromTemplate(val templateId: String) : Intent()
        data object ResumeActiveWorkout : Intent()
        data class WorkoutClicked(val sessionId: String) : Intent()
        data object OpenExportDialog : Intent()
        data object CloseExportDialog : Intent()
        data class CopyExportData(val text: String, val isJson: Boolean) : Intent()
        data object OpenExerciseCatalog : Intent()
        data object CloseExerciseCatalog : Intent()
        data class OpenExerciseStats(val exercise: Exercise) : Intent()
        data object CloseExerciseStats : Intent()
    }

    sealed class Effect : UiEffect {
        data object NavigateToActiveWorkout : Effect()
        data class NavigateToDetail(val sessionId: String) : Effect()
        data class ShowSnackbar(val message: String) : Effect()
    }
}
