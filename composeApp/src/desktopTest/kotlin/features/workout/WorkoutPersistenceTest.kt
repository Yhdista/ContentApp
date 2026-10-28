package features.workout

import features.workout.domain.*
import features.workout.data.entity.WorkoutSetEntity
import features.workout.data.entity.WorkoutExerciseEntity
import kotlin.test.assertTrue
import androidx.sqlite.use
import androidx.room.Room
import androidx.room.RoomDatabase
import core.storage.CommandCoordinator
import core.storage.RoomCommandCoordinator
import features.workout.data.entity.WorkoutSessionEntity
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.database.AppDatabase
import app.database.configureAppDatabase
import app.database.AppDatabaseConstructor
import core.common.AppClock
import core.common.ApplicationScope
import core.database.MutationDao
import core.database.SyncMutationEntity
import features.workout.data.dao.WorkoutDao
import features.exercises.api.Exercise
import features.exercises.api.ExerciseCatalog
import features.exercises.api.MuscleGroup
import features.workout.domain.WorkoutRepository
import features.workout.workoutFeatureModule
import features.exercises.data.exerciseFeatureModule
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlinx.coroutines.CancellationException
import java.nio.file.Files
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class WorkoutPersistenceTest {
    private fun database() = Room.inMemoryDatabaseBuilder<AppDatabase> { AppDatabaseConstructor.initialize() }
        .configureAppDatabase().build()

    private fun TestScope.writes(db: AppDatabase) =
        RoomCommandCoordinator(db, ApplicationScope(backgroundScope.coroutineContext))

    private fun repository(
        db: AppDatabase,
        writes: RoomCommandCoordinator,
        outbox: MutationDao = db.mutationDao(),
        dao: WorkoutDao = db.workoutDao()
    ): WorkoutRepository {
        val application = koinApplication {
            modules(module {
                single { dao }
                single { outbox }
                single { db.exerciseDao() }
                single { db.routineDao() }
                single<CommandCoordinator> { writes }
                single<AppClock> { object : AppClock { override fun nowMillis() = 1000L } }
            }, exerciseFeatureModule(), features.routines.data.routineFeatureModule(), workoutFeatureModule())
        }
        return application.koin.get<WorkoutRepository>().also { application.close() }
    }

    @Test fun addingExerciseRollsBackEntryWhenInitialSetOutboxFails() = runTest {
        val db = database()
        try {
            val outbox = object : MutationDao by db.mutationDao() {
                override suspend fun insertMutation(mutation: SyncMutationEntity) {
                    if (mutation.entityName == "workout_set") error("Controlled second outbox failure")
                    db.mutationDao().insertMutation(mutation)
                }
            }
            val repo = repository(db, writes(db), outbox)
            val session = repo.startWorkout("Atomic add")
            val before = db.mutationDao().getUnsentMutationsCount().first()
            assertFailsWith<IllegalStateException> {
                repo.addExerciseWithInitialSet(session.id, "bench", 60.0, 10)
            }
            assertTrue(db.workoutDao().getExercisesSnapshot(session.id).isEmpty(), "Entry must roll back with its first set")
            assertTrue(db.workoutDao().getSetsForSessionSnapshot(session.id).isEmpty())
            assertEquals(before, db.mutationDao().getUnsentMutationsCount().first())
        } finally { db.close() }
    }

    @Test fun previousPerformanceUsesCompletedSessionChronologyInsteadOfSetId() = runTest {
        val db = database()
        try {
            val dao = db.workoutDao()
            dao.insertSession(WorkoutSessionEntity("old", "Old", 100L, 200L, true, 0.0))
            dao.insertSession(WorkoutSessionEntity("new", "New", 300L, 400L, true, 0.0))
            dao.insertOrUpdateSet(WorkoutSetEntity("zz-old", "old", "bench", 1, 10.0, 8, null, true, null))
            dao.insertOrUpdateSet(WorkoutSetEntity("aa-new", "new", "bench", 1, 20.0, 8, null, true, null))
            assertEquals("aa-new", dao.getLastPerformanceForExercise("bench")?.id)
        } finally { db.close() }
    }

    @Test fun acceptedExerciseAndInitialSetSurviveCancellationAndKeepOccurrencesIndependent() = runTest {
        val db = database()
        try {
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            var pause = true
            val outbox = object : MutationDao by db.mutationDao() {
                override suspend fun insertMutation(mutation: SyncMutationEntity) {
                    if (pause && mutation.entityName == "workout_exercise") {
                        pause = false
                        entered.complete(Unit)
                        release.await()
                    }
                    db.mutationDao().insertMutation(mutation)
                }
            }
            val coordinator = writes(db)
            val repo = repository(db, coordinator, outbox)
            val session = repo.startWorkout("Repeated")
            val before = db.mutationDao().getUnsentMutationsCount().first()
            val caller = launch { repo.addExerciseWithInitialSet(session.id, "bench", 60.0, 10) }
            entered.await(); caller.cancelAndJoin(); release.complete(Unit)
            coordinator.write { Unit }
            val second = repo.addExerciseWithInitialSet(session.id, "bench", 70.0, 8)
            val entries = db.workoutDao().getExercisesSnapshot(session.id)
            val sets = db.workoutDao().getSetsForSessionSnapshot(session.id)
            assertEquals(2, entries.map { it.id }.distinct().size)
            assertEquals(listOf(0, 1), entries.map { it.orderIndex })
            assertEquals(second.id, entries.last().id)
            assertEquals(entries.map { it.id }.toSet(), sets.map { it.workoutExerciseId }.toSet())
            assertEquals(listOf(1, 1), sets.map { it.setNumber })
            assertEquals(setOf(60.0, 70.0), sets.map { it.weightKg }.toSet())
            assertEquals(before + 4, db.mutationDao().getUnsentMutationsCount().first())
        } finally { db.close() }
    }

    @Test fun previousPerformanceExcludesActiveSessionsAndOrdersRepeatedOccurrencesAndSets() = runTest {
        val db = database()
        try {
            val dao = db.workoutDao()
            dao.insertSession(WorkoutSessionEntity("done", "Done", 100L, 200L, true, 0.0))
            dao.insertSession(WorkoutSessionEntity("active", "Active", 300L, null, false, 0.0))
            dao.insertExercise(WorkoutExerciseEntity("first", "done", "bench", "Bench", "CHEST", 90, 0))
            dao.insertExercise(WorkoutExerciseEntity("last", "done", "bench", "Bench", "CHEST", 90, 1))
            suspend fun set(id: String, session: String, entry: String?, number: Int, completed: Boolean = true) {
                dao.insertOrUpdateSet(WorkoutSetEntity(id, session, "bench", number, 40.0, 8, null, completed, null,
                    workoutExerciseId = entry))
            }
            set("zz-first", "done", "first", 99)
            set("set-9999999999999", "done", "last", 1)
            set("00000000-0000-4000-8000-000000000001", "done", "last", 2)
            set("zz-active", "active", null, 99)
            set("zz-incomplete", "done", "last", 3, completed = false)
            assertEquals("00000000-0000-4000-8000-000000000001", dao.getLastPerformanceForExercise("bench")?.id)
            set("00000000-0000-4000-8000-000000000002", "done", "last", 2)
            assertEquals("00000000-0000-4000-8000-000000000002", dao.getLastPerformanceForExercise("bench")?.id)
        } finally { db.close() }
    }

    @Test fun previousPerformanceUsesStartTimeFallbackAndStableSessionTieBreakers() = runTest {
        val db = database()
        try {
            val dao = db.workoutDao()
            suspend fun session(id: String, start: Long, end: Long?) {
                dao.insertSession(WorkoutSessionEntity(id, id, start, end, true, 0.0))
                dao.insertOrUpdateSet(WorkoutSetEntity("set-$id", id, "bench", 1, 40.0, 8, null, true, null))
            }
            session("z-earlier-start", 100L, 500L)
            session("a-later-start", 300L, 500L)
            assertEquals("a-later-start", dao.getLastPerformanceForExercise("bench")?.sessionId)
            session("z-same-start", 300L, 500L)
            assertEquals("z-same-start", dao.getLastPerformanceForExercise("bench")?.sessionId)
            session("a-null-end", 600L, null)
            assertEquals("a-null-end", dao.getLastPerformanceForExercise("bench")?.sessionId)
        } finally { db.close() }
    }

    @Test
    fun genericCoordinatorCommitsBothWritesAndReturnsValue() = runTest {
        val db = database()
        try {
            val room: RoomDatabase = db
            val coordinator: CommandCoordinator = RoomCommandCoordinator(room, ApplicationScope(backgroundScope.coroutineContext))
            val result = coordinator.write {
                db.workoutDao().insertSession(WorkoutSessionEntity("generic", "Committed", 1L, null, false, 0.0))
                db.mutationDao().insertMutation(SyncMutationEntity("generic-outbox", "workout_session", "generic", "INSERT", "{}", 1L))
                "committed"
            }
            assertEquals("committed", result)
            assertEquals("Committed", db.workoutDao().getSessionById("generic")?.title)
            assertEquals(1, db.mutationDao().getUnsentMutationsCount().first())
        } finally { db.close() }
    }

    @Test
    fun genericCoordinatorRollsBackBothWritesOnFailure() = runTest {
        val db = database()
        try {
            val room: RoomDatabase = db
            val coordinator: CommandCoordinator = RoomCommandCoordinator(room, ApplicationScope(backgroundScope.coroutineContext))
            assertFailsWith<IllegalStateException> {
                coordinator.write {
                    db.workoutDao().insertSession(WorkoutSessionEntity("generic", "Rollback", 1L, null, false, 0.0))
                    db.mutationDao().insertMutation(SyncMutationEntity("generic-outbox", "workout_session", "generic", "INSERT", "{}", 1L))
                    error("Fail after both writes")
                }
            }
            assertNull(db.workoutDao().getSessionById("generic"))
            assertEquals(0, db.mutationDao().getUnsentMutationsCount().first())
        } finally { db.close() }
    }

    @Test
    fun catalogueRetainsStableIdsAndJoinsCallerTransaction() = runTest {
        val db = database()
        try {
            val writes = writes(db)
            val application = koinApplication {
                modules(module {
                    single { db.exerciseDao() }
                    single { db.mutationDao() }
                    single<AppClock> { object : AppClock { override fun nowMillis() = 1000L } }
                    single<CommandCoordinator> { writes }
                }, exerciseFeatureModule())
            }
            val catalogue = application.koin.get<ExerciseCatalog>().also { application.close() }
            val bench = Exercise("ex_bench", "Bench press", MuscleGroup.CHEST, 90)
            val squat = Exercise("ex_squat", "Squat", MuscleGroup.LEGS, 120)
            writes.write {
                catalogue.saveExercise(squat)
                catalogue.saveExercises(listOf(bench))
            }
            assertEquals(listOf(bench, squat), catalogue.getAllExercises())
            assertEquals(listOf(bench, squat), catalogue.observeExercises().first())
            assertEquals(bench, catalogue.getExerciseById("ex_bench"))
            assertNull(catalogue.getExerciseById("missing"))
            assertEquals(2, catalogue.getExercisesCount())
            assertFailsWith<IllegalStateException> {
                writes.write {
                    catalogue.saveExercise(bench.copy(name = "Changed"))
                    catalogue.saveExercise(Exercise("third", "Third", MuscleGroup.BACK))
                    error("Rollback catalogue changes")
                }
            }
            assertEquals(bench, catalogue.getExerciseById("ex_bench"))
            assertNull(catalogue.getExerciseById("third"))
            assertEquals(2, catalogue.getExercisesCount())
        } finally { db.close() }
    }

    @Test
    fun failedOutboxInsertRollsBackSession() = runTest {
        val db = database()
        try {
            val failingOutbox = object : MutationDao by db.mutationDao() {
                override suspend fun insertMutation(mutation: SyncMutationEntity) { error("Outbox unavailable") }
            }
            val repository = repository(db, writes(db), failingOutbox)
            assertFailsWith<IllegalStateException> { repository.startWorkout("Rollback") }
            assertNull(db.workoutDao().getActiveSessionSync())
            assertEquals(0, db.workoutDao().getAllSessionsList().size)
        } finally { db.close() }
    }

    @Test
    fun concurrentStartsCreateOneSessionAndOneMutation() = runTest {
        val db = database()
        try {
            val repository = repository(db, writes(db))
            val sessions = List(20) { async { repository.startWorkout("One session") } }.awaitAll()
            assertEquals(1, sessions.map { it.id }.toSet().size)
            assertEquals(1, db.workoutDao().getAllSessionsList().size)
            assertEquals(1, db.mutationDao().getUnsentMutationsCount().first())
        } finally { db.close() }
    }

    @Test
    fun queuedEditsUseLatestStoredSet() = runTest {
        val db = database()
        try {
            val repository = repository(db, writes(db))
            val session = repository.startWorkout("Edits")
            val set = repository.addSet(session.id, "bench", 60.0, 10)
            val weight = async(start = CoroutineStart.UNDISPATCHED) {
                repository.updateSet(set.id) { it.copy(weightKg = 65.0) }
            }
            val reps = async(start = CoroutineStart.UNDISPATCHED) {
                repository.updateSet(set.id) { it.copy(reps = 12) }
            }
            awaitAll(weight, reps)
            val saved = assertNotNull(db.workoutDao().getSetById(set.id))
            assertEquals(65.0, saved.weightKg)
            assertEquals(12, saved.reps)
        } finally { db.close() }
    }

    @Test
    fun acceptedCommandCommitsAfterCallerIsCancelled() = runTest {
        val db = database()
        try {
            val enteredOutbox = CompletableDeferred<Unit>()
            val releaseOutbox = CompletableDeferred<Unit>()
            val outbox = object : MutationDao by db.mutationDao() {
                override suspend fun insertMutation(mutation: SyncMutationEntity) {
                    enteredOutbox.complete(Unit)
                    releaseOutbox.await()
                    db.mutationDao().insertMutation(mutation)
                }
            }
            val writes = writes(db)
            val repository = repository(db, writes, outbox)
            val caller = launch { repository.startWorkout("Survives navigation") }
            enteredOutbox.await()
            caller.cancelAndJoin()
            releaseOutbox.complete(Unit)
            writes.write { Unit } // Barrier: the preceding transaction must finish first.
            assertNotNull(db.workoutDao().getActiveSessionSync())
            assertEquals(1, db.mutationDao().getUnsentMutationsCount().first())
        } finally { db.close() }
    }

    @Test
    fun failedCommandDoesNotStopFollowingCommands() = runTest {
        val db = database()
        try {
            var failNext = true
            val outbox = object : MutationDao by db.mutationDao() {
                override suspend fun insertMutation(mutation: SyncMutationEntity) {
                    if (failNext) { failNext = false; error("First insert failed") }
                    db.mutationDao().insertMutation(mutation)
                }
            }
            val repository = repository(db, writes(db), outbox)
            assertFailsWith<IllegalStateException> { repository.startWorkout("Failed") }
            val saved = repository.startWorkout("Successful")
            assertEquals("Successful", saved.title)
            assertEquals(1, db.workoutDao().getAllSessionsList().size)
            assertEquals(1, db.mutationDao().getUnsentMutationsCount().first())
        } finally { db.close() }
    }

    // RoutinePersistenceTest covers child failure after the legacy SQL tables were retired.
    @Test
    fun cancelledCommandDoesNotPoisonQueue() = runTest {
        val db = database()
        try {
            val writes = writes(db)
            assertFailsWith<CancellationException> {
                writes.write<Unit> { throw CancellationException("Only this command") }
            }
            assertEquals("Still active", repository(db, writes).startWorkout("Still active").title)
        } finally { db.close() }
    }

    @Test
    fun finishedWorkoutRejectsSubsequentEditsWithoutChangingVolume() = runTest {
        val db = database()
        try {
            val repository = repository(db, writes(db))
            val session = repository.startWorkout("Finalized")
            val set = repository.addSet(session.id, "bench", 60.0, 10)
            repository.updateSet(set.id) { it.copy(isCompleted = true) }
            // Arrange a finalized stored record; app workflow completion is covered by TrainingWorkflowTest.
            db.workoutDao().completeSession(session.id, 1000L, 600.0)
            assertFailsWith<IllegalStateException> {
                repository.updateSet(set.id) { it.copy(weightKg = 100.0) }
            }
            assertFailsWith<IllegalStateException> { repository.deleteSet(set.id) }
            assertEquals(60.0, db.workoutDao().getSetById(set.id)?.weightKg)
            assertEquals(600.0, db.workoutDao().getSessionById(session.id)?.totalVolumeKg)
        } finally { db.close() }
    }

    @Test
    fun deletingSetRenumbersRemainingSetsInSameCommand() = runTest {
        val db = database()
        try {
            val repository = repository(db, writes(db))
            val session = repository.startWorkout("Delete")
            val first = repository.addSet(session.id, "bench", 60.0, 10)
            repository.addSet(session.id, "bench", 65.0, 10)
            repository.addSet(session.id, "bench", 70.0, 10)
            repository.deleteSet(first.id)
            val remaining = db.workoutDao().getSetsForSessionSnapshot(session.id)
            assertEquals(listOf(1, 2), remaining.map { it.setNumber })
            assertEquals(listOf(65.0, 70.0), remaining.map { it.weightKg })
        } finally { db.close() }
    }

    @Test
    fun failedExerciseRemovalRollsBackAllDeletedSets() = runTest {
        val db = database()
        try {
            var rejectDeletes = false
            var deleteAttempts = 0
            val outbox = object : MutationDao by db.mutationDao() {
                override suspend fun insertMutation(mutation: SyncMutationEntity) {
                    if (rejectDeletes && mutation.operation == "DELETE" && ++deleteAttempts == 2) error("Delete failed")
                    db.mutationDao().insertMutation(mutation)
                }
            }
            val repository = repository(db, writes(db), outbox)
            val session = repository.startWorkout("Remove exercise")
            repository.addSet(session.id, "bench", 60.0, 10)
            repository.addSet(session.id, "bench", 65.0, 10)
            val outboxBefore = db.mutationDao().getUnsentMutationsCount().first()
            rejectDeletes = true
            assertFailsWith<IllegalStateException> { repository.removeExerciseFromWorkout(session.id, "bench") }
            assertEquals(2, db.workoutDao().getSetsForSessionSnapshot(session.id).size)
            assertEquals(outboxBefore, db.mutationDao().getUnsentMutationsCount().first())
        } finally { db.close() }
    }

    @Test
    fun missingMigrationFailsWithoutErasingExistingData() = runTest {
        val path = Files.createTempFile("contentapp-migration", ".db")
        fun open() = Room.databaseBuilder<AppDatabase>(
            name = path.toString(), factory = { AppDatabaseConstructor.initialize() }
        ).configureAppDatabase().build()
        var db = open()
        try {
            repository(db, writes(db)).startWorkout("Keep this data")
            db.close()
            // Simulate an older on-device version with no migration to the current schema.
            val versionConnection = BundledSQLiteDriver().open(path.toString())
            try {
                val connection = versionConnection
                connection.prepare("PRAGMA user_version = 5").use { it.step() }
            } finally { versionConnection.close() }
            db = open()
            assertFailsWith<IllegalStateException> { db.workoutDao().getAllSessionsList() }
            db.close()
            val verifyConnection = BundledSQLiteDriver().open(path.toString())
            try {
                val connection = verifyConnection
                connection.prepare("SELECT title FROM workout_sessions").use { statement ->
                    statement.step()
                    assertEquals("Keep this data", statement.getText(0))
                }
                connection.prepare("SELECT COUNT(*) FROM sync_mutations").use { statement ->
                    statement.step()
                    assertEquals(1L, statement.getLong(0))
                }
            } finally { verifyConnection.close() }
        } finally {
            db.close()
            Files.deleteIfExists(path)
            Files.deleteIfExists(path.resolveSibling(path.fileName.toString() + "-wal"))
            Files.deleteIfExists(path.resolveSibling(path.fileName.toString() + "-shm"))
        }
    }

    @Test
    fun retryQueryUsesConfiguredLimitAndUnsentCountIncludesDeadLetters() = runTest {
        val db = database()
        try {
            db.mutationDao().insertMutation(SyncMutationEntity(
                "failed", "session", "session1", "INSERT", "{}", 1000L, retryCount = 5
            ))
            assertEquals(1, db.mutationDao().getPendingMutations(7).first().size)
            assertEquals(0, db.mutationDao().getPendingMutations(5).first().size)
            assertEquals(1, db.mutationDao().getUnsentMutationsCount().first())
        } finally { db.close() }
    }

}
