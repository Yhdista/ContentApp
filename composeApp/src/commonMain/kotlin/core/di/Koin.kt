package core.di

import core.database.AppDatabase
import core.network.HttpClientFactory
import core.repository.ContentRepository
import core.repository.ContentRepositoryImpl
import features.content.ContentListViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val commonModule = module {
    single { HttpClientFactory.create() }
    
    // Repository
    single<ContentRepository> { ContentRepositoryImpl(get(), get()) }
    
    // ViewModel
    factory { ContentListViewModel(get()) }
}

expect val platformModule: org.koin.core.module.Module

fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(commonModule, platformModule)
    }

// Helper for iOS
fun initKoinIos() = initKoin {}
