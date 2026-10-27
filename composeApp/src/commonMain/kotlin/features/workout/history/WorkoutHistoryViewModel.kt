package features.workout.history

import androidx.lifecycle.viewModelScope
import core.common.AppClock
import core.common.SystemAppClock
import core.platform.NoOpPlatformClipboard
import core.platform.PlatformClipboard
import core.viewmodel.BaseViewModel
import features.workout.domain.Exercise
import features.workout.domain.WeeklyActivityCalculator
import features.workout.domain.WorkoutRepository
import features.workout.domain.WorkoutSession
import features.workout.domain.WorkoutTemplate
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class WorkoutHistoryViewModel(
    private val workoutRepository: WorkoutRepository,
    private val platformClipboard: PlatformClipboard = NoOpPlatformClipboard(),
    private val appClock: AppClock = SystemAppClock()
) : BaseViewModel<WorkoutHistoryContract.State, WorkoutHistoryContract.Intent, WorkoutHistoryContract.Effect>(
    WorkoutHistoryContract.State()
) {

    init {
        onIntent(WorkoutHistoryContract.Intent.LoadHistory)
    }

    override fun handleIntent(intent: WorkoutHistoryContract.Intent) {
        when (intent) {
            is WorkoutHistoryContract.Intent.LoadHistory -> loadHistory()
            is WorkoutHistoryContract.Intent.UpdateSearchQuery -> {
                updateState {
                    val filtered = WorkoutHistoryContract.filterSessions(completedSessions, sessionExerciseNames, intent.query)
                    copy(searchQuery = intent.query, filteredCompletedSessions = filtered)
                }
            }
            is WorkoutHistoryContract.Intent.ClearSearchQuery -> {
                updateState {
                    val filtered = WorkoutHistoryContract.filterSessions(completedSessions, sessionExerciseNames, "")
                    copy(searchQuery = "", filteredCompletedSessions = filtered)
                }
            }
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
            is WorkoutHistoryContract.Intent.OpenExportDialog -> openExportDialog()
            is WorkoutHistoryContract.Intent.CloseExportDialog -> {
                updateState { copy(isExportDialogOpen = false) }
            }
            is WorkoutHistoryContract.Intent.CopyExportData -> copyExportData(intent.text, intent.isJson)
            is WorkoutHistoryContract.Intent.OpenExerciseCatalog -> {
                updateState { copy(isExerciseCatalogOpen = true) }
            }
            is WorkoutHistoryContract.Intent.CloseExerciseCatalog -> {
                updateState { copy(isExerciseCatalogOpen = false) }
            }
            is WorkoutHistoryContract.Intent.OpenExerciseStats -> openExerciseStats(intent.exercise)
            is WorkoutHistoryContract.Intent.CloseExerciseStats -> {
                updateState { copy(isExerciseStatsOpen = false, selectedStatsExercise = null, exerciseStats = null) }
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
                workoutRepository.getTemplates(),
                workoutRepository.getExercises()
            ) { completed, active, pendingCount, templates, exercises ->
                val totalVolume = completed.sumOf { it.totalVolumeKg }
                HistoryData(completed, active, pendingCount, templates, exercises, totalVolume)
            }.collect { data ->
                val allSets = workoutRepository.getAllCompletedSets()
                val exerciseMap = data.exercises.associateBy { it.id }
                val sessionExerciseMap = allSets
                    .groupBy { it.sessionId }
                    .mapValues { (_, sets) ->
                        sets.mapNotNull { exerciseMap[it.exerciseId]?.name }.distinct()
                    }
                val weekly = WeeklyActivityCalculator.calculateWeeklyActivity(
                    nowEpochMs = appClock.nowMillis(),
                    sessions = data.completed,
                    timeZoneOffsetMs = appClock.timeZoneOffsetMillis()
                )

                val filtered = WorkoutHistoryContract.filterSessions(
                    data.completed,
                    sessionExerciseMap,
                    uiState.value.searchQuery
                )

                updateState {
                    copy(
                        completedSessions = data.completed,
                        activeSession = data.active,
                        templates = data.templates,
                        allExercises = data.exercises,
                        totalVolumeAllTimeKg = data.totalVolume,
                        totalWorkoutsCount = data.completed.size,
                        pendingSyncCount = data.pendingCount,
                        sessionExerciseNames = sessionExerciseMap,
                        weeklyActivity = weekly,
                        filteredCompletedSessions = filtered,
                        isLoading = false
                    )
                }
            }
        }
    }

    private data class HistoryData(
        val completed: List<WorkoutSession>,
        val active: WorkoutSession?,
        val pendingCount: Int,
        val templates: List<WorkoutTemplate>,
        val exercises: List<Exercise>,
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
            }
            sendEffect(WorkoutHistoryContract.Effect.NavigateToActiveWorkout)
        }
    }

    private fun openExportDialog() {
        viewModelScope.launch {
            val csv = workoutRepository.exportWorkoutDataCsv()
            val json = workoutRepository.exportWorkoutDataJson()
            val completedSets = workoutRepository.getAllCompletedSets()
            updateState {
                copy(
                    isExportDialogOpen = true,
                    exportCsvData = csv,
                    exportJsonData = json,
                    exportSessionsCount = completedSessions.size,
                    exportSetsCount = completedSets.size
                )
            }
        }
    }

    private fun copyExportData(text: String, isJson: Boolean) {
        platformClipboard.copyToClipboard("Workout Data", text)
        sendEffect(
            WorkoutHistoryContract.Effect.ShowSnackbar(
                if (isJson) "✓ JSON záloha zkopírována do schránky!" else "✓ CSV export zkopírován do schránky!"
            )
        )
    }

    private fun openExerciseStats(exercise: Exercise) {
        updateState {
            copy(
                isExerciseStatsOpen = true,
                selectedStatsExercise = exercise,
                exerciseStats = null
            )
        }
        viewModelScope.launch {
            workoutRepository.getExerciseStats(exercise.id).collect { stats ->
                updateState {
                    if (selectedStatsExercise?.id == exercise.id) {
                        copy(exerciseStats = stats)
                    } else this
                }
            }
        }
    }
}
