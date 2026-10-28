import androidx.room.Room
import app.database.*
import app.workflow.TrainingWorkflow
import core.common.*
import core.storage.*
import core.database.*
import core.result.Result
import features.exercises.api.*
import features.exercises.data.exerciseFeatureModule
import features.routines.api.*
import features.routines.data.routineFeatureModule
import features.trainingplans.api.*
import features.trainingplans.data.trainingPlanFeatureModule
import features.workout.domain.*
import features.workout.workoutFeatureModule
import features.workout.data.mapper.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.koin.dsl.*
import features.workout.active.ActiveWorkoutViewModel
import features.workout.active.ActiveWorkoutContract
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.test.*
import kotlinx.coroutines.flow.*

fun <T> ok(result: Result<T, *>): T = (result as Result.Success<T>).data
fun main() = runBlocking {
    val db = Room.inMemoryDatabaseBuilder<AppDatabase> { AppDatabaseConstructor.initialize() }.configureAppDatabase().build()
    val scope = ApplicationScope()
    var failSet = false
    val app = koinApplication { modules(module {
        single { scope }
        single { db.workoutDao() }; single { db.exerciseDao() }; single { db.routineDao() }; single { db.trainingPlanDao() }
        single<MutationDao> { object: MutationDao by db.mutationDao() {
            override suspend fun insertMutation(mutation: SyncMutationEntity) {
                if (failSet && mutation.entityName == "workout_set") error("Simulated set outbox failure")
                db.mutationDao().insertMutation(mutation)
            }
        } }
        single<CommandCoordinator> { RoomCommandCoordinator(db, scope) }
        single<AppClock> { SystemAppClock() }
    }, exerciseFeatureModule(), routineFeatureModule(), trainingPlanFeatureModule(), workoutFeatureModule()) }
    try {
        val k = app.koin
        val catalogue = k.get<ExerciseCatalog>()
        val routines = k.get<RoutineRepository>()
        val plans = k.get<TrainingPlanRepository>()
        val workouts = k.get<WorkoutRepository>()
        val workflow = TrainingWorkflow(k.get(), routines, plans, catalogue, k.get())
        catalogue.saveExercise(Exercise("bench", "Bench", MuscleGroup.CHEST))
        ok(routines.save(Routine("routine", "Routine", exercises=listOf(RoutineExercise("entry", "bench", 90, listOf(RoutineSet("set", 20.0, 8, WorkoutSetType.NORMAL))))), null))
        ok(plans.save(TrainingPlan("plan", "Plan", slots=listOf(PlanSlot("A", "A", "routine"), PlanSlot("B", "B", "routine"))), null))
        val id = ok(workflow.startPlan("plan")).sessionId
        val before = plans.getProgress("plan")!!
        check(before.nextSlotId == "A")
        check(workouts.completeWorkout(id)!!.isCompleted)
        val retry = ok(workflow.complete(id))
        val after = plans.getProgress("plan")!!
        check(!retry.firstCompletion)
        check(after == before)
        println("PROVEN completion bypass: completed=true, workflow retry firstCompletion=false, nextSlot=A instead of B, revision unchanged")

        val free = workouts.startWorkout("Atomic-add probe")
        failSet = true
        val entry = workouts.addExerciseEntry(free.id, "bench")
        check(runCatching { workouts.addSetToEntry(free.id, entry.id, 60.0, 10) }.isFailure)
        check(workouts.getWorkoutExercises(free.id).first().size == 1)
        check(workouts.getSetsForSession(free.id).first().isEmpty())
        println("PROVEN split add command: second outbox fails, persisted entries=1 sets=0; first transaction cannot roll back")
        lockingProbe(workouts, free)
        db.workoutDao().insertSession(WorkoutSession("chron-old", "Old", 100L, 200L, true).toEntity())
        db.workoutDao().insertSession(WorkoutSession("chron-new", "New", 300L, 400L, true).toEntity())
        db.workoutDao().insertOrUpdateSet(WorkoutSet("zz-old", "chron-old", "bench", 1, 10.0, 8, isCompleted=true).toEntity())
        db.workoutDao().insertOrUpdateSet(WorkoutSet("aa-new", "chron-new", "bench", 1, 20.0, 8, isCompleted=true).toEntity())
        val last = db.workoutDao().getLastPerformanceForExercise("bench")!!
        check(last.sessionId == "chron-old")
        println("PROVEN latest-performance chronology: old session end=200 chosen over newer end=400 because zz-old sorts after aa-new")
    } finally { scope.cancel(); app.close(); db.close() }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun lockingProbe(real: WorkoutRepository, session: WorkoutSession) = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    val failObservation = CompletableDeferred<Unit>()
    val finishGate = CompletableDeferred<Unit>()
    var finishCalls = 0
    val repo = object: WorkoutRepository by real {
        override suspend fun ensureDefaultExercises() {}
        override fun getExercises(): Flow<List<Exercise>> = flow {
            emit(listOf(Exercise("bench", "Bench", MuscleGroup.CHEST)))
            failObservation.await()
            error("Delayed catalogue failure")
        }
        override fun getActiveSession() = flowOf(session)
        override fun getSetsForSession(sessionId: String) = flowOf(emptyList<WorkoutSet>())
        override fun getWorkoutExercises(sessionId: String) = flowOf(emptyList<WorkoutExercise>())
    }
    val vm = ActiveWorkoutViewModel(repo, SystemAppClock(), completeWorkout = CompleteWorkout {
        finishCalls++
        finishGate.await()
        Result.Success(CompletionOutcome(session.copy(isCompleted = true), true))
    })
    try {
        testScheduler.runCurrent()
        check(vm.uiState.value.session != null)
        vm.onIntent(ActiveWorkoutContract.Intent.FinishWorkout)
        testScheduler.runCurrent()
        check(vm.uiState.value.isSaving && finishCalls == 1)
        failObservation.complete(Unit)
        testScheduler.runCurrent()
        check(!vm.uiState.value.isSaving && !finishGate.isCompleted)
        vm.onIntent(ActiveWorkoutContract.Intent.FinishWorkout)
        testScheduler.runCurrent()
        check(finishCalls == 2)
        println("PROVEN terminal lock leak: pending finish unlocked by catalogue Flow error; second Finish accepted, completion calls=2")
    } finally {
        vm.viewModelScope.cancel()
        testScheduler.runCurrent()
        Dispatchers.resetMain()
    }
}
