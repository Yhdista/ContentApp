package core.di

import core.common.ApplicationScope
import core.network.HttpClientFactory
import core.sync.SyncManager
import features.workout.active.ActiveWorkoutViewModel
import features.workout.data.WorkoutRepositoryImpl
import features.workout.detail.WorkoutDetailViewModel
import features.workout.domain.WorkoutRepository
import features.workout.history.WorkoutHistoryViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val commonModule = module {
    single { HttpClientFactory.create() }
    single { ApplicationScope() }

    // Repozitář & Sync (Outbox)
    single<WorkoutRepository> { WorkoutRepositoryImpl(get(), get(), get()) }
    single { SyncManager(get(), get()) }

    // ViewModely
    factory { ActiveWorkoutViewModel(get(), get(), get(), get()) }
    factory { WorkoutHistoryViewModel(get(), get(), get()) }
    factory { WorkoutDetailViewModel(get()) }
}

expect val platformModule: Module

fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(commonModule, platformModule)
    }

fun initKoinIos() = initKoin {}
