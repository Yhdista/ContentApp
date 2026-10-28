package app.di

import androidx.room.Room
import app.database.configureAppDatabase
import core.common.AndroidAppClock
import core.common.AppClock
import app.database.AppDatabase
import core.feedback.AndroidPlatformFeedback
import core.feedback.PlatformFeedback
import core.platform.AndroidPlatformClipboard
import core.platform.PlatformClipboard
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.onClose
import org.koin.dsl.module

actual val platformModule: Module = module {
    single {
        val dbFile = androidContext().getDatabasePath("content.db")
        Room.databaseBuilder<AppDatabase>(
            context = androidContext(),
            name = dbFile.absolutePath
        )
            .configureAppDatabase()
            .build()
    } onClose { it?.close() }
    single { get<AppDatabase>().mutationDao() }
    single { get<AppDatabase>().workoutDao() }
    single { get<AppDatabase>().exerciseDao() }
    single<AppClock> { AndroidAppClock() }
    single<PlatformFeedback> { AndroidPlatformFeedback(androidContext()) }
    single<PlatformClipboard> { AndroidPlatformClipboard(androidContext()) }
}
