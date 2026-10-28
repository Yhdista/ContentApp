package app.workflow

import core.result.Result
import core.storage.CommandCoordinator
import features.exercises.api.ExerciseCatalog
import features.routines.api.RoutineRepository
import features.routines.api.RoutineValidation
import features.trainingplans.api.TrainingPlanRepository
import features.workout.domain.*
import kotlinx.coroutines.CancellationException
import org.koin.dsl.module

/** 🛡️ Child typed errors abort the outer transaction, including previously committed savepoints. */
class TrainingWorkflow(
    private val writes: CommandCoordinator, private val routines: RoutineRepository,
    private val plans: TrainingPlanRepository, private val catalogue: ExerciseCatalog,
    private val workouts: WorkoutStore
) {
    suspend fun startRoutine(id: String): Result<StartOutcome, WorkoutError> = command {
        workouts.activeSession()?.let { return@command StartOutcome.ResumedExisting(it.id) }
        workouts.createFromPrescription(prescription(id,null)).value()
    }
    suspend fun startPlan(id: String): Result<StartOutcome, WorkoutError> = command {
        workouts.activeSession()?.let { return@command StartOutcome.ResumedExisting(it.id) }
        val context=plans.prepareStart(id).value(::mapPlanError)
        workouts.createFromPrescription(prescription(context.slot.routineId,context.source)).value()
    }
    suspend fun complete(id: String): Result<CompletionOutcome, WorkoutError> = command {
        val outcome=workouts.completeOnce(id).value()
        if(outcome.firstCompletion) outcome.session.sourcePlan?.let { plans.recordCompleted(it).value(::mapPlanError) }
        outcome
    }
    private suspend fun prescription(id: String, source: PlanWorkoutSource?): WorkoutStartPrescription {
        val routine=routines.getById(id) ?: abort(WorkoutError.NOT_FOUND)
        if(routine.archived) abort(WorkoutError.ARCHIVED)
        if(RoutineValidation.validate(routine)!=null) abort(WorkoutError.VALIDATION)
        val catalogueById=routine.exercises.map { it.exerciseId }.distinct().associateWith { exerciseId ->
            val exercise=catalogue.getExerciseById(exerciseId) ?: abort(WorkoutError.NOT_FOUND)
            if(exercise.archived) abort(WorkoutError.ARCHIVED)
            exercise
        }
        return WorkoutStartPrescription(routine.name,routine.exercises.map { e ->
            val exercise=catalogueById.getValue(e.exerciseId)
            WorkoutExercisePrescription(e.id,e.exerciseId,exercise.name,exercise.muscleGroup,e.restSeconds,
                e.sets.map { WorkoutSetPrescription(it.weightKg,it.reps,it.setType,it.id) })
        },routine.id,routine.revision,source)
    }
    private fun <T> Result<T, WorkoutError>.value(): T = value { it }
    private fun <T, E : core.result.RootError> Result<T, E>.value(mapError: (E) -> WorkoutError): T = when (this) {
        is Result.Success -> data
        is Result.Error -> abort(mapError(error))
    }
    private fun mapPlanError(error: features.trainingplans.api.PlanError): WorkoutError = when (error) {
        features.trainingplans.api.PlanError.VALIDATION -> WorkoutError.VALIDATION
        features.trainingplans.api.PlanError.NOT_FOUND -> WorkoutError.NOT_FOUND
        features.trainingplans.api.PlanError.ARCHIVED -> WorkoutError.ARCHIVED
        features.trainingplans.api.PlanError.REVISION_CONFLICT -> WorkoutError.REVISION_CONFLICT
        features.trainingplans.api.PlanError.REVIEW_REQUIRED -> WorkoutError.REVIEW_REQUIRED
        features.trainingplans.api.PlanError.STORAGE -> WorkoutError.STORAGE
    }
    private class Abort(val error: WorkoutError): RuntimeException()
    private fun abort(error: WorkoutError): Nothing = throw Abort(error)
    private suspend fun <T> command(block: suspend () -> T): Result<T, WorkoutError> = try {
        Result.Success(writes.write { block() })
    } catch(e: CancellationException) { throw e
    } catch(e: Abort) { Result.Error(e.error)
    } catch(_: Exception) { Result.Error(WorkoutError.STORAGE) }
}

fun trainingWorkflowModule() = module {
    single { TrainingWorkflow(get(),get(),get(),get(),get()) }
    single<StartRoutineWorkout> { StartRoutineWorkout(get<TrainingWorkflow>()::startRoutine) }
    single<StartPlanWorkout> { StartPlanWorkout(get<TrainingWorkflow>()::startPlan) }
    single<CompleteWorkout> { CompleteWorkout(get<TrainingWorkflow>()::complete) }
}
