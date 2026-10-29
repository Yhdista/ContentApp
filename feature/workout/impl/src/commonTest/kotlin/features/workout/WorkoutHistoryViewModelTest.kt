package features.workout

import features.exercises.api.Exercise
import features.exercises.api.MuscleGroup
import features.workout.domain.WorkoutDataExport
import features.workout.domain.WorkoutRepository
import features.workout.domain.WorkoutSession
import features.workout.domain.WorkoutSet
import features.workout.history.WorkoutHistoryContract
import features.workout.history.WorkoutHistoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutHistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: HistoryFakeWorkoutRepository
    private lateinit var fakeClipboard: FakePlatformClipboard
    private lateinit var viewModel: WorkoutHistoryViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = HistoryFakeWorkoutRepository()
        fakeClipboard = FakePlatformClipboard()
        viewModel = WorkoutHistoryViewModel(fakeRepository, fakeRepository, fakeClipboard)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadHistoryCombinesCompletedAndActiveSessions() = runTest(testDispatcher) {
        val completed1 = WorkoutSession(
            id = "sess-1",
            title = "Trénink A",
            startTimeEpochMs = 1000L,
            endTimeEpochMs = 2000L,
            isCompleted = true,
            totalVolumeKg = 500.0
        )
        val completed2 = WorkoutSession(
            id = "sess-2",
            title = "Trénink B",
            startTimeEpochMs = 3000L,
            endTimeEpochMs = 4000L,
            isCompleted = true,
            totalVolumeKg = 800.0
        )
        fakeRepository.setCompletedSessions(listOf(completed1, completed2))

        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals(2, state.completedSessions.size)
        assertEquals(2, state.totalWorkoutsCount)
        assertEquals(1300.0, state.totalVolumeAllTimeKg)
        assertNull(state.activeSession)
    }

    @Test
    fun testStartNewWorkoutCreatesSessionAndEmitsEffect() = runTest(testDispatcher) {
        var effectReceived: WorkoutHistoryContract.Effect? = null
        val job = launch {
            viewModel.effect.collect { effectReceived = it }
        }

        viewModel.onIntent(WorkoutHistoryContract.Intent.StartNewWorkout)
        testScheduler.runCurrent()

        assertEquals(WorkoutHistoryContract.Effect.NavigateToActiveWorkout, effectReceived)
        assertNotNull(fakeRepository.getActiveSessionSync())
        job.cancel()
    }

    @Test
    fun testWorkoutClickedEmitsNavigateToDetailEffect() = runTest(testDispatcher) {
        var effectReceived: WorkoutHistoryContract.Effect? = null
        val job = launch {
            viewModel.effect.collect { effectReceived = it }
        }

        viewModel.onIntent(WorkoutHistoryContract.Intent.WorkoutClicked("sess-xyz"))
        testScheduler.runCurrent()

        assertEquals(WorkoutHistoryContract.Effect.NavigateToDetail("sess-xyz"), effectReceived)
        job.cancel()
    }

    @Test
    fun testLoadHistoryObservesPendingSyncMutationsCount() = runTest(testDispatcher) {
        fakeRepository.setPendingMutationsCount(3)
        testScheduler.runCurrent()

        assertEquals(3, viewModel.uiState.value.pendingSyncCount)
    }

    @Test fun routinesEntryNavigatesWithoutStartingWorkout() = runTest(testDispatcher) {
        val effects = mutableListOf<WorkoutHistoryContract.Effect>()
        val job = launch { viewModel.effect.collect { effects.add(it) } }
        viewModel.onIntent(WorkoutHistoryContract.Intent.OpenRoutines)
        testScheduler.runCurrent()
        assertEquals(listOf<WorkoutHistoryContract.Effect>(WorkoutHistoryContract.Effect.NavigateToRoutines), effects)
        assertEquals(null, viewModel.uiState.value.activeSession)
        job.cancel()
    }

    @Test
    fun testOpenExportDialogFetchesCsvAndJson() = runTest(testDispatcher) {
        viewModel.onIntent(WorkoutHistoryContract.Intent.OpenExportDialog)
        testScheduler.runCurrent()

        assertTrue(viewModel.uiState.value.isExportDialogOpen)
        assertEquals("header\nrow1", viewModel.uiState.value.exportCsvData)
        assertEquals("{\"count\": 1}", viewModel.uiState.value.exportJsonData)

        viewModel.onIntent(WorkoutHistoryContract.Intent.CloseExportDialog)
        testScheduler.runCurrent()
        assertFalse(viewModel.uiState.value.isExportDialogOpen)
    }

    @Test
    fun testCopyExportDataCopiesToClipboardAndSendsSnackbar() = runTest(testDispatcher) {
        var effectReceived: WorkoutHistoryContract.Effect? = null
        val job = backgroundScope.launch(testDispatcher) {
            viewModel.effect.collect { effectReceived = it }
        }

        viewModel.onIntent(WorkoutHistoryContract.Intent.CopyExportData("csv-data-here", isJson = false))
        testScheduler.runCurrent()

        assertEquals("csv-data-here", fakeClipboard.lastCopiedText)
        assertTrue(effectReceived is WorkoutHistoryContract.Effect.ShowSnackbar)
        assertEquals("✓ CSV export zkopírován do schránky!", (effectReceived as WorkoutHistoryContract.Effect.ShowSnackbar).message)
        job.cancel()
    }

    @Test
    fun testOpenExerciseCatalogAndOpenStats() = runTest(testDispatcher) {
        viewModel.onIntent(WorkoutHistoryContract.Intent.OpenExerciseCatalog)
        testScheduler.runCurrent()
        assertTrue(viewModel.uiState.value.isExerciseCatalogOpen)

        val exercise = Exercise("ex_bench", "Bench press", MuscleGroup.CHEST, 120)
        viewModel.onIntent(WorkoutHistoryContract.Intent.OpenExerciseStats(exercise))
        testScheduler.runCurrent()

        assertTrue(viewModel.uiState.value.isExerciseStatsOpen)
        assertEquals(exercise, viewModel.uiState.value.selectedStatsExercise)

        viewModel.onIntent(WorkoutHistoryContract.Intent.CloseExerciseStats)
        testScheduler.runCurrent()
        assertFalse(viewModel.uiState.value.isExerciseStatsOpen)
        assertNull(viewModel.uiState.value.selectedStatsExercise)
    }

    @Test
    fun testSearchQueryFilteringAndClear() = runTest(testDispatcher) {
        val session1 = WorkoutSession("s1", "Push Day - Hrudník", 1000L, 2000L, isCompleted = true)
        val session2 = WorkoutSession("s2", "Pull Day - Záda", 3000L, 4000L, isCompleted = true)
        val session3 = WorkoutSession("s3", "Leg Day - Nohy", 5000L, 6000L, isCompleted = true)

        fakeRepository.setCompletedSessions(listOf(session1, session2, session3))
        testScheduler.runCurrent()

        assertEquals(3, viewModel.uiState.value.filteredCompletedSessions.size)

        // Hledání "push"
        viewModel.onIntent(WorkoutHistoryContract.Intent.UpdateSearchQuery("push"))
        testScheduler.runCurrent()
        assertEquals(1, viewModel.uiState.value.filteredCompletedSessions.size)
        assertEquals("s1", viewModel.uiState.value.filteredCompletedSessions.first().id)

        // Hledání "záda"
        viewModel.onIntent(WorkoutHistoryContract.Intent.UpdateSearchQuery("záda"))
        testScheduler.runCurrent()
        assertEquals(1, viewModel.uiState.value.filteredCompletedSessions.size)
        assertEquals("s2", viewModel.uiState.value.filteredCompletedSessions.first().id)

        // Vymazání filtru
        viewModel.onIntent(WorkoutHistoryContract.Intent.ClearSearchQuery)
        testScheduler.runCurrent()
        assertEquals(3, viewModel.uiState.value.filteredCompletedSessions.size)
        assertEquals("", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun testWeeklyActivitySummaryIsCalculatedInState() = runTest(testDispatcher) {
        val session = WorkoutSession("s1", "Trénink", 1700000000000L, 1700003600000L, isCompleted = true)
        fakeRepository.setCompletedSessions(listOf(session))
        testScheduler.runCurrent()

        val weekly = viewModel.uiState.value.weeklyActivity
        assertNotNull(weekly)
        assertEquals(7, weekly.days.size)
    }
}

private class HistoryFakeWorkoutRepository : WorkoutRepository, WorkoutDataExport {
    private val completed = MutableStateFlow<List<WorkoutSession>>(emptyList())
    private val active = MutableStateFlow<WorkoutSession?>(null)
    private val pendingMutations = MutableStateFlow(0)

    fun setCompletedSessions(list: List<WorkoutSession>) {
        completed.value = list
    }

    fun setPendingMutationsCount(count: Int) {
        pendingMutations.value = count
    }

    fun getActiveSessionSync(): WorkoutSession? = active.value

    override fun getExercises(): Flow<List<Exercise>> = MutableStateFlow(emptyList())
    override fun getActiveSession(): Flow<WorkoutSession?> = active.asStateFlow()
    override fun getCompletedSessions(): Flow<List<WorkoutSession>> = completed.asStateFlow()
    override fun getPendingSyncMutationsCount(): Flow<Int> = pendingMutations.asStateFlow()
    override suspend fun getSessionById(sessionId: String): WorkoutSession? =
        completed.value.firstOrNull { it.id == sessionId } ?: active.value?.takeIf { it.id == sessionId }
    override fun getSetsForSession(sessionId: String): Flow<List<WorkoutSet>> = MutableStateFlow(emptyList())

    override suspend fun startWorkout(title: String): WorkoutSession {
        val session = WorkoutSession(
            id = "sess-new",
            title = title,
            startTimeEpochMs = 12345L,
            isCompleted = false
        )
        active.value = session
        return session
    }

    override fun getPrescriptionForSession(sessionId: String) = kotlinx.coroutines.flow.flowOf(emptyList<features.workout.domain.WorkoutPrescriptionSet>())
    override fun getWorkoutExercises(sessionId: String) = fakeWorkoutEntries(this,sessionId)
    override suspend fun addSetToEntry(sessionId: String, workoutExerciseId: String, weightKg: Double, reps: Int, rpe: Double?) = addSet(sessionId,workoutExerciseId,weightKg,reps,rpe)
    override suspend fun removeWorkoutExercise(sessionId: String, workoutExerciseId: String) = removeExerciseFromWorkout(sessionId,workoutExerciseId)
    override suspend fun addExerciseWithInitialSet(sessionId: String, exerciseId: String, weightKg: Double, reps: Int) = features.workout.domain.WorkoutExercise(
        exerciseId,sessionId,exerciseId,exerciseId,features.exercises.api.MuscleGroup.CHEST,90,0)
    override suspend fun updateSet(setId: String, update: (WorkoutSet) -> WorkoutSet): WorkoutSet = error("Not used")

    override suspend fun deleteSet(setId: String) {}
    override suspend fun cancelWorkout(sessionId: String) {}
    override suspend fun getLastPerformance(exerciseId: String): String? = null
    override suspend fun getHistoricalMax1RM(exerciseId: String, currentSessionId: String): Double = 0.0

    override fun getExerciseStats(exerciseId: String): Flow<features.workout.domain.ExerciseStats?> = MutableStateFlow(null)
    override suspend fun getAllCompletedSets(): List<WorkoutSet> = emptyList()
    override suspend fun exportWorkoutDataCsv(): String = "header\nrow1"
    override suspend fun exportWorkoutDataJson(): String = "{\"count\": 1}"
}

private class FakePlatformClipboard : core.platform.PlatformClipboard {
    var lastCopiedLabel: String? = null
    var lastCopiedText: String? = null
    override fun copyToClipboard(label: String, text: String) {
        lastCopiedLabel = label
        lastCopiedText = text
    }
}
