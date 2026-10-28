package features.workout.history

import androidx.compose.runtime.Stable
import core.mvi.UiEffect
import core.mvi.UiIntent
import core.mvi.UiState
import core.ui.UiText
import features.exercises.api.Exercise
import features.workout.domain.ExerciseStats
import features.workout.domain.WeeklyActivitySummary
import features.workout.domain.WorkoutSession

internal class WorkoutHistoryContract {

    @Stable
    data class State(
        val completedSessions: List<WorkoutSession> = emptyList(),
        val activeSession: WorkoutSession? = null,
        val totalVolumeAllTimeKg: Double = 0.0,
        val totalWorkoutsCount: Int = 0,
        val pendingSyncCount: Int = 0,
        val isLoading: Boolean = false,
        val error: UiText? = null,
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
        val sessionExerciseNames: Map<String, List<String>> = emptyMap(),
        val filteredCompletedSessions: List<WorkoutSession> = emptyList()
    ) : UiState

    companion object {
        fun filterSessions(
            sessions: List<WorkoutSession>,
            sessionExerciseNames: Map<String, List<String>>,
            query: String
        ): List<WorkoutSession> {
            if (query.isBlank()) return sessions
            val clean = query.trim().lowercase()
            return sessions.filter { session ->
                session.title.lowercase().contains(clean) ||
                sessionExerciseNames[session.id]?.any { it.lowercase().contains(clean) } == true
            }
        }
    }

    sealed class Intent : UiIntent {
        data object LoadHistory : Intent()
        data class UpdateSearchQuery(val query: String) : Intent()
        data object ClearSearchQuery : Intent()
        data object StartNewWorkout : Intent()
        data object OpenRoutines : Intent()
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
        data object NavigateToRoutines : Effect()
        data object NavigateToActiveWorkout : Effect()
        data class NavigateToDetail(val sessionId: String) : Effect()
        data class ShowSnackbar(val message: String) : Effect()
    }
}
