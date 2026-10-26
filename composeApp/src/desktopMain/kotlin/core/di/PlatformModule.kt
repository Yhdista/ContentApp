package core.di

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import core.database.AppDatabase
import core.database.AppDatabaseConstructor
import core.feedback.DesktopPlatformFeedback
import core.feedback.PlatformFeedback
import core.platform.DesktopPlatformClipboard
import core.platform.PlatformClipboard
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
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
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
    single { get<AppDatabase>().contentDao() }
    single { get<AppDatabase>().mutationDao() }
    single { get<AppDatabase>().workoutDao() }
    single<PlatformFeedback> { DesktopPlatformFeedback() }
    single<PlatformClipboard> { DesktopPlatformClipboard() }
}
