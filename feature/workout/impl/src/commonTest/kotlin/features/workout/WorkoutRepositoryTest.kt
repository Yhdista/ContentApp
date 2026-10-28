package features.workout

import core.storage.CommandCoordinator
import core.common.AppClock
import core.database.MutationDao
import core.database.SyncMutationEntity
import features.workout.data.OfflineFirstWorkoutRepository
import features.workout.data.dao.WorkoutDao
import features.workout.data.datasource.RoomWorkoutLocalDataSource
import features.exercises.api.Exercise
import features.exercises.api.ExerciseCatalog
import features.workout.data.entity.WorkoutSessionEntity
import features.workout.data.entity.WorkoutSetEntity
import features.workout.domain.WorkoutRepository
import features.workout.domain.WorkoutSetType
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

    private lateinit var fakeCatalog: FakeExerciseCatalog
    private lateinit var fakeDao: FakeWorkoutDao
    private lateinit var fakeMutationDao: FakeMutationDao
    private lateinit var fakeClock: RepoFakeClock
    private lateinit var repository: WorkoutRepository

    @BeforeTest
    fun setup() {
        fakeDao = FakeWorkoutDao()
        fakeCatalog = FakeExerciseCatalog()
        fakeMutationDao = FakeMutationDao()
        fakeClock = RepoFakeClock(time = 1000000L)
        repository = OfflineFirstWorkoutRepository(
            localDataSource = RoomWorkoutLocalDataSource(fakeDao, fakeCatalog),
            mutationDao = fakeMutationDao,
            clock = fakeClock,
            writes = directWrites
        )
    }

    private suspend fun complete(sessionId: String) =
        (features.workout.data.RoomWorkoutStore(fakeDao, directWrites, fakeMutationDao, fakeClock).completeOnce(sessionId)
            as core.result.Result.Success).data.session

    @Test
    fun testEnsureDefaultExercisesSeedsWhenEmpty() = runTest {
        assertEquals(0, fakeCatalog.getExercisesCount())
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
    fun testGetSessionById() = runTest {
        val session = repository.startWorkout("Leg Day")
        val fetched = repository.getSessionById(session.id)
        assertNotNull(fetched)
        assertEquals(session.id, fetched.id)
        assertEquals("Leg Day", fetched.title)
    }

    @Test
    fun testCreateCustomExerciseInsertsEntityAndLogsMutation() = runTest {
        val created = repository.createCustomExercise(
            name = "Incline Dumbbell Press",
            muscleGroup = features.exercises.api.MuscleGroup.CHEST,
            defaultRestSeconds = 90
        )
        assertNotNull(created.id)
        assertEquals("Incline Dumbbell Press", created.name)
        assertEquals(features.exercises.api.MuscleGroup.CHEST, created.muscleGroup)

        val exercises = repository.getExercises().first()
        assertTrue(exercises.any { it.id == created.id })

        val mutations = fakeMutationDao.mutations
        assertTrue(mutations.any { it.recordId == created.id && it.entityName == "exercise" })
    }

    @Test
    fun testGetPendingSyncMutationsCountReflectsOutbox() = runTest {
        assertEquals(0, repository.getPendingSyncMutationsCount().first())

        val session = repository.startWorkout("Back Day")
        assertTrue(repository.getPendingSyncMutationsCount().first() > 0)
    }

    @Test
    fun testAddAndCompleteSetCalculatesVolumeAndLogsMutation() = runTest {
        val session = repository.startWorkout("Leg Day")
        val set1 = repository.addSet(sessionId = session.id, exerciseId = "ex_squat", weightKg = 100.0, reps = 5)
        val set2 = repository.addSet(sessionId = session.id, exerciseId = "ex_squat", weightKg = 100.0, reps = 5)

        repository.updateSet(set1.id) { it.copy(isCompleted = true) }
        repository.updateSet(set2.id) { it.copy(isCompleted = true) }

        val completed = complete(session.id)
        assertNotNull(completed)
        assertTrue(completed.isCompleted)
        assertEquals(1000.0, completed.totalVolumeKg) // (100 * 5) + (100 * 5)

        // Ověření zápisu do sync_mutations outboxu
        val mutations = fakeMutationDao.mutations
        assertTrue(mutations.any { it.recordId == session.id && it.operation == "COMPLETE" })
    }

    @Test
    fun testUpdateSetDetectsPersonalRecord() = runTest {
        // Sezení 1: Bench 80 kg × 8 reps (~101.3 kg 1RM)
        val session1 = repository.startWorkout("Bench Day 1")
        val set1 = repository.addSet(session1.id, "ex_bench", 80.0, 8)
        repository.updateSet(set1.id) { it.copy(isCompleted = true) }
        complete(session1.id)

        // Sezení 2: Bench 90 kg × 8 reps (~114 kg 1RM) -> Překonává sezení 1 -> PR!
        val session2 = repository.startWorkout("Bench Day 2")
        val set2 = repository.addSet(session2.id, "ex_bench", 90.0, 8)
        repository.updateSet(set2.id) { it.copy(isCompleted = true) }

        val setsSess2 = repository.getSetsForSession(session2.id).first()
        val completedSet2 = setsSess2.first { it.id == set2.id }
        assertTrue(completedSet2.isPersonalRecord)

        // Sezení 3: Bench 70 kg × 5 reps (~81.6 kg 1RM) -> Menší než minulé výkony -> Není PR
        val session3 = repository.startWorkout("Bench Day 3")
        val set3 = repository.addSet(session3.id, "ex_bench", 70.0, 5)
        repository.updateSet(set3.id) { it.copy(isCompleted = true) }

        val setsSess3 = repository.getSetsForSession(session3.id).first()
        val completedSet3 = setsSess3.first { it.id == set3.id }
        kotlin.test.assertFalse(completedSet3.isPersonalRecord)
    }

    @Test
    fun testWarmupSetNeverMarkedAsPersonalRecord() = runTest {
        val session = repository.startWorkout("Bench Day")
        val set = repository.addSet(session.id, "ex_bench", 150.0, 10)
        repository.updateSet(set.id) { it.copy(isCompleted = true, setType = WorkoutSetType.WARMUP) }

        val sets = repository.getSetsForSession(session.id).first()
        val completedSet = sets.first { it.id == set.id }
        kotlin.test.assertFalse(completedSet.isPersonalRecord)
    }

    @Test
    fun testHeavyWarmupSetDoesNotBlockSubsequentWorkingSetPersonalRecord() = runTest {
        // Sezení 1: Těžký zahřívací set (150 kg × 10 = 200 kg 1RM), který nesmí zablokovat pracovní PR!
        val session1 = repository.startWorkout("Session 1")
        val warmupSet = repository.addSet(session1.id, "ex_bench", 150.0, 10)
        repository.updateSet(warmupSet.id) { it.copy(isCompleted = true, setType = WorkoutSetType.WARMUP) }

        // Následný pracovní set ve stejném sezení (100 kg × 5 = ~116.7 kg 1RM) -> Musí být uznán jako PR!
        val workingSet1 = repository.addSet(session1.id, "ex_bench", 100.0, 5)
        repository.updateSet(workingSet1.id) { it.copy(isCompleted = true, setType = WorkoutSetType.NORMAL) }

        val setsSess1 = repository.getSetsForSession(session1.id).first()
        assertTrue(setsSess1.first { it.id == workingSet1.id }.isPersonalRecord)
        complete(session1.id)

        // Sezení 2: Pracovní set (105 kg × 5 = 122.5 kg 1RM) překonává předchozí pracovní 100×5, ačkoliv je nižší než starý warmup 150×10
        val session2 = repository.startWorkout("Session 2")
        val workingSet2 = repository.addSet(session2.id, "ex_bench", 105.0, 5)
        repository.updateSet(workingSet2.id) { it.copy(isCompleted = true, setType = WorkoutSetType.NORMAL) }

        val setsSess2 = repository.getSetsForSession(session2.id).first()
        assertTrue(setsSess2.first { it.id == workingSet2.id }.isPersonalRecord)
    }

    @Test
    fun testOutboxPayloadsAreValidJsonWithEscapedSpecialCharacters() = runTest {
        val created = repository.createCustomExercise(
            name = "Bench \"Heavy\" \\ Press",
            muscleGroup = features.exercises.api.MuscleGroup.CHEST,
            defaultRestSeconds = 120
        )
        val mutation = fakeMutationDao.mutations.first { it.recordId == created.id }
        assertTrue(mutation.payloadJson.contains("\\\"Heavy\\\""))
        val decoded = kotlinx.serialization.json.Json.decodeFromString<features.workout.data.model.ExerciseSyncPayload>(mutation.payloadJson)
        assertEquals("Bench \"Heavy\" \\ Press", decoded.name)
    }
}

private class RepoFakeClock(var time: Long) : AppClock {
    override fun nowMillis(): Long = time
}

private class FakeWorkoutDao : WorkoutDao {
    private val targets = MutableStateFlow<List<features.workout.data.entity.WorkoutPrescriptionSetEntity>>(emptyList())
    override suspend fun insertPrescription(target: features.workout.data.entity.WorkoutPrescriptionSetEntity) {
        check(targets.value.none { it.id == target.id }); targets.value += target
    }
    override fun observePrescription(sessionId: String) = targets.map { rows -> rows.filter { it.sessionId == sessionId } }
    override suspend fun getAllPrescriptions() = targets.value
    override suspend fun deletePrescriptionForSession(sessionId: String) { targets.value = targets.value.filterNot { it.sessionId == sessionId } }

    private val sessions = MutableStateFlow<List<WorkoutSessionEntity>>(emptyList())
    private val sets = MutableStateFlow<List<WorkoutSetEntity>>(emptyList())
    private val entries = MutableStateFlow<List<features.workout.data.entity.WorkoutExerciseEntity>>(emptyList())
    override suspend fun getAllSetsSnapshot() = sets.value
    override suspend fun getAllExercisesSnapshot() = entries.value
    override suspend fun getExercisesSnapshot(sessionId: String) = entries.value.filter { it.sessionId == sessionId }
    override fun observeExercises(sessionId: String) = entries.map { list -> list.filter { it.sessionId == sessionId } }
    override suspend fun insertExercise(entry: features.workout.data.entity.WorkoutExerciseEntity) { entries.value += entry }
    override suspend fun deleteExercises(sessionId: String) { entries.value = entries.value.filterNot { it.sessionId == sessionId } }
    override suspend fun deleteExercise(id: String) { entries.value = entries.value.filterNot { it.id == id } }

    override fun getActiveSession(): Flow<WorkoutSessionEntity?> =
        sessions.map { list -> list.firstOrNull { !it.isCompleted } }

    override suspend fun getActiveSessionSync(): WorkoutSessionEntity? =
        sessions.value.firstOrNull { !it.isCompleted }

    override fun getCompletedSessions(): Flow<List<WorkoutSessionEntity>> =
        sessions.map { list -> list.filter { it.isCompleted }.sortedByDescending { it.startTimeEpochMs } }

    override suspend fun getSessionById(sessionId: String): WorkoutSessionEntity? =
        sessions.value.firstOrNull { it.id == sessionId }

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

    override suspend fun getHistoricalCompletedSets(exerciseId: String, currentSessionId: String): List<WorkoutSetEntity> =
        sets.value.filter { it.exerciseId == exerciseId && it.isCompleted && it.sessionId != currentSessionId }

    override suspend fun getSetById(setId: String): WorkoutSetEntity? = sets.value.firstOrNull { it.id == setId }
    override suspend fun getSetsForSessionSnapshot(sessionId: String): List<WorkoutSetEntity> = sets.value.filter { it.sessionId == sessionId }

    override suspend fun getAllSessionsList(): List<WorkoutSessionEntity> = sessions.value
    override fun getAllCompletedSetsForExercise(exerciseId: String): Flow<List<WorkoutSetEntity>> =
        sets.map { list -> list.filter { it.exerciseId == exerciseId && it.isCompleted } }
    override suspend fun getAllCompletedSets(): List<WorkoutSetEntity> =
        sets.value.filter { it.isCompleted }
}

private class FakeMutationDao : MutationDao {
    val mutations = mutableListOf<SyncMutationEntity>()
    private val flow = MutableStateFlow<List<SyncMutationEntity>>(emptyList())

    override fun getPendingMutations(maxRetries: Int): Flow<List<SyncMutationEntity>> = flow.map { rows -> rows.filter { it.retryCount < maxRetries } }
    override fun getUnsentMutationsCount(): Flow<Int> = flow.map { it.size }

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

private class FakeExerciseCatalog : ExerciseCatalog {
    private val exercises = MutableStateFlow<List<Exercise>>(emptyList())
    override fun observeExercises(): Flow<List<Exercise>> = exercises.asStateFlow()
    override suspend fun getAllExercises(): List<Exercise> = exercises.value
    override suspend fun getExerciseById(id: String): Exercise? = exercises.value.firstOrNull { it.id == id }
    override suspend fun getExercisesCount(): Int = exercises.value.size
    override suspend fun saveExercise(exercise: Exercise) { exercises.value = exercises.value.filter { it.id != exercise.id } + exercise }
    override suspend fun saveExercises(exercises: List<Exercise>) { exercises.forEach { saveExercise(it) } }
}

private val directWrites = object : CommandCoordinator { override suspend fun <T> write(block: suspend () -> T): T = block() }
