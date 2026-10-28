package app.workflow

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.use
import app.database.*
import core.common.*
import core.database.*
import core.result.Result
import core.storage.*
import features.exercises.api.*
import features.exercises.data.exerciseFeatureModule
import features.routines.api.*
import features.routines.data.routineFeatureModule
import features.trainingplans.api.*
import features.trainingplans.data.trainingPlanFeatureModule
import features.workout.domain.*
import features.workout.workoutFeatureModule
import kotlinx.serialization.json.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.koin.dsl.*
import java.nio.file.Files
import kotlin.test.*

class TrainingWorkflowTest {
    private class Fixture(val db: AppDatabase, val workflow: TrainingWorkflow, val routines: RoutineRepository,
        val plans: TrainingPlanRepository, val catalogue: ExerciseCatalog, val workouts: WorkoutRepository, val drafts: RoutineDraftFactory)
    private fun TestScope.fixture(path: String? = null, outbox: ((MutationDao) -> MutationDao)? = null): Fixture {
        val db = (if(path == null) Room.inMemoryDatabaseBuilder<AppDatabase> { AppDatabaseConstructor.initialize() }
            else Room.databaseBuilder<AppDatabase>(name=path) { AppDatabaseConstructor.initialize() }).configureAppDatabase().build()
        val app = koinApplication { modules(module {
            single { ApplicationScope(backgroundScope.coroutineContext) }; single { db.editorDraftDao() }; single { db.workoutDao() }; single { db.exerciseDao() }; single { db.routineDao() }; single { db.trainingPlanDao() }
            single { outbox?.invoke(db.mutationDao()) ?: db.mutationDao() }
            single<CommandCoordinator> { RoomCommandCoordinator(db, ApplicationScope(backgroundScope.coroutineContext)) }
            single<AppClock> { object: AppClock { override fun nowMillis() = 1000L } }
        }, draftStorageModule(), features.routines.presentation.routinePresentationModule(), exerciseFeatureModule(), routineFeatureModule(), trainingPlanFeatureModule(), workoutFeatureModule()) }
        val k = app.koin
        return Fixture(db, TrainingWorkflow(k.get(), k.get(), k.get(), k.get(), k.get()), k.get(), k.get(), k.get(), k.get(), k.get()).also { app.close() }
    }
    private fun <T> ok(result: Result<T, *>): T = assertIs<Result.Success<T>>(result).data
    private suspend fun Fixture.seed() {
        catalogue.saveExercise(Exercise("bench", "Original bench", MuscleGroup.CHEST, 90))
        ok(routines.save(Routine("routine", "Morning", exercises=List(2) { e ->
            RoutineExercise("entry$e", "bench", 60 + e, List(2) { s -> RoutineSet("set$e$s", 20.0+s, 8+s, WorkoutSetType.DROPSET) }) }), null))
        ok(plans.save(TrainingPlan("plan", "Cycle", slots=listOf(PlanSlot("A", "A", "routine"), PlanSlot("B", "B", "routine"))), null))
    }
    @Test fun prescriptionStartRetainsPreviousCompletedPerformance() = runTest {
        val f=fixture(); try {
            f.seed()
            val previous=f.workouts.startWorkout("Earlier")
            val set=f.workouts.addSet(previous.id,"bench",75.0,6)
            f.workouts.updateSet(set.id) { it.copy(isCompleted=true) }
            ok(f.workflow.complete(previous.id))
            val next=ok(f.workflow.startRoutine("routine")).sessionId
            assertTrue(f.workouts.getSetsForSession(next).first().all { it.previousPerformance == "75.0 kg × 6" })
        } finally { f.db.close() }
    }
    @Test fun concurrentStartsResumeWithoutChangingSnapshotsOrSource() = runTest {
        val f=fixture(); try {
            f.seed()
            val outcomes=List(12) { i -> async { ok(if(i%2==0) f.workflow.startPlan("plan") else f.workflow.startRoutine("routine")) } }.awaitAll()
            assertEquals(1, outcomes.count { it is StartOutcome.Created }); assertEquals(1, outcomes.map { it.sessionId }.distinct().size)
            val id=outcomes.first().sessionId
            assertEquals(1, f.db.workoutDao().getAllSessionsList().size)
            val entries=f.db.workoutDao().getExercisesSnapshot(id)
            assertEquals(2, entries.size); assertEquals(listOf(0,1),entries.map { it.orderIndex })
            val sets=f.workouts.getSetsForSession(id).first(); assertEquals(4, sets.size)
            assertEquals(2, sets.map { it.workoutExerciseId }.distinct().size)
            assertEquals(listOf(20.0,21.0,20.0,21.0),sets.map { it.prescribedWeightKg })
            val session=f.workouts.getSessionById(id)
            val old=f.routines.getById("routine")!!
            ok(f.routines.save(old.copy(name="Changed", exercises=old.exercises.reversed()),old.revision))
            f.catalogue.saveExercise(Exercise("bench","Renamed",MuscleGroup.ARMS,12,true))
            assertEquals(entries, f.db.workoutDao().getExercisesSnapshot(id)); assertEquals(session,f.workouts.getSessionById(id))
            assertEquals(sets,f.workouts.getSetsForSession(id).first())
            val first=sets.first(); f.workouts.updateSet(first.id) { it.copy(weightKg=77.0,reps=3,setType=WorkoutSetType.FAILURE,isCompleted=true) }
            assertEquals(20.0, f.workouts.getSetsForSession(id).first().first().prescribedWeightKg)
            assertEquals(1L,f.plans.getProgress("plan")!!.progressRevision)
        } finally { f.db.close() }
    }
    @Test fun completionAdvancesExactlyOnceAndCancellationDoesNot() = runTest {
        val f=fixture(); try {
            f.seed(); val id=ok(f.workflow.startPlan("plan")).sessionId
            val set=f.workouts.getSetsForSession(id).first().first()
            f.workouts.updateSet(set.id) { it.copy(isCompleted=true,weightKg=30.0,reps=10) }
            val done=ok(f.workflow.complete(id)); assertTrue(done.firstCompletion); assertEquals(300.0,done.session.totalVolumeKg)
            assertEquals("B",f.plans.getProgress("plan")!!.nextSlotId)
            val count=f.db.mutationDao().getUnsentMutationsCount().first()
            assertFalse(ok(f.workflow.complete(id)).firstCompletion)
            assertEquals(count,f.db.mutationDao().getUnsentMutationsCount().first()); assertEquals(2L,f.plans.getProgress("plan")!!.progressRevision)
            f.workouts.cancelWorkout(ok(f.workflow.startPlan("plan")).sessionId)
            assertEquals(2L,f.plans.getProgress("plan")!!.progressRevision)
        } finally { f.db.close() }
    }
    @Test fun changedPlanCommitsCompletedWorkoutWithReviewAndArchivedPlanStaysUnchanged() = runTest {
        val f=fixture(); try {
            f.seed(); val id=ok(f.workflow.startPlan("plan")).sessionId
            ok(f.plans.skipNext("plan",1)); assertTrue(ok(f.workflow.complete(id)).session.isCompleted)
            assertTrue(f.plans.getProgress("plan")!!.needsReview)
            ok(f.plans.confirmNext("plan","A",3)); val next=ok(f.workflow.startPlan("plan")).sessionId
            val before=f.plans.getProgress("plan"); ok(f.plans.archive("plan",1))
            ok(f.routines.archive("routine",1)); assertTrue(ok(f.workflow.complete(next)).session.isCompleted)
            assertEquals(before,f.plans.getProgress("plan"))
        } finally { f.db.close() }
    }
    @Test fun failedThirdSetRollsBackSessionEntriesSetsAndOutbox() = runTest {
        val file=Files.createTempFile("workflow", ".db"); val f=fixture(file.toString())
        try {
            f.seed(); val count=f.db.mutationDao().getUnsentMutationsCount().first()
            sql(file.toString(),"CREATE TRIGGER fail_set BEFORE INSERT ON workout_sets WHEN (SELECT count(*) FROM workout_sets) = 2 BEGIN SELECT RAISE(ABORT, 'third'); END")
            assertEquals(WorkoutError.STORAGE,assertIs<Result.Error<WorkoutError>>(f.workflow.startPlan("plan")).error)
            assertTrue(f.db.workoutDao().getAllSessionsList().isEmpty()); assertEquals(0,scalar(file.toString(),"SELECT count(*) FROM workout_exercises"))
            assertEquals(0,scalar(file.toString(),"SELECT count(*) FROM workout_sets")); assertEquals(count,f.db.mutationDao().getUnsentMutationsCount().first())
        } finally { f.db.close(); Files.deleteIfExists(file) }
    }
    @Test fun progressFailureAndOutboxFailureAbortEarlierCompletionWrites() = runTest {
        for(trigger in listOf("CREATE TRIGGER fail_progress BEFORE UPDATE ON training_plan_progress BEGIN SELECT RAISE(ABORT,'progress'); END",
            "CREATE TRIGGER fail_outbox BEFORE INSERT ON sync_mutations WHEN NEW.entityName = 'training_plan_progress' BEGIN SELECT RAISE(ABORT,'outbox'); END")) {
            val file=Files.createTempFile("completion", ".db"); val f=fixture(file.toString())
            try {
                f.seed(); val id=ok(f.workflow.startPlan("plan")).sessionId; val count=f.db.mutationDao().getUnsentMutationsCount().first()
                sql(file.toString(),trigger)
                assertEquals(WorkoutError.STORAGE,assertIs<Result.Error<WorkoutError>>(f.workflow.complete(id)).error)
                assertFalse(f.workouts.getSessionById(id)!!.isCompleted); assertNull(f.workouts.getSessionById(id)!!.endTimeEpochMs)
                assertEquals(1L,f.plans.getProgress("plan")!!.progressRevision); assertEquals(count,f.db.mutationDao().getUnsentMutationsCount().first())
            } finally { f.db.close(); Files.deleteIfExists(file) }
        }
    }
    @Test fun entryEditsConversionAndExportsKeepRepeatedCatalogueOccurrencesIndependent() = runTest {
        val f=fixture(); try {
            f.seed(); val id=ok(f.workflow.startRoutine("routine")).sessionId
            val entries=f.workouts.getWorkoutExercises(id).first()
            f.workouts.addSetToEntry(id,entries[1].id,55.0,5)
            f.workouts.deleteSet(f.workouts.getSetsForSession(id).first().first().id)
            val sets=f.workouts.getSetsForSession(id).first()
            assertEquals(listOf(1),sets.filter { it.workoutExerciseId==entries[0].id }.map { it.setNumber })
            assertEquals(listOf(1,2,3),sets.filter { it.workoutExerciseId==entries[1].id }.map { it.setNumber })
            assertNull(sets.last().prescribedWeightKg)
            assertFailsWith<IllegalArgumentException> { f.workouts.updateSet(sets.first().id) { it.copy(prescribedWeightKg=999.0) } }
            val before = f.db.mutationDao().getUnsentMutationsCount().first()
            val draftId = f.drafts.fromWorkout(id)
            val draft = Json.parseToJsonElement(f.db.editorDraftDao().get("routines",draftId)!!).jsonObject
            val draftEntries = draft.getValue("exercises").jsonArray
            assertEquals(2, draftEntries.size)
            assertEquals(listOf("21.0"), draftEntries[0].jsonObject.getValue("sets").jsonArray.map { it.jsonObject.getValue("weight").jsonPrimitive.content })
            assertEquals(listOf("20.0","21.0","55.0"), draftEntries[1].jsonObject.getValue("sets").jsonArray.map { it.jsonObject.getValue("weight").jsonPrimitive.content })
            assertEquals("DROPSET",draftEntries[1].jsonObject.getValue("sets").jsonArray.first().jsonObject.getValue("type").jsonPrimitive.content)
            assertEquals("61",draftEntries[1].jsonObject.getValue("rest").jsonPrimitive.content)
            assertEquals(1,f.routines.observeAll().first().size)
            assertEquals(before,f.db.mutationDao().getUnsentMutationsCount().first())
            f.catalogue.saveExercise(Exercise("bench","Renamed",MuscleGroup.ARMS,12,true))
            val csv=WorkoutDataExporter.exportToCsv(listOf(f.workouts.getSessionById(id)!!),f.catalogue.getAllExercises(),sets)
            assertTrue(csv.contains("Original bench")); assertFalse(csv.contains("Renamed")); assertTrue(csv.contains(entries[1].id))
            val json= kotlinx.serialization.json.Json.parseToJsonElement(WorkoutDataExporter.exportToJson(
                listOf(f.workouts.getSessionById(id)!!),f.catalogue.getAllExercises(),sets,workoutExercises=entries)).jsonObject
            val exported=json.getValue("sets").jsonArray
            assertEquals("Original bench",exported.first().jsonObject.getValue("exerciseNameSnapshot").jsonPrimitive.content)
            assertEquals(kotlinx.serialization.json.JsonNull,exported.last().jsonObject.getValue("prescribedWeightKg"))
            f.workouts.removeWorkoutExercise(id,entries[0].id)
            assertEquals(listOf(entries[1]),f.workouts.getWorkoutExercises(id).first())
            assertEquals(3,f.workouts.getSetsForSession(id).first().size)
            assertFailsWith<IllegalStateException> { f.workouts.addSetToEntry(id,entries[0].id,10.0,8) }
            assertEquals(listOf(entries[1]),f.workouts.getWorkoutExercises(id).first())
        } finally { f.db.close() }
    }
    @Test fun startOutboxFailureRollsBackAndInvalidOrArchivedReferencesNeverStart() = runTest {
        var fail=false
        val f=fixture(outbox={ original -> object: MutationDao by original {
            override suspend fun insertMutation(mutation: SyncMutationEntity) {
                if(fail && mutation.entityName=="workout_set") error("Outbox failure")
                original.insertMutation(mutation)
            }
        } })
        try {
            f.seed(); val count=f.db.mutationDao().getUnsentMutationsCount().first(); fail=true
            assertEquals(WorkoutError.STORAGE,assertIs<Result.Error<WorkoutError>>(f.workflow.startRoutine("routine")).error)
            assertTrue(f.db.workoutDao().getAllSessionsList().isEmpty()); assertEquals(count,f.db.mutationDao().getUnsentMutationsCount().first())
            fail=false; f.catalogue.archive("bench")
            assertEquals(WorkoutError.ARCHIVED,assertIs<Result.Error<WorkoutError>>(f.workflow.startRoutine("routine")).error)
            assertEquals(WorkoutError.NOT_FOUND,assertIs<Result.Error<WorkoutError>>(f.workflow.startRoutine("absent")).error)
            assertTrue(f.db.workoutDao().getAllSessionsList().isEmpty())
        } finally { f.db.close() }
    }
    @Test fun changedPlanDefinitionAndArchivedRoutineDoNotPreventCompletion() = runTest {
        val f=fixture(); try {
            f.seed(); val id=ok(f.workflow.startPlan("plan")).sessionId
            ok(f.routines.archive("routine",1))
            val plan=f.plans.getById("plan")!!
            ok(f.plans.save(plan.copy(name="Changed"),1))
            assertTrue(ok(f.workflow.complete(id)).session.isCompleted)
            assertTrue(f.plans.getProgress("plan")!!.needsReview)
            assertEquals("A",f.plans.getProgress("plan")!!.nextSlotId)
        } finally { f.db.close() }
    }
    @Test fun completedEmptyEntriesAndSessionsCannotBeRemovedOrCancelled() = runTest {
        val f=fixture(); try {
            f.seed(); val session=f.workouts.startWorkout("Empty entry")
            val entry=f.workouts.addExerciseEntry(session.id,"bench")
            ok(f.workflow.complete(session.id))
            assertFailsWith<IllegalStateException> { f.workouts.removeExerciseFromWorkout(session.id,entry.id) }
            assertFailsWith<IllegalStateException> { f.workouts.cancelWorkout(session.id) }
            assertEquals(listOf(entry),f.workouts.getWorkoutExercises(session.id).first())
            assertTrue(f.workouts.getSessionById(session.id)!!.isCompleted)
        } finally { f.db.close() }
    }
    @Test fun catalogueArchiveCommitsOneMutationAndRollsBackOnOutboxFailure() = runTest {
        var reject=false
        val f=fixture(outbox={ original -> object: MutationDao by original {
            override suspend fun insertMutation(mutation: SyncMutationEntity) {
                if(reject && mutation.entityName=="exercise") error("Outbox failure")
                original.insertMutation(mutation)
            }
        } })
        try {
            f.seed(); val before=f.db.mutationDao().getUnsentMutationsCount().first()
            f.catalogue.archive("bench"); f.catalogue.archive("bench")
            assertTrue(f.catalogue.getExerciseById("bench")!!.archived)
            assertEquals(before+1,f.db.mutationDao().getUnsentMutationsCount().first())
            f.catalogue.saveExercise(Exercise("other","Other",MuscleGroup.BACK)); reject=true
            assertFailsWith<IllegalStateException> { f.catalogue.archive("other") }
            assertFalse(f.catalogue.getExerciseById("other")!!.archived)
            assertEquals(before+1,f.db.mutationDao().getUnsentMutationsCount().first())
        } finally { f.db.close() }
    }
    private fun sql(path: String, sql: String) { val c=BundledSQLiteDriver().open(path); try { c.prepare(sql).use { it.step() } } finally { c.close() } }
    private fun scalar(path: String, sql: String): Int { val c=BundledSQLiteDriver().open(path); return try { c.prepare(sql).use { it.step(); it.getLong(0).toInt() } } finally { c.close() } }
}
