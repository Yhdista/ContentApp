package features.workout

import core.common.AppClock
import core.database.MutationDao
import core.database.SyncMutationEntity
import features.workout.data.OfflineFirstWorkoutRepository
import features.workout.data.dao.WorkoutDao
import features.workout.data.datasource.RoomWorkoutLocalDataSource
import features.workout.data.entity.ExerciseEntity
import features.workout.data.entity.WorkoutSessionEntity
import features.workout.data.entity.WorkoutSetEntity
import features.workout.data.entity.WorkoutTemplateEntity
import features.workout.data.entity.WorkoutTemplateExerciseEntity
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

    private lateinit var fakeDao: FakeWorkoutDao
    private lateinit var fakeMutationDao: FakeMutationDao
    private lateinit var fakeClock: RepoFakeClock
    private lateinit var repository: WorkoutRepository

    @BeforeTest
    fun setup() {
        fakeDao = FakeWorkoutDao()
        fakeMutationDao = FakeMutationDao()
        fakeClock = RepoFakeClock(time = 1000000L)
        repository = OfflineFirstWorkoutRepository(
            localDataSource = RoomWorkoutLocalDataSource(fakeDao),
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
            muscleGroup = features.workout.domain.MuscleGroup.CHEST,
            defaultRestSeconds = 90
        )
        assertNotNull(created.id)
        assertEquals("Incline Dumbbell Press", created.name)
        assertEquals(features.workout.domain.MuscleGroup.CHEST, created.muscleGroup)

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

    @Test
    fun testEnsureDefaultTemplatesSeedsWhenEmpty() = runTest {
        assertEquals(0, fakeDao.getTemplatesCount())
        repository.ensureDefaultTemplates()
        val templates = repository.getTemplates().first()
        assertTrue(templates.isNotEmpty())
        assertTrue(templates.any { it.name.startsWith("Push Day") })
        assertTrue(templates.any { it.name.startsWith("Pull Day") })
        assertTrue(templates.any { it.name.startsWith("Leg Day") })
    }

    @Test
    fun testStartWorkoutFromTemplatePrepopulatesExercisesAndSets() = runTest {
        repository.ensureDefaultTemplates()
        val templates = repository.getTemplates().first()
        val pushDay = templates.first { it.id == "tmpl_push" }

        val session = repository.startWorkoutFromTemplate(pushDay.id)
        assertNotNull(session)
        assertEquals(pushDay.name, session.title)

        val sets = repository.getSetsForSession(session.id).first()
        assertTrue(sets.isNotEmpty())
        // Push Day má 2 cviky po 3 sériích -> 6 sérií
        assertEquals(6, sets.size)
        assertTrue(sets.all { !it.isCompleted })
        assertEquals(listOf(1, 2, 3), sets.filter { it.exerciseId == "ex_bench" }.map { it.setNumber })
        assertEquals(listOf(1, 2, 3), sets.filter { it.exerciseId == "ex_ohp" }.map { it.setNumber })

        // Ověření zápisu všech 6 sérií ze šablony do Outboxu
        val setInsertMutations = fakeMutationDao.mutations.filter { it.entityName == "workout_set" && it.operation == "INSERT" }
        assertEquals(6, setInsertMutations.size)
    }

    @Test
    fun testSaveSessionAsTemplate() = runTest {
        val session = repository.startWorkout("Full Body")
        repository.addSet(session.id, "ex_bench", weightKg = 70.0, reps = 8)
        repository.addSet(session.id, "ex_bench", weightKg = 70.0, reps = 8)
        repository.addSet(session.id, "ex_squat", weightKg = 90.0, reps = 5)

        val template = repository.saveSessionAsTemplate(session.id, "Moje Full Body")
        assertNotNull(template)
        assertEquals("Moje Full Body", template.name)
        assertEquals(2, template.exercises.size)
        val benchEx = template.exercises.first { it.exerciseId == "ex_bench" }
        assertEquals(2, benchEx.targetSets)
        assertEquals(70.0, benchEx.defaultWeightKg)
        assertEquals(8, benchEx.defaultReps)
    }

    @Test
    fun testUpdateSetDetectsPersonalRecord() = runTest {
        // Sezení 1: Bench 80 kg × 8 reps (~101.3 kg 1RM)
        val session1 = repository.startWorkout("Bench Day 1")
        val set1 = repository.addSet(session1.id, "ex_bench", 80.0, 8)
        repository.updateSet(set1.copy(isCompleted = true))
        repository.completeWorkout(session1.id)

        // Sezení 2: Bench 90 kg × 8 reps (~114 kg 1RM) -> Překonává sezení 1 -> PR!
        val session2 = repository.startWorkout("Bench Day 2")
        val set2 = repository.addSet(session2.id, "ex_bench", 90.0, 8)
        repository.updateSet(set2.copy(isCompleted = true))

        val setsSess2 = repository.getSetsForSession(session2.id).first()
        val completedSet2 = setsSess2.first { it.id == set2.id }
        assertTrue(completedSet2.isPersonalRecord)

        // Sezení 3: Bench 70 kg × 5 reps (~81.6 kg 1RM) -> Menší než minulé výkony -> Není PR
        val session3 = repository.startWorkout("Bench Day 3")
        val set3 = repository.addSet(session3.id, "ex_bench", 70.0, 5)
        repository.updateSet(set3.copy(isCompleted = true))

        val setsSess3 = repository.getSetsForSession(session3.id).first()
        val completedSet3 = setsSess3.first { it.id == set3.id }
        kotlin.test.assertFalse(completedSet3.isPersonalRecord)
    }

    @Test
    fun testWarmupSetNeverMarkedAsPersonalRecord() = runTest {
        val session = repository.startWorkout("Bench Day")
        val set = repository.addSet(session.id, "ex_bench", 150.0, 10)
        repository.updateSet(set.copy(isCompleted = true, setType = WorkoutSetType.WARMUP))

        val sets = repository.getSetsForSession(session.id).first()
        val completedSet = sets.first { it.id == set.id }
        kotlin.test.assertFalse(completedSet.isPersonalRecord)
    }

    @Test
    fun testHeavyWarmupSetDoesNotBlockSubsequentWorkingSetPersonalRecord() = runTest {
        // Sezení 1: Těžký zahřívací set (150 kg × 10 = 200 kg 1RM), který nesmí zablokovat pracovní PR!
        val session1 = repository.startWorkout("Session 1")
        val warmupSet = repository.addSet(session1.id, "ex_bench", 150.0, 10)
        repository.updateSet(warmupSet.copy(isCompleted = true, setType = WorkoutSetType.WARMUP))

        // Následný pracovní set ve stejném sezení (100 kg × 5 = ~116.7 kg 1RM) -> Musí být uznán jako PR!
        val workingSet1 = repository.addSet(session1.id, "ex_bench", 100.0, 5)
        repository.updateSet(workingSet1.copy(isCompleted = true, setType = WorkoutSetType.NORMAL))

        val setsSess1 = repository.getSetsForSession(session1.id).first()
        assertTrue(setsSess1.first { it.id == workingSet1.id }.isPersonalRecord)
        repository.completeWorkout(session1.id)

        // Sezení 2: Pracovní set (105 kg × 5 = 122.5 kg 1RM) překonává předchozí pracovní 100×5, ačkoliv je nižší než starý warmup 150×10
        val session2 = repository.startWorkout("Session 2")
        val workingSet2 = repository.addSet(session2.id, "ex_bench", 105.0, 5)
        repository.updateSet(workingSet2.copy(isCompleted = true, setType = WorkoutSetType.NORMAL))

        val setsSess2 = repository.getSetsForSession(session2.id).first()
        assertTrue(setsSess2.first { it.id == workingSet2.id }.isPersonalRecord)
    }

    @Test
    fun testOutboxPayloadsAreValidJsonWithEscapedSpecialCharacters() = runTest {
        val created = repository.createCustomExercise(
            name = "Bench \"Heavy\" \\ Press",
            muscleGroup = features.workout.domain.MuscleGroup.CHEST,
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
    private val exercises = MutableStateFlow<List<ExerciseEntity>>(emptyList())
    private val sessions = MutableStateFlow<List<WorkoutSessionEntity>>(emptyList())
    private val sets = MutableStateFlow<List<WorkoutSetEntity>>(emptyList())
    private val templates = MutableStateFlow<List<WorkoutTemplateEntity>>(emptyList())
    private val templateExercises = MutableStateFlow<List<WorkoutTemplateExerciseEntity>>(emptyList())

    override fun getAllExercises(): Flow<List<ExerciseEntity>> = exercises.asStateFlow()

    override suspend fun getExerciseById(id: String): ExerciseEntity? =
        exercises.value.firstOrNull { it.id == id }

    override suspend fun insertExercises(exercises: List<ExerciseEntity>) {
        this.exercises.value = this.exercises.value + exercises
    }

    override suspend fun insertExercise(exercise: ExerciseEntity) {
        this.exercises.value = this.exercises.value.filter { it.id != exercise.id } + exercise
    }

    override suspend fun getExercisesCount(): Int = exercises.value.size

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

    override fun getAllTemplates(): Flow<List<WorkoutTemplateEntity>> = templates.asStateFlow()

    override fun getAllTemplateExercises(): Flow<List<WorkoutTemplateExerciseEntity>> = templateExercises.asStateFlow()

    override suspend fun getTemplateById(templateId: String): WorkoutTemplateEntity? =
        templates.value.firstOrNull { it.id == templateId }

    override suspend fun getTemplateExercises(templateId: String): List<WorkoutTemplateExerciseEntity> =
        templateExercises.value.filter { it.templateId == templateId }.sortedBy { it.orderIndex }

    override suspend fun getTemplatesCount(): Int = templates.value.size

    override suspend fun insertTemplate(template: WorkoutTemplateEntity) {
        templates.value = templates.value.filter { it.id != template.id } + template
    }

    override suspend fun insertTemplateExercises(exercises: List<WorkoutTemplateExerciseEntity>) {
        templateExercises.value = templateExercises.value + exercises
    }

    override suspend fun deleteTemplateExercises(templateId: String) {
        templateExercises.value = templateExercises.value.filter { it.templateId != templateId }
    }

    override suspend fun deleteTemplate(templateId: String) {
        templates.value = templates.value.filter { it.id != templateId }
    }

    override suspend fun getAllExercisesList(): List<ExerciseEntity> = exercises.value
    override suspend fun getAllSessionsList(): List<WorkoutSessionEntity> = sessions.value
    override fun getAllCompletedSetsForExercise(exerciseId: String): Flow<List<WorkoutSetEntity>> =
        sets.map { list -> list.filter { it.exerciseId == exerciseId && it.isCompleted } }
    override suspend fun getAllCompletedSets(): List<WorkoutSetEntity> =
        sets.value.filter { it.isCompleted }
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
