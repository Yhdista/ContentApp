package features.workout

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
import features.workout.data.entity.WorkoutTemplateExerciseEntity
import features.exercises.api.Exercise
import features.exercises.api.ExerciseCatalog
import features.exercises.api.MuscleGroup
import features.workout.domain.WorkoutRepository
import features.workout.workoutFeatureModule
import features.exercises.data.exerciseFeatureModule
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import features.workout.domain.WorkoutTemplateExercise
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
                single<CommandCoordinator> { writes }
                single<AppClock> { object : AppClock { override fun nowMillis() = 1000L } }
            }, exerciseFeatureModule(), workoutFeatureModule())
        }
        return application.koin.get<WorkoutRepository>().also { application.close() }
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

    @Test
    fun templateChildInsertFailureRollsBackParentAndOutbox() = runTest {
        val db = database()
        try {
            val dao = object : WorkoutDao by db.workoutDao() {
                override suspend fun insertTemplateExercises(exercises: List<WorkoutTemplateExerciseEntity>) {
                    error("Child insert failed")
                }
            }
            val repository = repository(db, writes(db), dao = dao)
            assertFailsWith<IllegalStateException> {
                repository.createTemplate("Rollback", "", listOf(
                    WorkoutTemplateExercise("bench", "Bench", 3, 60.0, 10, 0)
                ))
            }
            assertEquals(0, db.workoutDao().getTemplatesCount())
            assertEquals(0, db.mutationDao().getUnsentMutationsCount().first())
        } finally { db.close() }
    }

    @Test
    fun repeatedTemplateStartDoesNotDuplicateSets() = runTest {
        val db = database()
        try {
            val repository = repository(db, writes(db))
            val template = repository.createTemplate("Push", "", listOf(
                WorkoutTemplateExercise("bench", "Bench", 3, 60.0, 10, 0)
            ))
            val first = repository.startWorkoutFromTemplate(template.id)
            val second = repository.startWorkoutFromTemplate(template.id)
            assertEquals(first.id, second.id)
            assertEquals(3, db.workoutDao().getSetsForSessionSnapshot(first.id).size)
        } finally { db.close() }
    }
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
            repository.completeWorkout(session.id)
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
            rejectDeletes = true
            assertFailsWith<IllegalStateException> { repository.removeExerciseFromWorkout(session.id, "bench") }
            assertEquals(2, db.workoutDao().getSetsForSessionSnapshot(session.id).size)
            assertEquals(3, db.mutationDao().getUnsentMutationsCount().first())
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
