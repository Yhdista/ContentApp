package features.workout

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flow
import core.common.AppClock
import core.common.ApplicationScope
import core.feedback.PlatformFeedback
import features.workout.active.ActiveWorkoutContract
import features.workout.active.ActiveWorkoutViewModel
import features.exercises.api.Exercise
import features.exercises.api.MuscleGroup
import features.workout.domain.WorkoutRepository
import features.workout.domain.WorkoutSession
import features.workout.domain.WorkoutSet
import features.workout.domain.WorkoutSetType
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

        viewModel = testActiveWorkoutViewModel(
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

    private fun testActiveWorkoutViewModel(workoutRepository: WorkoutRepository, clock: AppClock,
        platformFeedback: PlatformFeedback = core.feedback.NoOpPlatformFeedback(),
        completion: features.workout.domain.CompleteWorkout = features.workout.domain.CompleteWorkout { id ->
            core.result.Result.Success(features.workout.domain.CompletionOutcome(fakeRepository.complete(id)!!, true))
        }) = ActiveWorkoutViewModel(workoutRepository, clock, platformFeedback, completion, fakeRepository)

    @Test fun addingExerciseUsesOneAtomicCommand() = runTest(testDispatcher) {
        val session = fakeRepository.startWorkout("Atomic")
        var calls = 0
        val repo = object : WorkoutRepository by fakeRepository {
            override suspend fun addExerciseWithInitialSet(sessionId: String, exerciseId: String, weightKg: Double, reps: Int): features.workout.domain.WorkoutExercise {
                calls++
                assertEquals(session.id, sessionId)
                assertEquals(fakeRepository.benchExercise.id, exerciseId)
                assertEquals(60.0, weightKg); assertEquals(10, reps)
                return fakeRepository.addExerciseWithInitialSet(sessionId, exerciseId, weightKg, reps)
            }
            override suspend fun addSetToEntry(sessionId: String, workoutExerciseId: String, weightKg: Double, reps: Int, rpe: Double?): WorkoutSet = error("UI must not issue a second write")
        }
        val vm = testActiveWorkoutViewModel(repo, fakeClock)
        try {
            testScheduler.runCurrent()
            vm.onIntent(ActiveWorkoutContract.Intent.OpenExercisePicker(true))
            vm.onIntent(ActiveWorkoutContract.Intent.AddExerciseToWorkout(fakeRepository.benchExercise))
            testScheduler.runCurrent()
            assertEquals(1, calls)
            assertEquals(1, fakeRepository.getSetsList().size)
            assertFalse(vm.uiState.value.isPickerVisible)
        } finally { vm.viewModelScope.cancel(); viewModel.cancelTimers() }
    }

    @Test fun observerFailureCannotUnlockPendingFinish() = runTest(testDispatcher) {
        val session = fakeRepository.startWorkout("Pending")
        val failObservation = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        var finishCalls = 0
        val repository = object : WorkoutRepository by fakeRepository {
            override fun getExercises() = flow {
                emit(listOf(fakeRepository.benchExercise))
                failObservation.await()
                error("Controlled observation failure")
            }
        }
        val vm = ActiveWorkoutViewModel(repository, fakeClock, catalogue = fakeRepository, completeWorkout = features.workout.domain.CompleteWorkout {
            finishCalls++
            finish.await()
            core.result.Result.Success(features.workout.domain.CompletionOutcome(session.copy(isCompleted = true), true))
        })
        try {
            testScheduler.runCurrent()
            vm.onIntent(ActiveWorkoutContract.Intent.FinishWorkout); testScheduler.runCurrent()
            assertTrue(vm.uiState.value.isSaving)
            failObservation.complete(Unit); testScheduler.runCurrent()
            assertNotNull(vm.uiState.value.error)
            assertTrue(vm.uiState.value.isSaving, "Observer failure must not release the pending command")
            vm.onIntent(ActiveWorkoutContract.Intent.FinishWorkout); testScheduler.runCurrent()
            assertEquals(1, finishCalls)
            finish.complete(Unit); testScheduler.runCurrent()
            assertFalse(vm.uiState.value.isSaving)
        } finally { vm.viewModelScope.cancel(); viewModel.cancelTimers() }
    }

    @Test fun repeatedEntriesLoadIndependentlyAndCommandsUseEntryIdentity() = runTest(testDispatcher) {
        val session=fakeRepository.startWorkout("Repeated")
        val entries=List(2) { i -> features.workout.domain.WorkoutExercise("entry$i",session.id,"ex_bench","Snapshot $i",MuscleGroup.CHEST,40+i,i) }
        val sets=entries.map { e -> WorkoutSet("set${e.id}",session.id,"ex_bench",1,20.0,8,workoutExerciseId=e.id,restSecondsSnapshot=e.restSeconds) }
        var added: String?=null; var removed: String?=null
        val repo=object: WorkoutRepository by fakeRepository {
            override fun getWorkoutExercises(sessionId: String)=kotlinx.coroutines.flow.flowOf(entries)
            override fun getSetsForSession(sessionId: String)=kotlinx.coroutines.flow.flowOf(sets)
            override suspend fun addSetToEntry(sessionId: String,workoutExerciseId: String,weightKg: Double,reps: Int,rpe: Double?): WorkoutSet { added=workoutExerciseId; return sets.first() }
            override suspend fun removeWorkoutExercise(sessionId: String,workoutExerciseId: String) { removed=workoutExerciseId }
            override suspend fun updateSet(setId: String,update: (WorkoutSet)->WorkoutSet)=update(sets.first { it.id==setId })
        }
        val vm=testActiveWorkoutViewModel(repo,fakeClock)
        try {
            testScheduler.runCurrent()
            assertEquals(entries,vm.uiState.value.workoutExercises)
            vm.onIntent(ActiveWorkoutContract.Intent.AddSet("entry1")); testScheduler.runCurrent(); assertEquals("entry1",added)
            vm.onIntent(ActiveWorkoutContract.Intent.ToggleSetComplete(sets[1])); testScheduler.runCurrent()
            assertEquals(41,vm.uiState.value.restTimerTotalSeconds)
            vm.onIntent(ActiveWorkoutContract.Intent.RemoveExerciseFromWorkout("entry0")); testScheduler.runCurrent(); assertEquals("entry0",removed)
        } finally { vm.cancelTimers(); viewModel.cancelTimers() }
    }

    @Test
    fun failedFinishKeepsSessionAndExposesRecoverableError() = runTest(testDispatcher) {
        fakeRepository.startWorkout("Keep editing")
        val vm = testActiveWorkoutViewModel(fakeRepository, fakeClock,
            completion = features.workout.domain.CompleteWorkout { error("Disk unavailable") })
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
        val vm = testActiveWorkoutViewModel(fakeRepository, fakeClock,
            completion = features.workout.domain.CompleteWorkout { sessionId ->
                release.await()
                core.result.Result.Success(features.workout.domain.CompletionOutcome(fakeRepository.complete(sessionId)!!, true))
            })
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
        val vm = testActiveWorkoutViewModel(
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

private class FakeWorkoutRepository(private val clock: AppClock) : WorkoutRepository, features.exercises.api.ExerciseCatalog {
    val benchExercise = Exercise("ex_bench", "Bench press", MuscleGroup.CHEST, defaultRestSeconds = 120)
    val squatExercise = Exercise("ex_squat", "Dřep", MuscleGroup.LEGS, defaultRestSeconds = 180)

    private val exercises = MutableStateFlow(listOf(benchExercise, squatExercise))
    private val activeSession = MutableStateFlow<WorkoutSession?>(null)
    private val completedSessions = MutableStateFlow<List<WorkoutSession>>(emptyList())
    private val sets = MutableStateFlow<List<WorkoutSet>>(emptyList())

    fun getSetsList(): List<WorkoutSet> = sets.value

    override fun getExercises(): Flow<List<Exercise>> = exercises.asStateFlow()

    override fun observeExercises(): Flow<List<Exercise>> = exercises
    override suspend fun getAllExercises(): List<Exercise> = exercises.value
    override suspend fun getExerciseById(id: String): Exercise? = exercises.value.find { it.id == id }
    override suspend fun getExercisesCount(): Int = exercises.value.size
    override suspend fun update(exercise: Exercise): Exercise = error("Unused")
    override suspend fun archive(id: String) = error("Unused")

    override suspend fun create(
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

    override fun getPrescriptionForSession(sessionId: String) = kotlinx.coroutines.flow.flowOf(emptyList<features.workout.domain.WorkoutPrescriptionSet>())
    override fun getWorkoutExercises(sessionId: String) = fakeWorkoutEntries(this,sessionId)
    override suspend fun addSetToEntry(sessionId: String, workoutExerciseId: String, weightKg: Double, reps: Int, rpe: Double?) = addSet(sessionId,workoutExerciseId,weightKg,reps,rpe)
    override suspend fun removeWorkoutExercise(sessionId: String, workoutExerciseId: String) = removeExerciseFromWorkout(sessionId,workoutExerciseId)
    override suspend fun addExerciseWithInitialSet(sessionId: String, exerciseId: String, weightKg: Double, reps: Int): features.workout.domain.WorkoutExercise {
        addSet(sessionId, exerciseId, weightKg, reps)
        return features.workout.domain.WorkoutExercise(exerciseId, sessionId, exerciseId, exerciseId, MuscleGroup.CHEST, 90, 0)
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

    suspend fun complete(sessionId: String): WorkoutSession? {
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

    override fun getExerciseStats(exerciseId: String): Flow<features.workout.domain.ExerciseStats?> = MutableStateFlow(null)
    override suspend fun getAllCompletedSets(): List<WorkoutSet> = sets.value.filter { it.isCompleted }
}
