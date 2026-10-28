package features.workout

import features.workout.active.ActiveWorkoutViewModel
import features.workout.detail.WorkoutDetailViewModel
import features.workout.history.WorkoutHistoryViewModel
import features.workout.data.LocalWorkoutDataExport
import features.workout.data.OfflineFirstWorkoutRepository
import features.workout.data.datasource.RoomWorkoutLocalDataSource
import features.workout.data.datasource.WorkoutLocalDataSource
import features.workout.domain.WorkoutDataExport
import features.workout.domain.WorkoutRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

fun workoutFeatureModule(): Module = module {
    single<WorkoutLocalDataSource> { RoomWorkoutLocalDataSource(get(), get()) }
    single<WorkoutDataExport> { LocalWorkoutDataExport(get(), get()) }
    single<WorkoutRepository> { OfflineFirstWorkoutRepository(get(), get(), get(), get()) }
    single<features.workout.domain.WorkoutStore> { features.workout.data.RoomWorkoutStore(get(), get(), get(), get()) }
    factoryOf(::ActiveWorkoutViewModel)
    factoryOf(::WorkoutHistoryViewModel)
    factoryOf(::WorkoutDetailViewModel)
}
