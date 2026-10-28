package features.workout

import core.common.AppClock
import features.workout.detail.WorkoutDetailContract
import features.workout.detail.WorkoutDetailViewModel
import features.exercises.api.Exercise
import features.exercises.api.MuscleGroup
import features.workout.domain.WorkoutRepository
import features.workout.domain.WorkoutSession
import features.workout.domain.WorkoutSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: DetailFakeWorkoutRepository
    private lateinit var viewModel: WorkoutDetailViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = DetailFakeWorkoutRepository()
        viewModel = WorkoutDetailViewModel(fakeRepository, features.workout.domain.CreateRoutineDraft { core.result.Result.Success("workout:$it") })
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadDetailLoadsSessionAndGroupsSetsByExercise() = runTest(testDispatcher) {
        val sessionId = "session-100"
        val session = WorkoutSession(
            id = sessionId,
            title = "Push Day",
            startTimeEpochMs = 1000000L,
            endTimeEpochMs = 1600000L, // 600s = 10m 0s
            isCompleted = true,
            totalVolumeKg = 1200.0
        )
        fakeRepository.setSession(session)

        val set1 = WorkoutSet(
            id = "set-1",
            sessionId = sessionId,
            exerciseId = fakeRepository.benchExercise.id,
            setNumber = 1,
            weightKg = 60.0,
            reps = 10,
            isCompleted = true
        )
        val set2 = WorkoutSet(
            id = "set-2",
            sessionId = sessionId,
            exerciseId = fakeRepository.benchExercise.id,
            setNumber = 2,
            weightKg = 60.0,
            reps = 10,
            isCompleted = true
        )
        fakeRepository.addSets(listOf(set1, set2))

        viewModel.load(sessionId)
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.session)
        assertEquals("Push Day", state.session?.title)
        assertEquals("10m 0s", state.durationFormatted)
        assertEquals(1, state.exercisesWithSets.size)

        val benchGroup = state.exercisesWithSets.first()
        assertEquals("Bench press", benchGroup.exercise.name)
        assertEquals(2, benchGroup.sets.size)
        assertEquals(1200.0, benchGroup.exerciseVolumeKg)
    }

    @Test fun repeatedExerciseHistoryUsesIndependentKeysAndSnapshotNames() = runTest(testDispatcher) {
        fakeRepository.setSession(WorkoutSession("snapshot","History",1,isCompleted=true))
        fakeRepository.addSets(List(2) { index -> WorkoutSet("s$index","snapshot","ex_bench",1,20.0,8,
            workoutExerciseId="entry$index",exerciseNameSnapshot="Original $index",exerciseOrder=index) })
        viewModel.load("snapshot"); testScheduler.runCurrent()
        val entries=viewModel.uiState.value.exercisesWithSets
        assertEquals(listOf("entry0","entry1"),entries.map { it.entryId })
        assertEquals(listOf("Original 0","Original 1"),entries.map { it.exercise.name })
        assertTrue(entries.all { it.sets.size==1 })
    }

    @Test
    fun testBackClickedSendsNavigateBackEffect() = runTest(testDispatcher) {
        var effectReceived: WorkoutDetailContract.Effect? = null
        val job = launch {
            viewModel.effect.collect { effectReceived = it }
        }

        viewModel.onIntent(WorkoutDetailContract.Intent.BackClicked)
        testScheduler.runCurrent()

        assertEquals(WorkoutDetailContract.Effect.NavigateBack, effectReceived)
        job.cancel()
    }

    @Test
    fun testCreateRoutineOpensDraftAfterSuccess() = runTest(testDispatcher) {
        val session = WorkoutSession(
            id = "sess-1",
            title = "Bench Day",
            startTimeEpochMs = 1000L,
            endTimeEpochMs = 2000L,
            isCompleted = true,
            totalVolumeKg = 500.0
        )
        fakeRepository.setSession(session)
        viewModel.onIntent(WorkoutDetailContract.Intent.LoadDetail("sess-1"))
        testScheduler.runCurrent()

        var effectReceived: WorkoutDetailContract.Effect? = null
        val job = launch {
            viewModel.effect.collect { effectReceived = it }
        }

        viewModel.onIntent(WorkoutDetailContract.Intent.CreateRoutine)
        testScheduler.runCurrent()

        assertEquals(WorkoutDetailContract.Effect.RoutineDraft("workout:sess-1"), effectReceived)
        job.cancel()
    }
}

private class DetailFakeWorkoutRepository : WorkoutRepository {
    val benchExercise = Exercise("ex_bench", "Bench press", MuscleGroup.CHEST, defaultRestSeconds = 120)
    val squatExercise = Exercise("ex_squat", "Dřep", MuscleGroup.LEGS, defaultRestSeconds = 180)

    private val exercises = MutableStateFlow(listOf(benchExercise, squatExercise))
    private val sessions = MutableStateFlow<Map<String, WorkoutSession>>(emptyMap())
    private val sets = MutableStateFlow<List<WorkoutSet>>(emptyList())

    fun setSession(session: WorkoutSession) {
        sessions.value = sessions.value + (session.id to session)
    }

    fun addSets(newSets: List<WorkoutSet>) {
        sets.value = sets.value + newSets
    }

    override fun getExercises(): Flow<List<Exercise>> = exercises.asStateFlow()

    override suspend fun ensureDefaultExercises() {}

    override suspend fun createCustomExercise(
        name: String,
        muscleGroup: MuscleGroup,
        defaultRestSeconds: Int
    ): Exercise = error("Not needed")

    override fun getPendingSyncMutationsCount(): Flow<Int> = MutableStateFlow(0)

    override fun getActiveSession(): Flow<WorkoutSession?> = MutableStateFlow(null)

    override fun getCompletedSessions(): Flow<List<WorkoutSession>> =
        sessions.map { it.values.filter { s -> s.isCompleted } }

    override suspend fun getSessionById(sessionId: String): WorkoutSession? =
        sessions.value[sessionId]

    override fun getSetsForSession(sessionId: String): Flow<List<WorkoutSet>> =
        sets.map { list -> list.filter { it.sessionId == sessionId } }

    override suspend fun startWorkout(title: String): WorkoutSession = error("Not needed")
    override fun getWorkoutExercises(sessionId: String) = fakeWorkoutEntries(this,sessionId)
    override suspend fun addSetToEntry(sessionId: String, workoutExerciseId: String, weightKg: Double, reps: Int, rpe: Double?) = addSet(sessionId,workoutExerciseId,weightKg,reps,rpe)
    override suspend fun removeWorkoutExercise(sessionId: String, workoutExerciseId: String) = removeExerciseFromWorkout(sessionId,workoutExerciseId)
    override suspend fun addExerciseEntry(sessionId: String, exerciseId: String) = features.workout.domain.WorkoutExercise(
        exerciseId,sessionId,exerciseId,exerciseId,features.exercises.api.MuscleGroup.CHEST,90,0)
    override suspend fun addSet(sessionId: String, exerciseId: String, weightKg: Double, reps: Int, rpe: Double?): WorkoutSet = error("Not needed")
    override suspend fun updateSet(setId: String, update: (WorkoutSet) -> WorkoutSet): WorkoutSet = error("Not used")
    override suspend fun removeExerciseFromWorkout(sessionId: String, exerciseId: String) {}

    override suspend fun deleteSet(setId: String) {}
    override suspend fun completeWorkout(sessionId: String): WorkoutSession? = null
    override suspend fun cancelWorkout(sessionId: String) {}
    override suspend fun getLastPerformance(exerciseId: String): String? = null
    override suspend fun getHistoricalMax1RM(exerciseId: String, currentSessionId: String): Double = 0.0

    override fun getExerciseStats(exerciseId: String): Flow<features.workout.domain.ExerciseStats?> = MutableStateFlow(null)
    override suspend fun getAllCompletedSets(): List<WorkoutSet> = sets.value.filter { it.isCompleted }
}
