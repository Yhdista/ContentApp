package features.workout.history

import androidx.lifecycle.viewModelScope
import core.viewmodel.BaseViewModel
import features.workout.domain.WorkoutRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class WorkoutHistoryViewModel(
    private val workoutRepository: WorkoutRepository
) : BaseViewModel<WorkoutHistoryContract.State, WorkoutHistoryContract.Intent, WorkoutHistoryContract.Effect>(
    WorkoutHistoryContract.State()
) {

    init {
        onIntent(WorkoutHistoryContract.Intent.LoadHistory)
    }

    override fun handleIntent(intent: WorkoutHistoryContract.Intent) {
        when (intent) {
            is WorkoutHistoryContract.Intent.LoadHistory -> loadHistory()
            is WorkoutHistoryContract.Intent.StartNewWorkout -> startNewWorkout()
            is WorkoutHistoryContract.Intent.ResumeActiveWorkout -> {
                sendEffect(WorkoutHistoryContract.Effect.NavigateToActiveWorkout)
            }
            is WorkoutHistoryContract.Intent.WorkoutClicked -> {
                sendEffect(WorkoutHistoryContract.Effect.NavigateToDetail(intent.sessionId))
            }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            workoutRepository.ensureDefaultExercises()

            combine(
                workoutRepository.getCompletedSessions(),
                workoutRepository.getActiveSession(),
                workoutRepository.getPendingSyncMutationsCount()
            ) { completed, active, pendingCount ->
                val totalVolume = completed.sumOf { it.totalVolumeKg }
                WorkoutHistoryContract.State(
                    completedSessions = completed,
                    activeSession = active,
                    totalVolumeAllTimeKg = totalVolume,
                    totalWorkoutsCount = completed.size,
                    pendingSyncCount = pendingCount,
                    isLoading = false
                )
            }.collect { newState ->
                updateState { newState }
            }
        }
    }

    private fun startNewWorkout() {
        viewModelScope.launch {
            if (uiState.value.activeSession == null) {
                workoutRepository.startWorkout()
            }
            sendEffect(WorkoutHistoryContract.Effect.NavigateToActiveWorkout)
        }
    }
}
