package app.di

import androidx.room.Room
import app.database.configureAppDatabase
import core.common.AppClock
import core.common.IosAppClock
import app.database.AppDatabase
import app.database.AppDatabaseConstructor
import core.feedback.IosPlatformFeedback
import core.feedback.PlatformFeedback
import core.platform.IosPlatformClipboard
import core.platform.PlatformClipboard
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.onClose
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual val platformModule: Module = module {
    single {
        val dbFile = documentDirectory() + "/content.db"
        Room.databaseBuilder<AppDatabase>(
            name = dbFile,
            factory = { AppDatabaseConstructor.initialize() }
        )
            .configureAppDatabase()
            .build()
    } onClose { it?.close() }
    single { get<AppDatabase>().mutationDao() }
    single { get<AppDatabase>().workoutDao() }
    single { get<AppDatabase>().exerciseDao() }
    single<AppClock> { IosAppClock() }
    single<PlatformFeedback> { IosPlatformFeedback() }
    single<PlatformClipboard> { IosPlatformClipboard() }
}

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(documentDirectory?.path)
}
