package features.exercises

import androidx.room.Room
import app.database.AppDatabase
import app.database.AppDatabaseConstructor
import app.database.configureAppDatabase
import core.common.AppClock
import core.common.ApplicationScope
import core.database.MutationDao
import core.database.SyncMutationEntity
import core.storage.CommandCoordinator
import core.storage.RoomCommandCoordinator
import features.exercises.api.*
import features.exercises.data.exerciseFeatureModule
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.*
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseCatalogCommandsTest {
    private class Fixture(val db: AppDatabase, val catalog: ExerciseCatalog, val importer: ExerciseCatalogImport)
    private fun TestScope.fixture(outbox: (MutationDao) -> MutationDao = { it }): Fixture {
        val db = Room.inMemoryDatabaseBuilder<AppDatabase> { AppDatabaseConstructor.initialize() }
            .configureAppDatabase().build()
        val app = koinApplication { modules(exerciseFeatureModule(), module {
            single { db.exerciseDao() }
            single { outbox(db.mutationDao()) }
            single<AppClock> { object : AppClock { override fun nowMillis() = 123L } }
            single<CommandCoordinator> { RoomCommandCoordinator(db, ApplicationScope(backgroundScope.coroutineContext)) }
        }) }
        return Fixture(db, app.koin.get(), app.koin.get()).also { app.close() }
    }

    @Test fun userWritesEmitOutboxAtomically() = runTest {
        val f = fixture()
        try {
            f.importer.ensureDefaults()
            val defaults = f.catalog.getAllExercises()
            assertEquals(setOf("ex_bench", "ex_squat", "ex_deadlift", "ex_ohp", "ex_pullup", "ex_biceps", "ex_plank"), defaults.map { it.id }.toSet())
            assertTrue(defaults.any { it.name == "Bench press" })
            assertTrue(defaults.any { it.name == "Dřep s činkou" })
            assertEquals(120, defaults.single { it.id == "ex_bench" }.defaultRestSeconds)
            f.importer.ensureDefaults()
            assertEquals(defaults, f.catalog.getAllExercises())
            assertEquals(0, f.db.mutationDao().getUnsentMutationsCount().first())
            val created = f.catalog.create("  Bench \"Heavy\" \\ Press  ", MuscleGroup.CHEST, 120)
            assertTrue(created.id.startsWith("custom-ex-"))
            assertEquals("Bench \"Heavy\" \\ Press", created.name)
            assertEquals(created, f.catalog.getExerciseById(created.id))
            val updated = f.catalog.update(created.copy(name = "Renamed", defaultRestSeconds = 60))
            assertEquals(created.id, updated.id)
            f.catalog.archive(created.id)
            f.catalog.archive(created.id) // Idempotent archive produces one mutation.
            val archived = f.catalog.getExerciseById(created.id)!!
            assertTrue(archived.archived)
            val mutations = f.db.mutationDao().getPendingMutations().first().filter { it.recordId == created.id }
            assertEquals(3, mutations.size)
            assertEquals(1, mutations.count { it.operation == "INSERT" })
            assertEquals(2, mutations.count { it.operation == "UPDATE" })
            assertEquals(3, mutations.map { it.id }.distinct().size)
            val inserted = Json.parseToJsonElement(mutations.single { it.operation == "INSERT" }.payloadJson).jsonObject
            assertEquals(created.name, inserted.getValue("name").jsonPrimitive.content)
            assertEquals(created.id, inserted.getValue("id").jsonPrimitive.content)
            assertTrue(mutations.any { Json.parseToJsonElement(it.payloadJson).jsonObject["archived"]?.jsonPrimitive?.boolean == true })
        } finally { f.db.close() }
    }

    @Test fun outboxFailureRollsBackUserMutation() = runTest {
        var reject = false
        val f = fixture { real -> object : MutationDao by real {
            override suspend fun insertMutation(mutation: SyncMutationEntity) {
                if (reject) error("Outbox unavailable")
                real.insertMutation(mutation)
            }
        } }
        try {
            val original = f.catalog.create("Original", MuscleGroup.BACK)
            reject = true
            assertFails { f.catalog.create("Must roll back", MuscleGroup.LEGS) }
            assertFails { f.catalog.update(original.copy(name = "Must roll back")) }
            assertFails { f.catalog.archive(original.id) }
            assertEquals(listOf(original), f.catalog.getAllExercises())
            assertEquals(1, f.db.mutationDao().getUnsentMutationsCount().first())
        } finally { f.db.close() }
    }

    @Test fun invalidCommandPreservesCatalogAndOutbox() = runTest {
        val f = fixture()
        try {
            val original = f.catalog.create("Original", MuscleGroup.BACK)
            assertFailsWith<IllegalArgumentException> { f.catalog.create("  ", MuscleGroup.CORE) }
            assertFailsWith<IllegalArgumentException> { f.catalog.create("Invalid rest", MuscleGroup.CORE, 0) }
            assertFailsWith<IllegalArgumentException> { f.catalog.update(original.copy(name = " ")) }
            assertFailsWith<IllegalArgumentException> { f.catalog.update(original.copy(defaultRestSeconds = -1)) }
            assertFailsWith<NoSuchElementException> { f.catalog.update(original.copy(id = "missing")) }
            assertFailsWith<NoSuchElementException> { f.catalog.archive("missing") }
            assertEquals(listOf(original), f.catalog.getAllExercises())
            assertEquals(1, f.db.mutationDao().getUnsentMutationsCount().first())
        } finally { f.db.close() }
    }

    @Test fun acceptedCommandSurvivesCallerCancellation() = runTest {
        val accepted = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val committed = CompletableDeferred<Unit>()
        val f = fixture { real -> object : MutationDao by real {
            override suspend fun insertMutation(mutation: SyncMutationEntity) {
                accepted.complete(Unit)
                release.await()
                real.insertMutation(mutation)
                committed.complete(Unit)
            }
        } }
        try {
            val caller = launch { f.catalog.create("Accepted", MuscleGroup.ARMS) }
            accepted.await()
            caller.cancelAndJoin()
            release.complete(Unit)
            committed.await()
            // A read waits for the writer transaction to commit, including its outbox.
            val saved = f.catalog.getAllExercises().single()
            assertEquals("Accepted", saved.name)
            assertEquals(saved.id, f.db.mutationDao().getPendingMutations().first().single().recordId)
        } finally { release.complete(Unit); f.db.close() }
    }
}
