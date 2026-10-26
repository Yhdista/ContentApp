package features.workout

import core.common.AppClock
import core.common.ApplicationScope
import features.workout.active.ActiveWorkoutContract
import features.workout.active.ActiveWorkoutViewModel
import features.workout.domain.Exercise
import features.workout.domain.MuscleGroup
import features.workout.domain.WorkoutRepository
import features.workout.domain.WorkoutSession
import features.workout.domain.WorkoutSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveWorkoutViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val appJob = SupervisorJob()

    private lateinit var fakeRepository: FakeWorkoutRepository
    private lateinit var fakeClock: WorkoutVmFakeClock
    private lateinit var appScope: ApplicationScope
    private lateinit var viewModel: ActiveWorkoutViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeClock = WorkoutVmFakeClock(time = 1000000L)
        fakeRepository = FakeWorkoutRepository(fakeClock)
        appScope = ApplicationScope(testDispatcher + appJob)

        viewModel = ActiveWorkoutViewModel(
            workoutRepository = fakeRepository,
            appScope = appScope,
            clock = fakeClock
        )
    }

    @AfterTest
    fun tearDown() {
        viewModel.cancelTimers()
        appJob.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialLoadLoadsActiveSessionAndExercises() = runTest(testDispatcher) {
        fakeRepository.startWorkout("Leg Day")
        viewModel.onIntent(ActiveWorkoutContract.Intent.LoadWorkout)
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertNotNull(state.session)
        assertTrue(state.availableExercises.isNotEmpty())
        viewModel.cancelTimers()
    }

    @Test
    fun testToggleSetCompleteTriggersRestTimer() = runTest(testDispatcher) {
        fakeRepository.startWorkout("Upper Body")
        viewModel.onIntent(ActiveWorkoutContract.Intent.LoadWorkout)
        testScheduler.runCurrent()

        val bench = fakeRepository.benchExercise
        viewModel.onIntent(ActiveWorkoutContract.Intent.AddExerciseToWorkout(bench))
        viewModel.onIntent(ActiveWorkoutContract.Intent.AddSet(bench.id))
        testScheduler.runCurrent()

        val set = viewModel.uiState.value.sets.first()
        assertFalse(set.isCompleted)
        assertFalse(viewModel.uiState.value.isRestTimerActive)

        // Odškrtnutí série -> auto trigger Rest Timeru!
        viewModel.onIntent(ActiveWorkoutContract.Intent.ToggleSetComplete(set))
        testScheduler.runCurrent()

        val updatedState = viewModel.uiState.value
        assertTrue(updatedState.isRestTimerActive)
        assertEquals(bench.defaultRestSeconds, updatedState.restTimerTotalSeconds)
        assertEquals(bench.defaultRestSeconds, updatedState.restTimerRemainingSeconds)

        // Vyčistit rest timer
        viewModel.onIntent(ActiveWorkoutContract.Intent.SkipRestTimer)
        viewModel.cancelTimers()
    }

    @Test
    fun testRestTimerAdjustAndSkip() = runTest(testDispatcher) {
        fakeRepository.startWorkout("Upper Body")
        viewModel.onIntent(ActiveWorkoutContract.Intent.LoadWorkout)
        testScheduler.runCurrent()

        val bench = fakeRepository.benchExercise
        viewModel.onIntent(ActiveWorkoutContract.Intent.AddExerciseToWorkout(bench))
        viewModel.onIntent(ActiveWorkoutContract.Intent.AddSet(bench.id))
        testScheduler.runCurrent()

        val set = viewModel.uiState.value.sets.first()
        viewModel.onIntent(ActiveWorkoutContract.Intent.ToggleSetComplete(set))
        testScheduler.runCurrent()

        // Přidání +30s
        viewModel.onIntent(ActiveWorkoutContract.Intent.AdjustRestTimer(30))
        testScheduler.runCurrent()
        assertEquals(bench.defaultRestSeconds + 30, viewModel.uiState.value.restTimerRemainingSeconds)

        // Přeskočení
        viewModel.onIntent(ActiveWorkoutContract.Intent.SkipRestTimer)
        testScheduler.runCurrent()
        assertFalse(viewModel.uiState.value.isRestTimerActive)
        viewModel.cancelTimers()
    }

    @Test
    fun testFinishWorkoutCompletesSessionInApplicationScope() = runTest(testDispatcher) {
        fakeRepository.startWorkout("Upper Body")
        viewModel.onIntent(ActiveWorkoutContract.Intent.LoadWorkout)
        testScheduler.runCurrent()

        val bench = fakeRepository.benchExercise
        viewModel.onIntent(ActiveWorkoutContract.Intent.AddExerciseToWorkout(bench))
        viewModel.onIntent(ActiveWorkoutContract.Intent.AddSet(bench.id))
        testScheduler.runCurrent()

        val set = viewModel.uiState.value.sets.first()
        viewModel.onIntent(ActiveWorkoutContract.Intent.ToggleSetComplete(set))
        testScheduler.runCurrent()

        viewModel.onIntent(ActiveWorkoutContract.Intent.FinishWorkout)
        testScheduler.runCurrent()

        // Sezení v repozitáři je dokončené
        val completed = fakeRepository.getCompletedSessions().first()
        assertTrue(completed.isNotEmpty())
    }
}

private class WorkoutVmFakeClock(var time: Long) : AppClock {
    override fun nowMillis(): Long = time
}

private class FakeWorkoutRepository(private val clock: AppClock) : WorkoutRepository {
    val benchExercise = Exercise("ex_bench", "Bench press", MuscleGroup.CHEST, defaultRestSeconds = 120)
    val squatExercise = Exercise("ex_squat", "Dřep", MuscleGroup.LEGS, defaultRestSeconds = 180)

    private val exercises = MutableStateFlow(listOf(benchExercise, squatExercise))
    private val activeSession = MutableStateFlow<WorkoutSession?>(null)
    private val completedSessions = MutableStateFlow<List<WorkoutSession>>(emptyList())
    private val sets = MutableStateFlow<List<WorkoutSet>>(emptyList())

    override fun getExercises(): Flow<List<Exercise>> = exercises.asStateFlow()

    override suspend fun ensureDefaultExercises() {
        // default already initialized
    }

    override fun getActiveSession(): Flow<WorkoutSession?> = activeSession.asStateFlow()

    override fun getCompletedSessions(): Flow<List<WorkoutSession>> = completedSessions.asStateFlow()

    override suspend fun getSessionById(sessionId: String): WorkoutSession? =
        completedSessions.value.firstOrNull { it.id == sessionId } ?: activeSession.value?.takeIf { it.id == sessionId }

    override fun getSetsForSession(sessionId: String): Flow<List<WorkoutSet>> =
        sets.map { list -> list.filter { it.sessionId == sessionId } }

    override suspend fun startWorkout(title: String): WorkoutSession {
        val existing = activeSession.value
        if (existing != null) return existing
        val session = WorkoutSession(
            id = "sess-1",
            title = title,
            startTimeEpochMs = clock.nowMillis(),
            isCompleted = false
        )
        activeSession.value = session
        return session
    }

    override suspend fun addSet(
        sessionId: String,
        exerciseId: String,
        weightKg: Double,
        reps: Int,
        rpe: Double?
    ): WorkoutSet {
        val currentSets = sets.value.filter { it.sessionId == sessionId && it.exerciseId == exerciseId }
        val nextNumber = currentSets.size + 1
        val set = WorkoutSet(
            id = "set-${sets.value.size + 1}",
            sessionId = sessionId,
            exerciseId = exerciseId,
            setNumber = nextNumber,
            weightKg = weightKg,
            reps = reps,
            rpe = rpe,
            isCompleted = false
        )
        sets.value = sets.value + set
        return set
    }

    override suspend fun updateSet(set: WorkoutSet) {
        sets.value = sets.value.map { if (it.id == set.id) set else it }
    }

    override suspend fun deleteSet(setId: String) {
        sets.value = sets.value.filter { it.id != setId }
    }

    override suspend fun completeWorkout(sessionId: String): WorkoutSession? {
        val session = activeSession.value ?: return null
        val totalVolume = sets.value.filter { it.sessionId == sessionId && it.isCompleted }.sumOf { it.weightKg * it.reps }
        val completed = session.copy(isCompleted = true, endTimeEpochMs = clock.nowMillis(), totalVolumeKg = totalVolume)
        completedSessions.value = completedSessions.value + completed
        activeSession.value = null
        return completed
    }

    override suspend fun cancelWorkout(sessionId: String) {
        activeSession.value = null
        sets.value = sets.value.filter { it.sessionId != sessionId }
    }

    override suspend fun getLastPerformance(exerciseId: String): String? = "80 kg × 8"
}
