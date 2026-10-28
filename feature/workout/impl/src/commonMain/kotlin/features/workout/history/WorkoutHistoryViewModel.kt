package features.workout.history

import androidx.lifecycle.viewModelScope
import core.result.DataError
import core.ui.toUiText
import kotlinx.coroutines.Job
import core.common.AppClock
import core.common.SystemAppClock
import core.platform.NoOpPlatformClipboard
import core.platform.PlatformClipboard
import core.viewmodel.BaseViewModel
import features.exercises.api.Exercise
import features.workout.domain.WeeklyActivityCalculator
import features.workout.domain.WorkoutDataExport
import features.workout.domain.WorkoutRepository
import features.workout.domain.WorkoutSession
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

internal class WorkoutHistoryViewModel(
    private val workoutRepository: WorkoutRepository,
    private val dataExport: WorkoutDataExport,
    private val platformClipboard: PlatformClipboard = NoOpPlatformClipboard(),
    private val appClock: AppClock = SystemAppClock()
) : BaseViewModel<WorkoutHistoryContract.State, WorkoutHistoryContract.Intent, WorkoutHistoryContract.Effect>(
    WorkoutHistoryContract.State()
) {

    private var loadJob: Job? = null
    private var statsJob: Job? = null

    init {
        onIntent(WorkoutHistoryContract.Intent.LoadHistory)
    }

    override fun onOperationFailure(error: DataError.Local) {
        updateState { copy(error = error.toUiText(), isLoading = false) }
        sendEffect(WorkoutHistoryContract.Effect.ShowSnackbar(error.toUiText().asString()))
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
            is WorkoutHistoryContract.Intent.OpenRoutines -> sendEffect(WorkoutHistoryContract.Effect.NavigateToRoutines)
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
                statsJob?.cancel()
                updateState { copy(isExerciseStatsOpen = false, selectedStatsExercise = null, exerciseStats = null) }
            }
        }
    }

    private fun loadHistory() {
        loadJob?.cancel()
        loadJob = launchOperation {
            updateState { copy(isLoading = true, error = null) }
            workoutRepository.ensureDefaultExercises()

            // Samostatný lehký odběr počtu čekajících mutací v Outboxu,
            // aby změna stavu synchronizace nespouštěla přepočet celé historie a čtení všech sérií.
            launch {
                workoutRepository.getPendingSyncMutationsCount().collect { pendingCount ->
                    updateState { copy(pendingSyncCount = pendingCount) }
                }
            }

            combine(
                workoutRepository.getCompletedSessions(),
                workoutRepository.getActiveSession(),
                workoutRepository.getExercises()
            ) { completed, active, exercises ->
                val totalVolume = completed.sumOf { it.totalVolumeKg }
                HistoryData(completed, active, exercises, totalVolume)
            }.collect { data ->
                val allSets = workoutRepository.getAllCompletedSets()
                val exerciseMap = data.exercises.associateBy { it.id }
                val sessionExerciseMap = allSets
                    .groupBy { it.sessionId }
                    .mapValues { (_, sets) ->
                        sets.mapNotNull { it.exerciseNameSnapshot ?: exerciseMap[it.exerciseId]?.name }.distinct()
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
                        allExercises = data.exercises,
                        totalVolumeAllTimeKg = data.totalVolume,
                        totalWorkoutsCount = data.completed.size,
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
        val exercises: List<Exercise>,
        val totalVolume: Double
    )

    private fun startNewWorkout() {
        launchOperation {
            if (uiState.value.activeSession == null) {
                workoutRepository.startWorkout()
            }
            sendEffect(WorkoutHistoryContract.Effect.NavigateToActiveWorkout)
        }
    }

    private fun openExportDialog() {
        launchOperation {
            val csv = dataExport.exportWorkoutDataCsv()
            val json = dataExport.exportWorkoutDataJson()
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
                if (isJson) "✓ JSON export tréninků zkopírován do schránky!" else "✓ CSV export zkopírován do schránky!"
            )
        )
    }

    private fun openExerciseStats(exercise: Exercise) {
        statsJob?.cancel()
        updateState {
            copy(
                isExerciseStatsOpen = true,
                selectedStatsExercise = exercise,
                exerciseStats = null
            )
        }
        statsJob = launchOperation {
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
