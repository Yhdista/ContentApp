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
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val coreModule = module {
    single { HttpClientFactory.create() }
    single { ApplicationScope() }
}

val workoutDataModule = module {
    single<WorkoutLocalDataSource> { RoomWorkoutLocalDataSource(get()) }
    single<WorkoutRepository> { OfflineFirstWorkoutRepository(get(), get(), get()) }
    single { SyncManager(get(), get()) }
}

val workoutPresentationModule = module {
    factory { ActiveWorkoutViewModel(get(), get(), get(), get()) }
    factory { WorkoutHistoryViewModel(get(), get(), get()) }
    factory { WorkoutDetailViewModel(get()) }
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
