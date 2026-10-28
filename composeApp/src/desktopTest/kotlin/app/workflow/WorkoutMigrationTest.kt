package app.workflow

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.use
import app.database.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

class WorkoutMigrationTest {
    @Test fun actualSchemaSixMigratesLosslesslyAndRoomValidatesSchemaSeven() = runTest {
        val file=Files.createTempFile("actual-v6", ".db")
        val connection=BundledSQLiteDriver().open(file.toString())
        val payload="{\"name\":\"quote \\\" slash \\\\ newline \\n\"}"
        fun sql(text: String) { connection.prepare(text).use { it.step() } }
        try {
            val schema=Json.parseToJsonElement(Files.readString(Path.of("schemas/app.database.AppDatabase/6.json"))).jsonObject.getValue("database").jsonObject
            schema.getValue("entities").jsonArray.forEach { item ->
                val e=item.jsonObject
                sql(e.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}",e.getValue("tableName").jsonPrimitive.content))
                e["indices"]?.jsonArray?.forEach { sql(it.jsonObject.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}",e.getValue("tableName").jsonPrimitive.content)) }
            }
            schema.getValue("setupQueries").jsonArray.forEach { sql(it.jsonPrimitive.content) }
            sql("PRAGMA user_version = 6")
            sql("INSERT INTO exercises VALUES ('bench','Original bench','CHEST',123)")
            sql("INSERT INTO workout_templates VALUES ('template','Legacy routine','notes'),('invalid','Invalid','keep'),('oversized','Oversized','keep all')")
            sql("INSERT INTO workout_template_exercises VALUES ('template','bench','Old display',2,12.5,7,0),('template','bench','Second occurrence',1,50,3,1),('template','missing','Lost name',1,0,9,2),('invalid','bench','Invalid scalar',-2,-10,-8,9),('oversized','bench','Large',21,0,1000,0)")
            sql("INSERT INTO workout_sessions VALUES ('session','Old session',1234,5678,1,123.5)")
            sql("INSERT INTO workout_sets VALUES ('set-a','session','bench',1,12.5,7,8.0,1,'old performance',1,'DROPSET'),('set-b','session','missing-history',3,4.5,12,NULL,0,NULL,0,'NORMAL')")
            connection.prepare("INSERT INTO sync_mutations VALUES ('legacy-id','workout_template','template','INSERT',?,99,2,'old-error')").use { it.bindText(1,payload); it.step() }
        } finally { connection.close() }
        val db=Room.databaseBuilder<AppDatabase>(name=file.toString()) { AppDatabaseConstructor.initialize() }.configureAppDatabase().build()
        try {
            // Opening production Room runs migration and validates its result against exported v7.
            val session=db.workoutDao().getSessionById("session")!!
            assertEquals(123.5,session.totalVolumeKg); assertEquals(1234,session.startTimeEpochMs); assertEquals(5678,session.endTimeEpochMs)
            assertNull(session.sourceRoutineId); assertNull(session.sourcePlanId)
            val sets=db.workoutDao().getSetsForSessionSnapshot("session")
            assertEquals(setOf("set-a","set-b"),sets.map { it.id }.toSet())
            val a=sets.single { it.id=="set-a" }; assertEquals(12.5,a.weightKg); assertEquals(7,a.reps); assertEquals(8.0,a.rpe)
            assertEquals("old performance",a.previousPerformance); assertTrue(a.isPersonalRecord); assertEquals("DROPSET",a.setType)
            assertTrue(sets.all { it.workoutExerciseId!=null && it.prescribedWeightKg==null && it.prescribedReps==null && it.prescribedSetType==null })
            val entries=db.workoutDao().getExercisesSnapshot("session"); assertEquals(listOf(0,1),entries.map { it.orderIndex }); assertTrue(entries.all { it.orderReconstructed })
            assertEquals("Original bench",db.exerciseDao().getExerciseById("bench")!!.name)
            assertEquals("Lost name",db.exerciseDao().getExerciseById("missing")!!.name); assertTrue(db.exerciseDao().getExerciseById("missing")!!.archived)
            assertTrue(db.exerciseDao().getExerciseById("missing-history")!!.archived)
            val routine=db.routineDao().getById("template")!!
            assertEquals("Legacy routine",routine.routine.name); assertEquals(3,routine.exercises.size)
            assertEquals(2,routine.exercises.count { it.entry.exerciseId=="bench" })
            assertEquals(4,routine.exercises.sumOf { it.sets.size }); assertEquals(123,routine.exercises.first { it.entry.orderIndex==0 }.entry.restSeconds)
            assertTrue(routine.exercises.flatMap { it.sets }.all { it.setType=="NORMAL" })
            assertEquals(21,db.routineDao().getById("oversized")!!.exercises.single().sets.size)
            val invalid=db.routineDao().getById("invalid")!!.exercises.single()
            assertEquals(-2,invalid.entry.legacyTargetSets); assertEquals(-10.0,invalid.entry.legacyDefaultWeightKg); assertEquals(-8,invalid.entry.legacyDefaultReps)
            assertTrue(invalid.sets.isEmpty())
            val c=BundledSQLiteDriver().open(file.toString()); try {
                c.prepare("SELECT id,payloadJson,entityName,retryCount,lastError FROM sync_mutations").use {
                    assertTrue(it.step()); assertEquals("legacy-id",it.getText(0)); assertEquals(payload,it.getText(1)); assertEquals("workout_template",it.getText(2)); assertEquals(2,it.getLong(3)); assertEquals("old-error",it.getText(4)); assertFalse(it.step())
                }
                c.prepare("SELECT count(*) FROM sqlite_master WHERE name IN ('workout_templates','workout_template_exercises')").use { it.step(); assertEquals(0,it.getLong(0)) }
            } finally { c.close() }
            val app=org.koin.dsl.koinApplication { modules(org.koin.dsl.module {
                single { db.routineDao() }; single { db.exerciseDao() }; single { db.workoutDao() }; single { db.trainingPlanDao() }; single { db.mutationDao() }
                single<core.storage.CommandCoordinator> { core.storage.RoomCommandCoordinator(db,core.common.ApplicationScope(backgroundScope.coroutineContext)) }
                single<core.common.AppClock> { object: core.common.AppClock { override fun nowMillis()=1000L } }
            },features.routines.data.routineFeatureModule(),features.exercises.data.exerciseFeatureModule(),features.workout.workoutFeatureModule(),features.trainingplans.data.trainingPlanFeatureModule()) }
            try {
                val repo=app.koin.get<features.routines.api.RoutineRepository>()
                val workflow=TrainingWorkflow(app.koin.get(),repo,app.koin.get(),app.koin.get(),app.koin.get())
                assertEquals(features.workout.domain.WorkoutError.VALIDATION,assertIs<core.result.Result.Error<features.workout.domain.WorkoutError>>(workflow.startRoutine("invalid")).error)
                assertEquals(features.workout.domain.WorkoutError.VALIDATION,assertIs<core.result.Result.Error<features.workout.domain.WorkoutError>>(workflow.startRoutine("oversized")).error)
                val original=repo.getById("template")!!
                val saved=assertIs<core.result.Result.Success<features.routines.api.Routine>>(repo.save(original.copy(name="Renamed routine"),1)).data
                assertEquals(original.exercises.map { it.legacyValues },saved.exercises.map { it.legacyValues })
                assertEquals(saved,repo.getById("template"))
                val invalidRoutine=repo.getById("invalid")!!
                val repaired=invalidRoutine.copy(exercises=invalidRoutine.exercises.map { it.copy(sets=listOf(features.routines.api.RoutineSet("repair",0.0,8)),legacyValues=null) })
                assertIs<core.result.Result.Success<*>>(repo.save(repaired,1))
                assertNull(repo.getById("invalid")!!.exercises.single().legacyValues)
                assertIs<core.result.Result.Success<*>>(workflow.startRoutine("invalid"))
            } finally { app.close() }
        } finally { db.close(); Files.deleteIfExists(file) }
    }
}
