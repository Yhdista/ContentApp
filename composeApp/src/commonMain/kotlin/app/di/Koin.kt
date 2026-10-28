package app.di

import app.database.AppDatabase
import core.common.ApplicationScope
import core.storage.draftStorageModule
import features.routines.data.routineFeatureModule
import features.trainingplans.data.trainingPlanFeatureModule
import core.storage.CommandCoordinator
import core.storage.RoomCommandCoordinator
import core.sync.SyncManager
import features.exercises.data.exerciseFeatureModule
import features.workout.workoutFeatureModule
import kotlinx.coroutines.cancel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.onClose
import org.koin.dsl.module

val coreModule = module {
    single { ApplicationScope() } onClose { it?.cancel() }
    single<CommandCoordinator> { RoomCommandCoordinator(get<AppDatabase>(), get()) }
    single { SyncManager(get(), get()) }
    single { get<AppDatabase>().routineDao() }
    single { get<AppDatabase>().trainingPlanDao() }
    single { get<AppDatabase>().editorDraftDao() }
}

val commonModule = module {
    includes(app.workflow.trainingWorkflowModule(), coreModule, draftStorageModule(), exerciseFeatureModule(), workoutFeatureModule(), routineFeatureModule(), trainingPlanFeatureModule())
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
