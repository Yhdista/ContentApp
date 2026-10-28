package app.di

import androidx.room.Room
import app.database.configureAppDatabase
import core.common.AppClock
import core.common.DesktopAppClock
import app.database.AppDatabase
import app.database.AppDatabaseConstructor
import core.feedback.DesktopPlatformFeedback
import core.feedback.PlatformFeedback
import core.platform.DesktopPlatformClipboard
import core.platform.PlatformClipboard
import org.koin.core.module.Module
import org.koin.dsl.onClose
import org.koin.dsl.module
import java.io.File

actual val platformModule: Module = module {
    single {
        val userHome = System.getProperty("user.home") ?: "."
        val dbDir = File(userHome, ".contentapp")
        if (!dbDir.exists()) {
            dbDir.mkdirs()
        }
        val dbFile = File(dbDir, "content.db")
        Room.databaseBuilder<AppDatabase>(
            name = dbFile.absolutePath,
            factory = { AppDatabaseConstructor.initialize() }
        )
            .configureAppDatabase()
            .build()
    } onClose { it?.close() }
    single { get<AppDatabase>().mutationDao() }
    single { get<AppDatabase>().workoutDao() }
    single { get<AppDatabase>().exerciseDao() }
    single<AppClock> { DesktopAppClock() }
    single<PlatformFeedback> { DesktopPlatformFeedback() }
    single<PlatformClipboard> { DesktopPlatformClipboard() }
}
