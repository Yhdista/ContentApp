package core.di

import core.common.ApplicationScope
import core.network.HttpClientFactory
import core.sync.SyncManager
import features.workout.active.ActiveWorkoutViewModel
import features.workout.data.OfflineFirstWorkoutRepository
import features.workout.data.datasource.RoomWorkoutLocalDataSource
import features.workout.data.datasource.WorkoutLocalDataSource
import features.workout.detail.WorkoutDetailViewModel
import features.workout.domain.WorkoutRepository
import features.workout.history.WorkoutHistoryViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val coreModule = module {
    single { HttpClientFactory.create() }
    single { ApplicationScope() }
}

val workoutDataModule = module {
    singleOf(::RoomWorkoutLocalDataSource) { bind<WorkoutLocalDataSource>() }
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
    }

fun initKoinIos() = initKoin {}
