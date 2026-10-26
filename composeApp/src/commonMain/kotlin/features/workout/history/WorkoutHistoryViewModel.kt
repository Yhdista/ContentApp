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
            is WorkoutHistoryContract.Intent.OpenTemplatePicker -> {
                updateState { copy(isTemplatePickerOpen = true) }
            }
            is WorkoutHistoryContract.Intent.CloseTemplatePicker -> {
                updateState { copy(isTemplatePickerOpen = false) }
            }
            is WorkoutHistoryContract.Intent.StartWorkoutFromTemplate -> startWorkoutFromTemplate(intent.templateId)
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
            workoutRepository.ensureDefaultTemplates()

            combine(
                workoutRepository.getCompletedSessions(),
                workoutRepository.getActiveSession(),
                workoutRepository.getPendingSyncMutationsCount(),
                workoutRepository.getTemplates()
            ) { completed, active, pendingCount, templates ->
                val totalVolume = completed.sumOf { it.totalVolumeKg }
                HistoryData(completed, active, pendingCount, templates, totalVolume)
            }.collect { data ->
                updateState {
                    copy(
                        completedSessions = data.completed,
                        activeSession = data.active,
                        templates = data.templates,
                        totalVolumeAllTimeKg = data.totalVolume,
                        totalWorkoutsCount = data.completed.size,
                        pendingSyncCount = data.pendingCount,
                        isLoading = false
                    )
                }
            }
        }
    }

    private data class HistoryData(
        val completed: List<features.workout.domain.WorkoutSession>,
        val active: features.workout.domain.WorkoutSession?,
        val pendingCount: Int,
        val templates: List<features.workout.domain.WorkoutTemplate>,
        val totalVolume: Double
    )

    private fun startNewWorkout() {
        viewModelScope.launch {
            if (uiState.value.activeSession == null) {
                workoutRepository.startWorkout()
            }
            sendEffect(WorkoutHistoryContract.Effect.NavigateToActiveWorkout)
        }
    }

    private fun startWorkoutFromTemplate(templateId: String) {
        viewModelScope.launch {
            if (uiState.value.activeSession == null) {
                workoutRepository.startWorkoutFromTemplate(templateId)
                updateState { copy(isTemplatePickerOpen = false) }
                sendEffect(WorkoutHistoryContract.Effect.NavigateToActiveWorkout)
            } else {
                updateState { copy(isTemplatePickerOpen = false) }
                sendEffect(WorkoutHistoryContract.Effect.ShowSnackbar("Již probíhá aktivní trénink. Dokončete jej nebo zrušte."))
            }
        }
    }
}
