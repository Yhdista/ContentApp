package core.di

import androidx.room.Room
import core.database.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single {
        val dbFile = androidContext().getDatabasePath("content.db")
        Room.databaseBuilder<AppDatabase>(
            context = androidContext(),
            name = dbFile.absolutePath
        )
            .fallbackToDestructiveMigration(true)
            .build()
    }
    single { get<AppDatabase>().contentDao() }
    single { get<AppDatabase>().mutationDao() }
    single { get<AppDatabase>().workoutDao() }
}
