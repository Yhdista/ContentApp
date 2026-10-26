package features.workout

import core.common.AppClock
import core.database.MutationDao
import core.database.SyncMutationEntity
import features.workout.data.WorkoutRepositoryImpl
import features.workout.data.dao.WorkoutDao
import features.workout.data.entity.ExerciseEntity
import features.workout.data.entity.WorkoutSessionEntity
import features.workout.data.entity.WorkoutSetEntity
import features.workout.domain.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WorkoutRepositoryTest {

    private lateinit var fakeDao: FakeWorkoutDao
    private lateinit var fakeMutationDao: FakeMutationDao
    private lateinit var fakeClock: RepoFakeClock
    private lateinit var repository: WorkoutRepository

    @BeforeTest
    fun setup() {
        fakeDao = FakeWorkoutDao()
        fakeMutationDao = FakeMutationDao()
        fakeClock = RepoFakeClock(time = 1000000L)
        repository = WorkoutRepositoryImpl(
            workoutDao = fakeDao,
            mutationDao = fakeMutationDao,
            clock = fakeClock
        )
    }

    @Test
    fun testEnsureDefaultExercisesSeedsWhenEmpty() = runTest {
        assertEquals(0, fakeDao.getExercisesCount())
        repository.ensureDefaultExercises()
        val exercises = repository.getExercises().first()
        assertTrue(exercises.isNotEmpty())
        assertTrue(exercises.any { it.name == "Bench press" })
        assertTrue(exercises.any { it.name == "Dřep s činkou" })
    }

    @Test
    fun testStartWorkoutCreatesActiveSession() = runTest {
        val session = repository.startWorkout("Nohy a břicho")
        assertEquals("Nohy a břicho", session.title)
        assertEquals(1000000L, session.startTimeEpochMs)

        val active = repository.getActiveSession().first()
        assertNotNull(active)
        assertEquals(session.id, active.id)
    }

    @Test
    fun testAddAndCompleteSetCalculatesVolumeAndLogsMutation() = runTest {
        val session = repository.startWorkout("Leg Day")
        val set1 = repository.addSet(sessionId = session.id, exerciseId = "ex_squat", weightKg = 100.0, reps = 5)
        val set2 = repository.addSet(sessionId = session.id, exerciseId = "ex_squat", weightKg = 100.0, reps = 5)

        repository.updateSet(set1.copy(isCompleted = true))
        repository.updateSet(set2.copy(isCompleted = true))

        val completed = repository.completeWorkout(session.id)
        assertNotNull(completed)
        assertTrue(completed.isCompleted)
        assertEquals(1000.0, completed.totalVolumeKg) // (100 * 5) + (100 * 5)

        // Ověření zápisu do sync_mutations outboxu
        val mutations = fakeMutationDao.mutations
        assertTrue(mutations.any { it.recordId == session.id && it.operation == "COMPLETE" })
    }
}

private class RepoFakeClock(var time: Long) : AppClock {
    override fun nowMillis(): Long = time
}

private class FakeWorkoutDao : WorkoutDao {
    private val exercises = MutableStateFlow<List<ExerciseEntity>>(emptyList())
    private val sessions = MutableStateFlow<List<WorkoutSessionEntity>>(emptyList())
    private val sets = MutableStateFlow<List<WorkoutSetEntity>>(emptyList())

    override fun getAllExercises(): Flow<List<ExerciseEntity>> = exercises.asStateFlow()

    override suspend fun getExerciseById(id: String): ExerciseEntity? =
        exercises.value.firstOrNull { it.id == id }

    override suspend fun insertExercises(newExercises: List<ExerciseEntity>) {
        exercises.value = exercises.value + newExercises
    }

    override suspend fun getExercisesCount(): Int = exercises.value.size

    override fun getActiveSession(): Flow<WorkoutSessionEntity?> =
        sessions.map { list -> list.firstOrNull { !it.isCompleted } }

    override fun getCompletedSessions(): Flow<List<WorkoutSessionEntity>> =
        sessions.map { list -> list.filter { it.isCompleted }.sortedByDescending { it.startTimeEpochMs } }

    override suspend fun insertSession(session: WorkoutSessionEntity) {
        sessions.value = sessions.value.filter { it.id != session.id } + session
    }

    override suspend fun completeSession(sessionId: String, endTime: Long, volume: Double) {
        sessions.value = sessions.value.map {
            if (it.id == sessionId) it.copy(isCompleted = true, endTimeEpochMs = endTime, totalVolumeKg = volume) else it
        }
    }

    override suspend fun deleteSession(sessionId: String) {
        sessions.value = sessions.value.filter { it.id != sessionId }
    }

    override fun getSetsForSession(sessionId: String): Flow<List<WorkoutSetEntity>> =
        sets.map { list -> list.filter { it.sessionId == sessionId }.sortedWith(compareBy({ it.exerciseId }, { it.setNumber })) }

    override suspend fun insertOrUpdateSet(set: WorkoutSetEntity) {
        sets.value = sets.value.filter { it.id != set.id } + set
    }

    override suspend fun deleteSet(setId: String) {
        sets.value = sets.value.filter { it.id != setId }
    }

    override suspend fun deleteSetsForSession(sessionId: String) {
        sets.value = sets.value.filter { it.sessionId != sessionId }
    }

    override suspend fun getLastPerformanceForExercise(exerciseId: String): WorkoutSetEntity? =
        sets.value.filter { it.exerciseId == exerciseId && it.isCompleted }.maxByOrNull { it.id }
}

private class FakeMutationDao : MutationDao {
    val mutations = mutableListOf<SyncMutationEntity>()
    private val flow = MutableStateFlow<List<SyncMutationEntity>>(emptyList())

    override fun getPendingMutations(): Flow<List<SyncMutationEntity>> = flow

    override suspend fun insertMutation(mutation: SyncMutationEntity) {
        mutations.add(mutation)
        flow.value = mutations.toList()
    }

    override suspend fun deleteMutation(id: String) {
        mutations.removeAll { it.id == id }
        flow.value = mutations.toList()
    }

    override suspend fun markError(id: String, error: String, retryCount: Int) {
        // no-op
    }
}
