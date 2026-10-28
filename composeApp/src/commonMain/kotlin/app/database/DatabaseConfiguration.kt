package app.database

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/** All hosts use the same driver and transaction API. Missing migrations fail without deleting data. */
fun RoomDatabase.Builder<AppDatabase>.configureAppDatabase(): RoomDatabase.Builder<AppDatabase> =
    addMigrations(MIGRATION_6_7).setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO)
