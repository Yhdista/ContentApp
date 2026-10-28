package features.routines

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.use
import app.database.AppDatabase
import app.database.AppDatabaseConstructor
import app.database.configureAppDatabase
import core.common.AppClock
import core.common.ApplicationScope
import core.database.MutationDao
import core.result.Result
import core.storage.CommandCoordinator
import core.storage.DraftStore
import core.storage.RoomCommandCoordinator
import core.storage.draftStorageModule
import features.exercises.api.Exercise
import features.exercises.api.ExerciseCatalog
import features.exercises.api.MuscleGroup
import features.exercises.data.exerciseFeatureModule
import features.routines.api.Routine
import features.routines.api.RoutineError
import features.routines.api.RoutineExercise
import features.routines.api.RoutineRepository
import features.routines.api.RoutineSet
import features.routines.data.routineFeatureModule
import features.workout.domain.WorkoutSetType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import java.nio.file.Files
import kotlin.test.*

class RoutinePersistenceTest {
    private fun database(path: String? = null): AppDatabase =
        (if (path == null) Room.inMemoryDatabaseBuilder<AppDatabase> { AppDatabaseConstructor.initialize() }
        else Room.databaseBuilder<AppDatabase>(name = path) { AppDatabaseConstructor.initialize() })
            .configureAppDatabase().build()

    private data class Services(val routines: RoutineRepository, val drafts: DraftStore, val catalogue: ExerciseCatalog, val writes: CommandCoordinator)

    private fun TestScope.services(db: AppDatabase, catalogue: ExerciseCatalog? = null): Services {
        val writes = RoomCommandCoordinator(db, ApplicationScope(backgroundScope.coroutineContext))
        val app = koinApplication {
            modules(module {
                single { db.routineDao() }
                single { db.editorDraftDao() }
                single { db.exerciseDao() }
                single { db.mutationDao() }
                single<CommandCoordinator> { writes }
                single<AppClock> { object : AppClock { override fun nowMillis() = 1000L } }
            }, exerciseFeatureModule(), routineFeatureModule(), draftStorageModule())
            if (catalogue != null) modules(module { single<ExerciseCatalog> { catalogue } })
        }
        return Services(app.koin.get(), app.koin.get(), app.koin.get(), writes).also { app.close() }
    }

    private suspend fun seed(s: Services) {
        (s.catalogue as features.exercises.api.ExerciseCatalogImport).importExercises(listOf(Exercise("bench", "Bench", MuscleGroup.CHEST)))
    }
    private fun routine() = Routine("routine", "  Morning  ", "Notes \"x\"\n", exercises = listOf(
        RoutineExercise("entry-b", "bench", 90, listOf(
            RoutineSet("set-b", 20.0, 8, WorkoutSetType.WARMUP),
            RoutineSet("set-a", 40.5, 10, WorkoutSetType.DROPSET))),
        RoutineExercise("entry-a", "bench", 0, listOf(RoutineSet("set-c", 0.0, 12, WorkoutSetType.FAILURE)))
    ))
    private fun <T> success(result: Result<T, RoutineError>): T = assertIs<Result.Success<T>>(result).data
    private fun error(code: RoutineError, result: Result<*, RoutineError>) = assertEquals(code, assertIs<Result.Error<RoutineError>>(result).error)

    @Test fun liveObserverReceivesOuterCommitWithoutReopeningAndNeverSeesRollback() = runTest {
        val db = database()
        try {
            val s = services(db); seed(s)
            val original = success(s.routines.save(routine(), null))
            val emissions = Channel<List<Routine>>(Channel.UNLIMITED)
            val observer = backgroundScope.launch { s.routines.observeAll(true).collect { emissions.send(it) } }
            suspend fun next() = withContext(Dispatchers.IO) { withTimeout(5000) { emissions.receive() } }
            assertEquals(listOf(original), next()) // Register before writing; never reopen the Flow.
            assertFailsWith<IllegalStateException> {
                s.writes.write {
                    success(s.routines.save(original.copy(name = "Rolled back"), original.revision))
                    error("abort outer transaction")
                }
            }
            val saved = success(s.routines.save(original.copy(name = "Committed"), original.revision))
            assertEquals(listOf(saved), next())
            success(s.routines.archive(saved.id, saved.revision))
            val archived = next().single()
            assertTrue(archived.archived); assertEquals(saved.revision + 1, archived.revision)
            observer.cancel()
        } finally { db.close() }
    }

    @Test fun saveReopensOrderedAggregateWithRepeatedCatalogueIdsAndTypedOutbox() = runTest {
        val db = database()
        try {
            val s = services(db); seed(s)
            val saved = success(s.routines.save(routine(), null))
            assertEquals("Morning", saved.name); assertEquals(1L, saved.revision)
            assertEquals(saved, s.routines.getById("routine"))
            assertEquals(saved, s.routines.observeById("routine").first())
            assertEquals(listOf(saved), s.routines.observeAll().first())
            assertEquals(listOf("entry-b", "entry-a"), saved.exercises.map { it.id })
            assertEquals(listOf("set-b", "set-a"), saved.exercises.first().sets.map { it.id })
            val outbox = db.mutationDao().getPendingMutations().first()
            assertEquals(6, outbox.size)
            assertEquals(mapOf("routine" to 1, "routine_exercise" to 2, "routine_set" to 3), outbox.groupingBy { it.entityName }.eachCount())
            assertTrue(outbox.all { it.operation == "INSERT" && it.createdAt == 1000L })
            val parent = Json.parseToJsonElement(outbox.single { it.entityName == "routine" }.payloadJson).jsonObject
            assertEquals("Notes \"x\"\n", parent["notes"]?.jsonPrimitive?.content)
            assertEquals("1", parent["revision"]?.jsonPrimitive?.content)
            val second = Json.parseToJsonElement(outbox.single { it.recordId == "entry-a" }.payloadJson).jsonObject
            assertEquals("1", second["orderIndex"]?.jsonPrimitive?.content)
            val set = Json.parseToJsonElement(outbox.single { it.recordId == "set-a" }.payloadJson).jsonObject
            assertEquals("DROPSET", set["setType"]?.jsonPrimitive?.content)
            assertEquals(saved, Json.decodeFromString<Routine>(Json.encodeToString(saved)))
        } finally { db.close() }
    }

    @Test fun updateRetainsStableIdsNormalizesOrderAndRemovesDeletedRows() = runTest {
        val db = database()
        try {
            val s = services(db); seed(s)
            val saved = success(s.routines.save(routine(), null))
            val edited = saved.copy(name = "Edited", exercises = saved.exercises.reversed().map { e ->
                if (e.id == "entry-b") e.copy(sets = e.sets.takeLast(1)) else e
            })
            val updated = success(s.routines.save(edited, 1))
            assertEquals(2L, updated.revision)
            assertEquals(edited.copy(revision = 2), s.routines.getById(saved.id))
            val rows = db.routineDao().getById(saved.id)!!
            assertEquals(listOf(0, 1), rows.exercises.map { it.entry.orderIndex }.sorted())
            assertEquals(listOf("set-a", "set-c"), rows.exercises.flatMap { it.sets }.map { it.id }.sorted())
            val outbox = db.mutationDao().getPendingMutations().first()
            assertTrue(outbox.any { it.recordId == "set-b" && it.operation == "DELETE" })
            assertTrue(outbox.any { it.recordId == "entry-a" && it.operation == "UPDATE" })
        } finally { db.close() }
    }

    @Test fun staleAndMissingExpectedRevisionsNeverOverwriteOrEnqueue() = runTest {
        val db = database()
        try {
            val s = services(db); seed(s)
            val saved = success(s.routines.save(routine(), null))
            error(RoutineError.REVISION_CONFLICT, s.routines.save(saved.copy(name = "Lost"), null))
            val latest = success(s.routines.save(saved.copy(name = "Latest"), 1))
            val count = db.mutationDao().getUnsentMutationsCount().first()
            error(RoutineError.REVISION_CONFLICT, s.routines.save(saved.copy(name = "Lost"), 1))
            error(RoutineError.REVISION_CONFLICT, s.routines.archive(saved.id, 1))
            assertEquals(latest, s.routines.getById(saved.id))
            assertEquals(count, db.mutationDao().getUnsentMutationsCount().first())
        } finally { db.close() }
    }

    @Test fun duplicateCopiesValuesWithFreshIdsAndRevisionOneEvenFromArchive() = runTest {
        val db = database()
        try {
            val s = services(db); seed(s)
            val saved = success(s.routines.save(routine(), null))
            success(s.routines.archive(saved.id, 1))
            val archivedOriginal = s.routines.getById(saved.id)
            val copy = success(s.routines.duplicate(saved.id))
            assertEquals(archivedOriginal, s.routines.getById(saved.id))
            assertNotEquals(saved.id, copy.id); assertEquals(1L, copy.revision); assertFalse(copy.archived)
            assertEquals(saved.name, copy.name); assertEquals(saved.notes, copy.notes)
            assertTrue(copy.exercises.map { it.id }.intersect(saved.exercises.map { it.id }.toSet()).isEmpty())
            assertTrue(copy.exercises.flatMap { it.sets }.map { it.id }.intersect(saved.exercises.flatMap { it.sets }.map { it.id }.toSet()).isEmpty())
            assertEquals(saved.exercises.map { it.exerciseId to it.restSeconds }, copy.exercises.map { it.exerciseId to it.restSeconds })
            assertEquals(saved.exercises.flatMap { it.sets }.map { Triple(it.weightKg, it.reps, it.setType) }, copy.exercises.flatMap { it.sets }.map { Triple(it.weightKg, it.reps, it.setType) })
            assertEquals(copy, s.routines.getById(copy.id))
        } finally { db.close() }
    }

    @Test fun archiveIsSoftRevisionedAndCannotBeBypassedBySave() = runTest {
        val db = database()
        try {
            val s = services(db); seed(s)
            val saved = success(s.routines.save(routine(), null))
            success(s.routines.archive(saved.id, 1))
            val archived = s.routines.getById(saved.id)!!
            assertTrue(archived.archived); assertEquals(2L, archived.revision)
            assertEquals(saved.exercises, archived.exercises)
            assertTrue(s.routines.observeAll().first().isEmpty())
            assertEquals(listOf(archived), s.routines.observeAll(true).first())
            error(RoutineError.ARCHIVED, s.routines.save(saved, 2))
            error(RoutineError.ARCHIVED, s.routines.archive(saved.id, 2))
            error(RoutineError.ARCHIVED, s.routines.save(saved.copy(id = "new", archived = true), null))
        } finally { db.close() }
    }

    @Test fun invalidAndMissingReferencesReturnTypedErrorsWithoutRows() = runTest {
        val db = database()
        try {
            val s = services(db)
            error(RoutineError.VALIDATION, s.routines.save(routine().copy(name = " "), null))
            error(RoutineError.NOT_FOUND, s.routines.save(routine(), null))
            error(RoutineError.NOT_FOUND, s.routines.save(routine(), 1))
            error(RoutineError.NOT_FOUND, s.routines.duplicate("missing"))
            error(RoutineError.NOT_FOUND, s.routines.archive("missing", 1))
            assertNull(s.routines.getById("routine"))
            assertEquals(0, db.mutationDao().getUnsentMutationsCount().first())
        } finally { db.close() }
    }

    @Test fun catalogueArchiveBlocksNewEntriesButExistingEntryCanBeRetainedForRepair() = runTest {
        val db = database()
        try {
            val regular = services(db); seed(regular)
            val saved = success(regular.routines.save(routine(), null))
            val archivedCatalogue = object : ExerciseCatalog by regular.catalogue {
                override suspend fun getExerciseById(id: String) = regular.catalogue.getExerciseById(id)?.copy(archived = true)
            }
            val s = services(db, archivedCatalogue)
            success(s.routines.save(saved.copy(name = "Repairing"), 1))
            val latest = s.routines.getById(saved.id)!!
            error(RoutineError.ARCHIVED, s.routines.save(latest.copy(exercises = latest.exercises + RoutineExercise("new", "bench", 90, listOf(RoutineSet("new-set", 0.0, 1)))), 2))
            error(RoutineError.ARCHIVED, s.routines.duplicate(saved.id))
        } finally { db.close() }
    }

    @Test fun duplicateChildIdsAreRejectedAndCrossAggregateIdsNeverStealRows() = runTest {
        val db = database()
        try {
            val s = services(db); seed(s)
            val original = routine()
            error(RoutineError.VALIDATION, s.routines.save(original.copy(exercises = original.exercises.map { it.copy(id = "same") }), null))
            error(RoutineError.VALIDATION, s.routines.save(original.copy(exercises = original.exercises.map { e -> e.copy(sets = e.sets.map { it.copy(id = "same") }) }), null))
            val saved = success(s.routines.save(original, null))
            error(RoutineError.STORAGE, s.routines.save(original.copy(id = "another"), null))
            assertNull(s.routines.getById("another")); assertEquals(saved, s.routines.getById(saved.id))
            assertEquals(6, db.mutationDao().getUnsentMutationsCount().first())
        } finally { db.close() }
    }

    private fun sql(path: String, statement: String) {
        val c = BundledSQLiteDriver().open(path)
        try { c.prepare(statement).use { it.step() } } finally { c.close() }
    }

    @Test fun failedThirdSetRollsBackExistingAggregateRevisionAndAllOutbox() = runTest {
        val path = Files.createTempFile("routine-rollback", ".db")
        val db = database(path.toString())
        try {
            val s = services(db); seed(s)
            val saved = success(s.routines.save(routine(), null))
            sql(path.toString(), "CREATE TRIGGER fail_set BEFORE INSERT ON routine_sets WHEN NEW.id = 'fail-third' BEGIN SELECT RAISE(ABORT, 'third set rejected'); END")
            val edited = saved.copy(name = "Lost", exercises = listOf(saved.exercises.first().copy(sets = saved.exercises.first().sets + RoutineSet("fail-third", 12.0, 10))))
            error(RoutineError.STORAGE, s.routines.save(edited, 1))
            assertEquals(saved, s.routines.getById(saved.id)); assertEquals(6, db.mutationDao().getUnsentMutationsCount().first())
        } finally { db.close(); Files.deleteIfExists(path) }
    }

    @Test fun failedOutboxRollsBackRoutineChildrenAndPreviousOutboxInserts() = runTest {
        val path = Files.createTempFile("routine-outbox", ".db")
        val db = database(path.toString())
        try {
            val s = services(db); seed(s)
            sql(path.toString(), "CREATE TRIGGER fail_outbox BEFORE INSERT ON sync_mutations WHEN NEW.entityName = 'routine_set' BEGIN SELECT RAISE(ABORT, 'outbox rejected'); END")
            error(RoutineError.STORAGE, s.routines.save(routine(), null))
            assertNull(s.routines.getById("routine")); assertEquals(0, db.mutationDao().getUnsentMutationsCount().first())
            // The feature handles its database error; its savepoint still rolls back while
            // the caller deliberately commits an unrelated draft in the outer transaction.
            s.writes.write {
                s.drafts.put("routine", "other-editor", "keep")
                error(RoutineError.STORAGE, s.routines.save(routine(), null))
            }
            assertEquals("keep", s.drafts.get("routine", "other-editor"))
            assertNull(s.routines.getById("routine")); assertEquals(0, db.mutationDao().getUnsentMutationsCount().first())
        } finally { db.close(); Files.deleteIfExists(path) }
    }

    @Test fun draftSurvivesReopenAcceptsIncompleteRoutineAndNeverEnqueues() = runTest {
        val path = Files.createTempFile("routine-draft", ".db")
        var db = database(path.toString())
        try {
            val s = services(db)
            val incomplete = routine().copy(name = "", exercises = emptyList())
            s.drafts.put("routine", "editor", Json.encodeToString(incomplete))
            s.drafts.put("plan", "editor", "plan-payload")
            s.drafts.put("routine", "editor", Json.encodeToString(incomplete.copy(notes = "Last edit")))
            assertNull(s.routines.getById("routine")); assertEquals(0, db.mutationDao().getUnsentMutationsCount().first())
            db.close(); db = database(path.toString())
            val reopened = services(db)
            val restored = Json.decodeFromString<Routine>(assertNotNull(reopened.drafts.get("routine", "editor")))
            assertEquals("Last edit", restored.notes); assertEquals("", restored.name); assertTrue(restored.exercises.isEmpty())
            assertEquals("plan-payload", reopened.drafts.get("plan", "editor"))
            reopened.drafts.delete("routine", "editor")
            assertNull(reopened.drafts.get("routine", "editor")); assertEquals("plan-payload", reopened.drafts.get("plan", "editor"))
        } finally { db.close(); Files.deleteIfExists(path) }
    }

    @Test fun orderedDraftPublishDeleteFailureRollsBackAndSuccessCannotResurrect() = runTest {
        val path = Files.createTempFile("publish-draft", ".db")
        val db = database(path.toString())
        try {
            val s = services(db); seed(s)
            val queue = core.storage.OrderedDrafts(s.drafts, ApplicationScope(backgroundScope.coroutineContext))
            val editor = queue.open("routines", "editor").await()
            editor.put("initial"); editor.put("latest raw -")
            editor.barrier().await()
            sql(path.toString(), "CREATE TRIGGER fail_draft_delete BEFORE DELETE ON editor_drafts BEGIN SELECT RAISE(ABORT, 'draft deletion failed'); END")
            assertFailsWith<Exception> { editor.finish {
                s.writes.write { success(s.routines.save(routine(),null)); s.drafts.delete("routines","editor") }
            }.await() }
            assertNull(s.routines.getById("routine")); assertEquals("latest raw -",s.drafts.get("routines","editor"))
            assertEquals(0,db.mutationDao().getUnsentMutationsCount().first())
            sql(path.toString(), "DROP TRIGGER fail_draft_delete")
            editor.put("final"); editor.finish {
                s.writes.write { success(s.routines.save(routine(),null)); s.drafts.delete("routines","editor") }
            }.await()
            assertNotNull(s.routines.getById("routine")); assertNull(s.drafts.get("routines","editor"))
            assertFailsWith<IllegalStateException> { editor.put("late callback").await() }
            assertNull(s.drafts.get("routines","editor"))
        } finally { db.close(); Files.deleteIfExists(path) }
    }

    @Test fun draftAndAggregatePublishJoinSameOuterTransaction() = runTest {
        val db = database()
        try {
            val s = services(db); seed(s)
            s.drafts.put("routine", "editor", "draft")
            assertFailsWith<IllegalStateException> {
                s.writes.write {
                    success(s.routines.save(routine(), null))
                    s.drafts.delete("routine", "editor")
                    error("publish failed")
                }
            }
            assertNull(s.routines.getById("routine")); assertEquals("draft", s.drafts.get("routine", "editor"))
            assertEquals(0, db.mutationDao().getUnsentMutationsCount().first())
            s.writes.write {
                success(s.routines.save(routine(), null))
                s.drafts.delete("routine", "editor")
            }
            assertNotNull(s.routines.getById("routine")); assertNull(s.drafts.get("routine", "editor"))
        } finally { db.close() }
    }

    @Test fun failedNestedCommandRollsBackToSavepointWhenOuterContinues() = runTest {
        val db = database()
        try {
            val s = services(db)
            s.writes.write {
                db.editorDraftDao().put(core.storage.EditorDraftEntity("routine", "outer", "keep"))
                try {
                    s.writes.write {
                        db.editorDraftDao().put(core.storage.EditorDraftEntity("routine", "inner", "rollback"))
                        error("nested failure")
                    }
                } catch (_: IllegalStateException) { }
                db.editorDraftDao().put(core.storage.EditorDraftEntity("routine", "after", "keep"))
            }
            assertEquals("keep", db.editorDraftDao().get("routine", "outer"))
            assertNull(db.editorDraftDao().get("routine", "inner"))
            assertEquals("keep", db.editorDraftDao().get("routine", "after"))
        } finally { db.close() }
    }

    @Test fun cancellationFromCatalogueIsPropagatedAndCannotLeaveRows() = runTest {
        val db = database()
        try {
            val regular = services(db)
            val cancelled = object : ExerciseCatalog by regular.catalogue {
                override suspend fun getExerciseById(id: String): Exercise? = throw CancellationException("catalogue cancelled")
            }
            val s = services(db, cancelled)
            assertFailsWith<CancellationException> { s.routines.save(routine(), null) }
            assertNull(s.routines.getById("routine")); assertEquals(0, db.mutationDao().getUnsentMutationsCount().first())
        } finally { db.close() }
    }
}
