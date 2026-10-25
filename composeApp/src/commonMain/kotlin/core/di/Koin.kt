package core.di

import core.common.AppClock
import core.common.ApplicationScope
import core.common.SystemAppClock
import core.network.HttpClientFactory
import core.repository.ContentRepository
import core.repository.ContentRepositoryImpl
import core.sync.SyncManager
import features.content.ContentListViewModel
import features.universal.UniversalViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val commonModule = module {
    single { HttpClientFactory.create() }
    single<AppClock> { SystemAppClock() }
    single { ApplicationScope() }

    // Repozitář & Sync
    single<ContentRepository> { ContentRepositoryImpl(get(), get(), get(), get()) }
    single { SyncManager(get(), get(), get()) }

    // ViewModely
    factory { ContentListViewModel(get()) }
    factory { UniversalViewModel(get(), get(), get(), get()) }
}

expect val platformModule: Module

fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(commonModule, platformModule)
    }

fun initKoinIos() = initKoin {}
