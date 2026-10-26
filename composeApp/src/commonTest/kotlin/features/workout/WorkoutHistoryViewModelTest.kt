package features.workout

import features.workout.domain.Exercise
import features.workout.domain.MuscleGroup
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
    private lateinit var viewModel: WorkoutHistoryViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = HistoryFakeWorkoutRepository()
        viewModel = WorkoutHistoryViewModel(fakeRepository)
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
}

private class HistoryFakeWorkoutRepository : WorkoutRepository {
    private val completed = MutableStateFlow<List<WorkoutSession>>(emptyList())
    private val active = MutableStateFlow<WorkoutSession?>(null)

    fun setCompletedSessions(list: List<WorkoutSession>) {
        completed.value = list
    }

    fun getActiveSessionSync(): WorkoutSession? = active.value

    override fun getExercises(): Flow<List<Exercise>> = MutableStateFlow(emptyList())
    override suspend fun ensureDefaultExercises() {}
    override fun getActiveSession(): Flow<WorkoutSession?> = active.asStateFlow()
    override fun getCompletedSessions(): Flow<List<WorkoutSession>> = completed.asStateFlow()
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

    override suspend fun addSet(sessionId: String, exerciseId: String, weightKg: Double, reps: Int, rpe: Double?): WorkoutSet = error("Not needed")
    override suspend fun updateSet(set: WorkoutSet) {}
    override suspend fun deleteSet(setId: String) {}
    override suspend fun completeWorkout(sessionId: String): WorkoutSession? = null
    override suspend fun cancelWorkout(sessionId: String) {}
    override suspend fun getLastPerformance(exerciseId: String): String? = null
}
