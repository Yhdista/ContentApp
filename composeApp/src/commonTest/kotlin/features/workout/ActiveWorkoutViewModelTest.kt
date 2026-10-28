package features.workout

import core.common.AppClock
import core.common.ApplicationScope
import core.feedback.PlatformFeedback
import features.workout.active.ActiveWorkoutContract
import features.workout.active.ActiveWorkoutViewModel
import features.workout.domain.Exercise
import features.workout.domain.MuscleGroup
import features.workout.domain.WorkoutRepository
import features.workout.domain.WorkoutSession
import features.workout.domain.WorkoutSet
import features.workout.domain.WorkoutSetType
import features.workout.domain.WorkoutTemplate
import features.workout.domain.WorkoutTemplateExercise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
            clock = fakeClock
        )
    }

    @AfterTest
    fun tearDown() {
        viewModel.cancelTimers()
        viewModel.cancelTimers()
        appJob.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun failedFinishKeepsSessionAndExposesRecoverableError() = runTest(testDispatcher) {
        fakeRepository.startWorkout("Keep editing")
        val failing = object : WorkoutRepository by fakeRepository {
            override suspend fun completeWorkout(sessionId: String): WorkoutSession? = error("Disk unavailable")
        }
        val vm = ActiveWorkoutViewModel(failing, fakeClock)
        try {
            testScheduler.runCurrent()
            vm.onIntent(ActiveWorkoutContract.Intent.FinishWorkout)
            testScheduler.runCurrent()
            assertNotNull(vm.uiState.value.session)
            assertNotNull(vm.uiState.value.error)
            assertFalse(vm.uiState.value.isSaving)
            assertTrue(vm.effect.first() is ActiveWorkoutContract.Effect.ShowSnackbar)
        } finally { vm.cancelTimers(); viewModel.cancelTimers() }
    }

    @Test
    fun mutatingIntentsAreIgnoredWhileFinishIsPending() = runTest(testDispatcher) {
        val session = fakeRepository.startWorkout("Finish in progress")
        val set = fakeRepository.addSet(session.id, fakeRepository.benchExercise.id, 60.0, 10)
        val release = kotlinx.coroutines.CompletableDeferred<Unit>()
        val delayed = object : WorkoutRepository by fakeRepository {
            override suspend fun completeWorkout(sessionId: String): WorkoutSession? {
                release.await()
                return fakeRepository.completeWorkout(sessionId)
            }
        }
        val vm = ActiveWorkoutViewModel(delayed, fakeClock)
        try {
            testScheduler.runCurrent()
            vm.onIntent(ActiveWorkoutContract.Intent.FinishWorkout)
            testScheduler.runCurrent()
            assertTrue(vm.uiState.value.isSaving)
            vm.onIntent(ActiveWorkoutContract.Intent.UpdateWeight(set, 100.0))
            testScheduler.runCurrent()
            assertEquals(60.0, fakeRepository.getSetsForSession(session.id).first().single().weightKg)
            release.complete(Unit)
            testScheduler.runCurrent()
        } finally {
            release.complete(Unit)
            vm.cancelTimers()
            viewModel.cancelTimers()
        }
    }

    @Test
    fun editsFromSameUiSnapshotPreserveBothFields() = runTest(testDispatcher) {
        val session = fakeRepository.startWorkout("Concurrent edits")
        val set = fakeRepository.addSet(session.id, fakeRepository.benchExercise.id, 60.0, 10)
        viewModel.onIntent(ActiveWorkoutContract.Intent.LoadWorkout)
        testScheduler.runCurrent()
        viewModel.onIntent(ActiveWorkoutContract.Intent.UpdateWeight(set, 65.0))
        viewModel.onIntent(ActiveWorkoutContract.Intent.UpdateReps(set, 12))
        testScheduler.runCurrent()
        val saved = fakeRepository.getSetsForSession(session.id).first().single()
        viewModel.cancelTimers()
        assertEquals(65.0, saved.weightKg)
        assertEquals(12, saved.reps)
        viewModel.cancelTimers()
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

    @Test
    fun testCreateCustomExerciseAddsToAvailableAndWorkoutExercises() = runTest(testDispatcher) {
        fakeRepository.startWorkout("Arms Day")
        viewModel.onIntent(ActiveWorkoutContract.Intent.LoadWorkout)
        testScheduler.runCurrent()

        viewModel.onIntent(
            ActiveWorkoutContract.Intent.CreateCustomExercise(
                name = "Tricepsový klik",
                muscleGroup = MuscleGroup.ARMS,
                defaultRestSeconds = 60
            )
        )
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.workoutExercises.any { it.name == "Tricepsový klik" })
        assertTrue(state.availableExercises.any { it.name == "Tricepsový klik" })
        assertTrue(state.sets.any { set -> state.workoutExercises.any { it.id == set.exerciseId } })
        viewModel.cancelTimers()
    }

    @Test
    fun testDeleteSetUpdatesVisibleSets() = runTest(testDispatcher) {
        fakeRepository.startWorkout("Chest Day")
        viewModel.onIntent(ActiveWorkoutContract.Intent.LoadWorkout)
        testScheduler.runCurrent()

        val bench = fakeRepository.benchExercise
        viewModel.onIntent(ActiveWorkoutContract.Intent.AddExerciseToWorkout(bench))
        // Add second and third set
        viewModel.onIntent(ActiveWorkoutContract.Intent.AddSet(bench.id))
        viewModel.onIntent(ActiveWorkoutContract.Intent.AddSet(bench.id))
        testScheduler.runCurrent()

        val setsBefore = viewModel.uiState.value.sets
        assertEquals(3, setsBefore.size)
        assertEquals(listOf(1, 2, 3), setsBefore.map { it.setNumber })

        // Delete set 2
        val setToDelete = setsBefore[1]
        viewModel.onIntent(ActiveWorkoutContract.Intent.DeleteSet(setToDelete))
        testScheduler.runCurrent()

        val setsAfter = viewModel.uiState.value.sets
        assertEquals(2, setsAfter.size)
        assertEquals(listOf(setsBefore[0].id, setsBefore[2].id), setsAfter.map { it.id })
        viewModel.cancelTimers()
    }

    @Test
    fun testRestTimerCompletionTriggersFeedbackAndSnackbar() = runTest(testDispatcher) {
        val fakeFeedback = FakePlatformFeedback()
        val vm = ActiveWorkoutViewModel(
            workoutRepository = fakeRepository,
            clock = fakeClock,
            platformFeedback = fakeFeedback
        )
        testScheduler.runCurrent()

        var effectReceived: ActiveWorkoutContract.Effect? = null
        val job = backgroundScope.launch(testDispatcher) {
            vm.effect.collect { effectReceived = it }
        }

        // Start timer for 2 seconds
        vm.onIntent(ActiveWorkoutContract.Intent.StartRestTimer(2))
        testScheduler.runCurrent()
        assertTrue(vm.uiState.value.isRestTimerActive)
        assertEquals(2, vm.uiState.value.restTimerRemainingSeconds)

        // Advance 1s -> 1s remaining
        advanceTimeBy(1000)
        testScheduler.runCurrent()
        assertEquals(1, vm.uiState.value.restTimerRemainingSeconds)
        assertEquals(0, fakeFeedback.vibrateCount)

        // Advance 1s -> 0s remaining -> Triggers vibration, sound, and snackbar!
        advanceTimeBy(1000)
        testScheduler.runCurrent()
        assertEquals(0, vm.uiState.value.restTimerRemainingSeconds)
        assertFalse(vm.uiState.value.isRestTimerActive)
        assertEquals(1, fakeFeedback.vibrateCount)
        assertEquals(1, fakeFeedback.soundCount)
        assertTrue(effectReceived is ActiveWorkoutContract.Effect.ShowSnackbar)

        vm.cancelTimers()
        job.cancel()
    }

    @Test
    fun testOpenExerciseStatsUpdatesState() = runTest(testDispatcher) {
        val bench = Exercise("ex_bench", "Bench press", MuscleGroup.CHEST, 120)

        viewModel.onIntent(ActiveWorkoutContract.Intent.OpenExerciseStats(bench))
        testScheduler.runCurrent()

        assertEquals(bench, viewModel.uiState.value.selectedStatsExercise)

        viewModel.onIntent(ActiveWorkoutContract.Intent.OpenExerciseStats(null))
        testScheduler.runCurrent()

        kotlin.test.assertNull(viewModel.uiState.value.selectedStatsExercise)
        viewModel.cancelTimers()
    }

    @Test
    fun testChangeSetTypeUpdatesWorkoutSet() = runTest(testDispatcher) {
        fakeRepository.startWorkout()
        val set = fakeRepository.addSet("sess-1", "ex_bench", 100.0, 5)
        testScheduler.runCurrent()

        assertEquals(WorkoutSetType.NORMAL, fakeRepository.getSetsList().first().setType)

        viewModel.onIntent(ActiveWorkoutContract.Intent.ChangeSetType(set, WorkoutSetType.WARMUP))
        testScheduler.runCurrent()

        assertEquals(WorkoutSetType.WARMUP, fakeRepository.getSetsList().first().setType)

        viewModel.onIntent(ActiveWorkoutContract.Intent.ChangeSetType(set, WorkoutSetType.DROPSET))
        testScheduler.runCurrent()

        assertEquals(WorkoutSetType.DROPSET, fakeRepository.getSetsList().first().setType)

        viewModel.onIntent(ActiveWorkoutContract.Intent.ChangeSetType(set, WorkoutSetType.FAILURE))
        testScheduler.runCurrent()

        assertEquals(WorkoutSetType.FAILURE, fakeRepository.getSetsList().first().setType)
        viewModel.cancelTimers()
    }
}

private class FakePlatformFeedback : PlatformFeedback {
    var vibrateCount = 0
    var soundCount = 0

    override fun vibrateTimerComplete() {
        vibrateCount++
    }

    override fun playTimerSound() {
        soundCount++
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

    fun getSetsList(): List<WorkoutSet> = sets.value

    override fun getExercises(): Flow<List<Exercise>> = exercises.asStateFlow()

    override suspend fun ensureDefaultExercises() {
        // default already initialized
    }

    override suspend fun createCustomExercise(
        name: String,
        muscleGroup: MuscleGroup,
        defaultRestSeconds: Int
    ): Exercise {
        val ex = Exercise("ex_custom_${exercises.value.size + 1}", name, muscleGroup, defaultRestSeconds)
        exercises.value = exercises.value + ex
        return ex
    }

    override fun getPendingSyncMutationsCount(): Flow<Int> = MutableStateFlow(0)

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

    override suspend fun updateSet(setId: String, update: (WorkoutSet) -> WorkoutSet): WorkoutSet {
        val updated = update(sets.value.first { it.id == setId })
        sets.value = sets.value.map { if (it.id == setId) updated else it }
        return updated
    }

    override suspend fun removeExerciseFromWorkout(sessionId: String, exerciseId: String) {
        sets.value = sets.value.filterNot { it.sessionId == sessionId && it.exerciseId == exerciseId }
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
    override suspend fun getHistoricalMax1RM(exerciseId: String, currentSessionId: String): Double = 0.0

    override fun getTemplates(): Flow<List<WorkoutTemplate>> = MutableStateFlow(emptyList())
    override suspend fun ensureDefaultTemplates() {}
    override suspend fun createTemplate(name: String, description: String, exercises: List<WorkoutTemplateExercise>): WorkoutTemplate = error("Not needed")
    override suspend fun startWorkoutFromTemplate(templateId: String): WorkoutSession = startWorkout("From template")
    override suspend fun saveSessionAsTemplate(sessionId: String, templateName: String): WorkoutTemplate = error("Not needed")
    override fun getExerciseStats(exerciseId: String): Flow<features.workout.domain.ExerciseStats?> = MutableStateFlow(null)
    override suspend fun getAllCompletedSets(): List<WorkoutSet> = sets.value.filter { it.isCompleted }
}
