package app.di

import app.database.RoomWorkoutWriteCoordinator
import features.workout.data.WorkoutWriteCoordinator
import core.common.ApplicationScope
import core.sync.SyncManager
import features.workout.active.ActiveWorkoutViewModel
import features.workout.data.LocalWorkoutDataExport
import features.workout.domain.WorkoutDataExport
import features.workout.data.OfflineFirstWorkoutRepository
import features.workout.data.datasource.RoomWorkoutLocalDataSource
import features.workout.data.datasource.WorkoutLocalDataSource
import features.workout.detail.WorkoutDetailViewModel
import features.workout.domain.WorkoutRepository
import features.workout.history.WorkoutHistoryViewModel
import kotlinx.coroutines.cancel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.onClose
import org.koin.dsl.module

val coreModule = module {
    single { ApplicationScope() } onClose { it?.cancel() }
}

val workoutDataModule = module {
    single<WorkoutWriteCoordinator> { RoomWorkoutWriteCoordinator(get(), get()) }
    singleOf(::RoomWorkoutLocalDataSource) { bind<WorkoutLocalDataSource>() }
    singleOf(::LocalWorkoutDataExport) { bind<WorkoutDataExport>() }
    singleOf(::OfflineFirstWorkoutRepository) { bind<WorkoutRepository>() }
    single { SyncManager(get(), get()) }
}

val workoutPresentationModule = module {
    factoryOf(::ActiveWorkoutViewModel)
    factoryOf(::WorkoutHistoryViewModel)
    factoryOf(::WorkoutDetailViewModel)
}

val commonModule = module {
    includes(coreModule, workoutDataModule, workoutPresentationModule)
}

expect val platformModule: Module

fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(commonModule, platformModule)
    }.also { application ->
        application.koin.get<SyncManager>().startSyncLoop()
    }

fun initKoinIos() = initKoin {}
