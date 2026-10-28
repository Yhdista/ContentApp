package features.trainingplans

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.use
import app.database.AppDatabase
import app.database.AppDatabaseConstructor
import app.database.configureAppDatabase
import core.common.AppClock
import core.common.ApplicationScope
import core.result.Result
import core.storage.CommandCoordinator
import core.storage.RoomCommandCoordinator
import features.routines.api.*
import features.trainingplans.api.*
import features.trainingplans.data.trainingPlanFeatureModule
import features.workout.domain.PlanWorkoutSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import java.nio.file.Files
import kotlin.test.*

class TrainingPlanPersistenceTest {
    private fun database(path: String? = null): AppDatabase =
        (if (path == null) Room.inMemoryDatabaseBuilder<AppDatabase> { AppDatabaseConstructor.initialize() }
        else Room.databaseBuilder<AppDatabase>(name = path) { AppDatabaseConstructor.initialize() })
            .configureAppDatabase().build()

    // Plans consume routines through their public boundary; no dependency on routine storage or catalogue impl.
    private val routine = Routine("routine", "Morning", revision = 1, exercises = listOf(
        RoutineExercise("entry", "bench", 90, listOf(RoutineSet("set", 20.0, 8)))))
    private fun routines(value: Routine? = routine): RoutineRepository = object : RoutineRepository {
        override suspend fun getById(id: String) = value?.takeIf { it.id == id }
        override fun observeById(id: String) = flowOf(value?.takeIf { it.id == id })
        override fun observeAll(includeArchived: Boolean) = flowOf(listOfNotNull(value))
        override suspend fun save(routine: Routine, expectedRevision: Long?): Result<Routine, RoutineError> = error("unused")
        override suspend fun duplicate(id: String): Result<Routine, RoutineError> = error("unused")
        override suspend fun archive(id: String, expectedRevision: Long): Result<Unit, RoutineError> = error("unused")
    }
    private data class Services(val plans: TrainingPlanRepository, val writes: CommandCoordinator)
    private fun TestScope.services(db: AppDatabase, routines: RoutineRepository = routines()): Services {
        val writes = RoomCommandCoordinator(db, ApplicationScope(backgroundScope.coroutineContext))
        val app = koinApplication {
            modules(module {
                single { db.trainingPlanDao() }
                single { db.mutationDao() }
                single<RoutineRepository> { routines }
                single<CommandCoordinator> { writes }
                single<AppClock> { object : AppClock { override fun nowMillis() = 2000L } }
            }, trainingPlanFeatureModule())
        }
        return Services(app.koin.get(), writes).also { app.close() }
    }
    private fun plan(id: String = "plan", count: Int = 3) = TrainingPlan(id, " Cycle ", "Notes \"x\"\n", slots =
        List(count) { PlanSlot("$id-${"ABC"[it]}", " Day ${"ABC"[it]} ", "routine") })
    private fun <T> success(result: Result<T, PlanError>): T = assertIs<Result.Success<T>>(result).data
    private fun error(code: PlanError, result: Result<*, PlanError>) = assertEquals(code, assertIs<Result.Error<PlanError>>(result).error)
    private suspend fun pending(db: AppDatabase) = db.mutationDao().getPendingMutations().first()
    private fun sql(path: String, statement: String) {
        val c = BundledSQLiteDriver().open(path)
        try { c.prepare(statement).use { it.step() } } finally { c.close() }
    }

    @Test fun savePersistsStableOrderedSlotsRepeatedRoutineAndTypedOutbox() = runTest {
        val db = database()
        try {
            val p = services(db).plans
            val saved = success(p.save(plan(), null))
            assertEquals("Cycle", saved.name); assertEquals(1L, saved.revision)
            assertEquals(listOf("Day A", "Day B", "Day C"), saved.slots.map { it.label })
            assertEquals(listOf("plan-A", "plan-B", "plan-C"), saved.slots.map { it.id })
            assertEquals(saved, p.getById(saved.id)); assertEquals(saved, p.observeById(saved.id).first())
            assertEquals(listOf(saved), p.observeAll().first())
            assertEquals(PlanProgress("plan", "plan-A", 1, false), p.getProgress("plan"))
            assertEquals(p.getProgress("plan"), p.observeProgress("plan").first())
            assertEquals(saved, Json.decodeFromString<TrainingPlan>(Json.encodeToString(saved)))
            val rows = pending(db)
            assertEquals(mapOf("training_plan" to 1, "training_plan_slot" to 3, "training_plan_progress" to 1), rows.groupingBy { it.entityName }.eachCount())
            assertTrue(rows.all { it.operation == "INSERT" && it.createdAt == 2000L })
            val parent = Json.parseToJsonElement(rows.single { it.entityName == "training_plan" }.payloadJson).jsonObject
            assertEquals("Notes \"x\"\n", parent["notes"]?.jsonPrimitive?.content)
            assertEquals("1", parent["revision"]?.jsonPrimitive?.content)
            val slot = Json.parseToJsonElement(rows.single { it.recordId == "plan-C" }.payloadJson).jsonObject
            assertEquals("2", slot["orderIndex"]?.jsonPrimitive?.content)
            assertEquals("plan", slot["planId"]?.jsonPrimitive?.content)
            val progress = Json.parseToJsonElement(rows.single { it.entityName == "training_plan_progress" }.payloadJson).jsonObject
            assertEquals("plan-A", progress["nextSlotId"]?.jsonPrimitive?.content)
        } finally { db.close() }
    }

    @Test fun saveNormalizesOrderRetainsIdsAndRejectsStaleRevision() = runTest {
        val db = database()
        try {
            val p = services(db).plans
            val old = success(p.save(plan(), null))
            val edited = success(p.save(old.copy(slots = old.slots.reversed()), 1))
            assertEquals(2L, edited.revision); assertEquals(listOf("plan-C", "plan-B", "plan-A"), p.getById("plan")!!.slots.map { it.id })
            assertEquals(PlanProgress("plan", "plan-A", 1, false), p.getProgress("plan"))
            val count = pending(db).size
            error(PlanError.REVISION_CONFLICT, p.save(old.copy(name = "Lost"), 1))
            error(PlanError.REVISION_CONFLICT, p.save(old, null))
            error(PlanError.REVISION_CONFLICT, p.archive(old.id, 1))
            assertEquals(edited, p.getById("plan")); assertEquals(count, pending(db).size)
            assertTrue(pending(db).any { it.recordId == "plan-A" && it.operation == "UPDATE" })
        } finally { db.close() }
    }

    @Test fun removingCursorAtomicallyRequiresExplicitConfirmation() = runTest {
        val db = database()
        try {
            val p = services(db).plans
            val saved = success(p.save(plan(), null))
            success(p.save(saved.copy(slots = saved.slots.drop(1)), 1))
            assertEquals(PlanProgress("plan", null, 2, true), p.getProgress("plan"))
            error(PlanError.REVIEW_REQUIRED, p.prepareStart("plan"))
            error(PlanError.REVIEW_REQUIRED, p.skipNext("plan", 2))
            error(PlanError.REVISION_CONFLICT, p.confirmNext("plan", "plan-B", 1))
            error(PlanError.NOT_FOUND, p.confirmNext("plan", "removed", 2))
            assertEquals(PlanProgress("plan", "plan-C", 3, false), success(p.confirmNext("plan", "plan-C", 2)))
            val context = success(p.prepareStart("plan"))
            assertEquals("plan-C", context.slot.id); assertEquals(PlanWorkoutSource("plan", 2, "plan-C", 3), context.source)
            val rows = pending(db)
            assertTrue(rows.any { it.entityName == "training_plan_slot" && it.recordId == "plan-A" && it.operation == "DELETE" })
            val removed = Json.parseToJsonElement(rows.single { it.recordId == "plan-A" && it.operation == "DELETE" }.payloadJson).jsonObject
            assertEquals("plan-A", removed["id"]?.jsonPrimitive?.content)
        } finally { db.close() }
    }

    @Test fun startOnlyReturnsCurrentSlotAndNeverAdvancesCursor() = runTest {
        val db = database()
        try {
            val p = services(db).plans
            success(p.save(plan(), null))
            val count = pending(db).size
            repeat(2) { assertEquals(PlanWorkoutSource("plan", 1, "plan-A", 1), success(p.prepareStart("plan")).source) }
            assertEquals(count, pending(db).size); assertEquals(1L, p.getProgress("plan")!!.progressRevision)
            assertEquals("plan-B", success(p.skipNext("plan", 1)).nextSlotId)
            assertEquals("plan-B", success(p.prepareStart("plan")).slot.id)
            error(PlanError.REVISION_CONFLICT, p.skipNext("plan", 1))
        } finally { db.close() }
    }

    @Test fun completionCyclesABCAndOneSlotIncrementsProgressRevision() = runTest {
        val db = database()
        try {
            val p = services(db).plans
            success(p.save(plan(), null))
            for ((slot, revision) in listOf("plan-B" to 2L, "plan-C" to 3L, "plan-A" to 4L)) {
                val source = success(p.prepareStart("plan")).source
                val result = assertIs<ProgressOutcome.Advanced>(success(p.recordCompleted(source)))
                assertEquals(PlanProgress("plan", slot, revision, false), result.progress)
            }
            success(p.save(plan("single", 1), null))
            val source = success(p.prepareStart("single")).source
            assertEquals(PlanProgress("single", "single-A", 2, false), assertIs<ProgressOutcome.Advanced>(success(p.recordCompleted(source))).progress)
            assertEquals(0, db.workoutDao().getAllSessionsList().size)
        } finally { db.close() }
    }

    @Test fun skipWhileWorkoutRunsRequiresReviewEvenAfterFullWrapOrOneSlot() = runTest {
        val db = database()
        try {
            val p = services(db).plans
            for ((id, n) in listOf("plan" to 3, "single" to 1)) {
                success(p.save(plan(id, n), null))
                val source = success(p.prepareStart(id)).source
                repeat(n) { success(p.skipNext(id, it + 1L)) }
                assertEquals(source.slotId, p.getProgress(id)!!.nextSlotId)
                val outcome = assertIs<ProgressOutcome.ReviewRequired>(success(p.recordCompleted(source)))
                assertEquals(source.slotId, outcome.progress.nextSlotId); assertTrue(outcome.progress.needsReview)
                assertEquals(n + 2L, outcome.progress.progressRevision)
                error(PlanError.REVIEW_REQUIRED, p.prepareStart(id))
                error(PlanError.REVIEW_REQUIRED, p.skipNext(id, outcome.progress.progressRevision))
                val confirmed = success(p.confirmNext(id, source.slotId, outcome.progress.progressRevision))
                assertFalse(confirmed.needsReview); assertEquals(n + 3L, confirmed.progressRevision)
            }
        } finally { db.close() }
    }

    @Test fun editedSourceAndDifferentCursorAreReviewOutcomesNotErrors() = runTest {
        val db = database()
        try {
            val p = services(db).plans
            val saved = success(p.save(plan(), null))
            val source = success(p.prepareStart("plan")).source
            success(p.save(saved.copy(name = "Edited"), 1))
            assertIs<ProgressOutcome.ReviewRequired>(success(p.recordCompleted(source)))
            success(p.confirmNext("plan", "plan-A", 2))
            val currentSource = success(p.prepareStart("plan")).source
            success(p.skipNext("plan", 3))
            val result = assertIs<ProgressOutcome.ReviewRequired>(success(p.recordCompleted(currentSource)))
            assertEquals("plan-B", result.progress.nextSlotId)
            assertEquals(5L, result.progress.progressRevision)
        } finally { db.close() }
    }

    @Test fun activeSelectionPersistsWithoutOutboxOrResetAndCompletionUsesSourcePlan() = runTest {
        val path = Files.createTempFile("plan-selection", ".db")
        var db = database(path.toString())
        try {
            var p = services(db).plans
            success(p.save(plan(), null)); success(p.save(plan("other"), null))
            success(p.selectActive("plan"))
            val source = success(p.prepareStart("plan")).source
            success(p.skipNext("other", 1))
            val count = pending(db).size
            success(p.selectActive("other"))
            assertEquals(count, pending(db).size)
            db.close(); db = database(path.toString()); p = services(db).plans
            assertEquals("other", p.observeActive().first()!!.id)
            assertIs<ProgressOutcome.Advanced>(success(p.recordCompleted(source)))
            assertEquals(PlanProgress("plan", "plan-B", 2, false), p.getProgress("plan"))
            assertEquals(PlanProgress("other", "other-B", 2, false), p.getProgress("other"))
            success(p.selectActive("plan")); assertEquals(2L, p.getProgress("other")!!.progressRevision)
            success(p.selectActive(null)); assertNull(p.observeActive().first())
        } finally { db.close(); Files.deleteIfExists(path) }
    }

    @Test fun archiveTakesPrecedenceAndDuplicateUsesFreshIdsAndIndependentProgress() = runTest {
        val db = database()
        try {
            val p = services(db).plans
            val saved = success(p.save(plan(), null)); success(p.selectActive("plan"))
            val source = success(p.prepareStart("plan")).source
            success(p.skipNext("plan", 1))
            success(p.archive("plan", 1))
            val count = pending(db).size
            val result = assertIs<ProgressOutcome.ArchivedUnchanged>(success(p.recordCompleted(source)))
            assertEquals(PlanProgress("plan", "plan-B", 2, false), result.progress)
            assertEquals(count, pending(db).size)
            assertNull(p.observeActive().first()); assertTrue(p.observeAll().first().isEmpty())
            val archived = p.observeAll(true).first().single()
            assertTrue(archived.archived); assertEquals(2L, archived.revision); assertEquals(saved.slots, archived.slots)
            error(PlanError.ARCHIVED, p.prepareStart("plan")); error(PlanError.ARCHIVED, p.selectActive("plan"))
            error(PlanError.ARCHIVED, p.skipNext("plan", 2)); error(PlanError.ARCHIVED, p.confirmNext("plan", "plan-A", 2))
            error(PlanError.ARCHIVED, p.archive("plan", 2)); error(PlanError.ARCHIVED, p.save(saved, 2))
            val copy = success(p.duplicate("plan"))
            assertNotEquals("plan", copy.id); assertEquals(1L, copy.revision); assertFalse(copy.archived)
            assertEquals(saved.slots.map { it.label to it.routineId }, copy.slots.map { it.label to it.routineId })
            assertTrue(copy.slots.none { it.id in saved.slots.map { it.id } })
            assertEquals(PlanProgress(copy.id, copy.slots.first().id, 1, false), p.getProgress(copy.id))
        } finally { db.close() }
    }

    @Test fun archivedRoutineBlocksStartAndNewReferencesButPreservedSlotCanBeRepaired() = runTest {
        val db = database()
        try {
            val saved = success(services(db).plans.save(plan(), null))
            val p = services(db, routines(routine.copy(archived = true))).plans
            error(PlanError.ARCHIVED, p.prepareStart("plan"))
            success(p.save(saved.copy(name = "Repair"), 1))
            val current = p.getById("plan")!!
            error(PlanError.ARCHIVED, p.save(current.copy(slots = current.slots + PlanSlot("new", "New", "routine")), 2))
            error(PlanError.ARCHIVED, p.duplicate("plan"))
            // Skip and confirmation may select an archived routine so that a blocked day can be skipped.
            assertEquals("plan-B", success(p.skipNext("plan", 1)).nextSlotId)
        } finally { db.close() }
    }

    @Test fun missingAndValidationErrorsRemainTypedWithoutWrites() = runTest {
        val db = database()
        try {
            val p = services(db, routines(null)).plans
            error(PlanError.VALIDATION, p.save(plan().copy(name = " "), null))
            error(PlanError.VALIDATION, p.save(plan().copy(slots = List(2) { PlanSlot("same", "Day", "routine") }), null))
            error(PlanError.NOT_FOUND, p.save(plan(), null)); error(PlanError.NOT_FOUND, p.save(plan(), 1))
            error(PlanError.NOT_FOUND, p.duplicate("missing")); error(PlanError.NOT_FOUND, p.archive("missing", 1))
            error(PlanError.NOT_FOUND, p.prepareStart("missing")); error(PlanError.NOT_FOUND, p.selectActive("missing"))
            error(PlanError.NOT_FOUND, p.skipNext("missing", 1)); error(PlanError.NOT_FOUND, p.confirmNext("missing", "A", 1))
            error(PlanError.NOT_FOUND, p.recordCompleted(PlanWorkoutSource("missing", 1, "A", 1)))
            assertNull(p.getById("plan")); assertNull(p.getProgress("plan")); assertEquals(0, pending(db).size)
        } finally { db.close() }
    }

    @Test fun childIdCollisionCannotStealAnotherPlansSlot() = runTest {
        val db = database()
        try {
            val p = services(db).plans
            val saved = success(p.save(plan(), null))
            val count = pending(db).size
            error(PlanError.STORAGE, p.save(saved.copy(id = "other"), null))
            assertNull(p.getById("other")); assertNull(p.getProgress("other")); assertEquals(saved, p.getById("plan"))
            assertEquals(count, pending(db).size)
        } finally { db.close() }
    }

    @Test fun failedChildInsertRollsBackDefinitionChildrenProgressAndOutbox() = runTest {
        val path = Files.createTempFile("plan-child-rollback", ".db")
        val db = database(path.toString())
        try {
            val p = services(db).plans
            val saved = success(p.save(plan(), null)); val count = pending(db).size
            sql(path.toString(), "CREATE TRIGGER fail_slot BEFORE INSERT ON training_plan_slots WHEN NEW.id = 'fail' BEGIN SELECT RAISE(ABORT, 'slot rejected'); END")
            error(PlanError.STORAGE, p.save(saved.copy(slots = listOf(PlanSlot("fail", "New", "routine"))), 1))
            assertEquals(saved, p.getById("plan")); assertEquals(PlanProgress("plan", "plan-A", 1, false), p.getProgress("plan"))
            assertEquals(count, pending(db).size)
        } finally { db.close(); Files.deleteIfExists(path) }
    }

    @Test fun failedProgressOutboxRollsBackEntireSaveIncludingInsideSuccessfulOuterCommand() = runTest {
        val path = Files.createTempFile("plan-outbox-rollback", ".db")
        val db = database(path.toString())
        try {
            val s = services(db); val p = s.plans
            val saved = success(p.save(plan(), null)); val count = pending(db).size
            sql(path.toString(), "CREATE TRIGGER fail_progress_outbox BEFORE INSERT ON sync_mutations WHEN NEW.entityName = 'training_plan_progress' BEGIN SELECT RAISE(ABORT, 'outbox rejected'); END")
            s.writes.write {
                db.editorDraftDao().put(core.storage.EditorDraftEntity("plan", "outer", "keep"))
                error(PlanError.STORAGE, p.save(saved.copy(slots = saved.slots.drop(1)), 1))
            }
            assertEquals("keep", db.editorDraftDao().get("plan", "outer"))
            assertEquals(saved, p.getById("plan")); assertEquals(PlanProgress("plan", "plan-A", 1, false), p.getProgress("plan"))
            error(PlanError.STORAGE, p.recordCompleted(success(p.prepareStart("plan")).source))
            assertEquals(PlanProgress("plan", "plan-A", 1, false), p.getProgress("plan"))
            assertEquals(count, pending(db).size)
        } finally { db.close(); Files.deleteIfExists(path) }
    }

    @Test fun completionIsTransactionParticipantAndOuterFailureRollsBackProgressAndOutbox() = runTest {
        val db = database()
        try {
            val s = services(db); val p = s.plans
            success(p.save(plan(), null)); val source = success(p.prepareStart("plan")).source
            val count = pending(db).size
            assertFailsWith<IllegalStateException> {
                s.writes.write {
                    assertIs<ProgressOutcome.Advanced>(success(p.recordCompleted(source)))
                    error("outer completion failed")
                }
            }
            assertEquals(PlanProgress("plan", "plan-A", 1, false), p.getProgress("plan")); assertEquals(count, pending(db).size)
        } finally { db.close() }
    }

    @Test fun cancellationFromRoutineApiPropagatesWithoutPartialRows() = runTest {
        val db = database()
        try {
            val cancelled = object : RoutineRepository by routines() {
                override suspend fun getById(id: String): Routine? = throw CancellationException("routine cancelled")
            }
            val p = services(db, cancelled).plans
            assertFailsWith<CancellationException> { p.save(plan(), null) }
            assertNull(p.getById("plan")); assertNull(p.getProgress("plan")); assertEquals(0, pending(db).size)
        } finally { db.close() }
    }

    @Test fun nullCursorWithoutReviewFlagStillBlocksStartAndSkipUntilConfirmation() = runTest {
        val db = database()
        try {
            val p = services(db).plans
            success(p.save(plan(), null))
            assertEquals(1, db.trainingPlanDao().updateProgress("plan", null, 2, false, 1))
            error(PlanError.REVIEW_REQUIRED, p.prepareStart("plan"))
            error(PlanError.REVIEW_REQUIRED, p.skipNext("plan", 2))
            assertEquals(PlanProgress("plan", "plan-B", 3, false), success(p.confirmNext("plan", "plan-B", 2)))
            // Explicit confirmation counts even when it selects the same already-current slot.
            assertEquals(PlanProgress("plan", "plan-B", 4, false), success(p.confirmNext("plan", "plan-B", 3)))
            assertEquals(PlanWorkoutSource("plan", 1, "plan-B", 4), success(p.prepareStart("plan")).source)
        } finally { db.close() }
    }

    @Test fun sourceRoutineArchivedOrMissingAfterStartDoesNotPreventCompletion() = runTest {
        val db = database()
        try {
            val p = services(db).plans
            success(p.save(plan(), null)); success(p.save(plan("other"), null))
            val source = success(p.prepareStart("plan")).source
            val otherSource = success(p.prepareStart("other")).source
            val archived = services(db, routines(routine.copy(archived = true))).plans
            error(PlanError.ARCHIVED, archived.prepareStart("plan"))
            assertEquals(PlanProgress("plan", "plan-B", 2, false), assertIs<ProgressOutcome.Advanced>(success(archived.recordCompleted(source))).progress)
            val missing = services(db, routines(null)).plans
            error(PlanError.NOT_FOUND, missing.prepareStart("other"))
            assertEquals(PlanProgress("other", "other-B", 2, false), assertIs<ProgressOutcome.Advanced>(success(missing.recordCompleted(otherSource))).progress)
        } finally { db.close() }
    }

    @Test fun progressCasFailureRetainsTypedConflictAndRollsBackPartialDefinitionSave() = runTest {
        val path = Files.createTempFile("plan-cas-rollback", ".db")
        val db = database(path.toString())
        try {
            val p = services(db).plans
            val saved = success(p.save(plan(), null)); val count = pending(db).size
            sql(path.toString(), "CREATE TRIGGER ignore_progress BEFORE UPDATE ON training_plan_progress BEGIN SELECT RAISE(IGNORE); END")
            error(PlanError.REVISION_CONFLICT, p.save(saved.copy(slots = saved.slots.drop(1)), 1))
            assertEquals(saved, p.getById("plan")); assertEquals(PlanProgress("plan", "plan-A", 1, false), p.getProgress("plan"))
            error(PlanError.REVISION_CONFLICT, p.skipNext("plan", 1))
            error(PlanError.REVISION_CONFLICT, p.confirmNext("plan", "plan-B", 1))
            error(PlanError.REVISION_CONFLICT, p.recordCompleted(success(p.prepareStart("plan")).source))
            assertEquals(count, pending(db).size)
        } finally { db.close(); Files.deleteIfExists(path) }
    }

}
