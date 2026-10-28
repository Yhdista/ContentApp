package app.workflow

import core.result.Result
import features.routines.api.*
import features.trainingplans.api.TrainingPlanRepository
import features.workout.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map
import org.koin.dsl.module

fun editorIntegrationModule() = module {
    single<RoutineUsage> {
        val plans = get<TrainingPlanRepository>()
        RoutineUsage { routineId -> plans.observeAll().map { list -> list.count { plan -> plan.slots.any { it.routineId == routineId } } } }
    }
    single<CreateRoutineDraft> {
        val factory = get<RoutineDraftFactory>()
        CreateRoutineDraft { id -> try { Result.Success(factory.fromWorkout(id)) }
            catch(e: CancellationException) { throw e }
            catch(_: Exception) { Result.Error(WorkoutError.STORAGE) } }
    }
}
