package core.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import features.workout.data.dao.WorkoutDao
import features.workout.data.entity.ExerciseEntity
import features.workout.data.entity.WorkoutSessionEntity
import features.workout.data.entity.WorkoutSetEntity
import features.workout.data.entity.WorkoutTemplateEntity
import features.workout.data.entity.WorkoutTemplateExerciseEntity

@Database(
    entities = [
        SyncMutationEntity::class,
        ExerciseEntity::class,
        WorkoutSessionEntity::class,
        WorkoutSetEntity::class,
        WorkoutTemplateEntity::class,
        WorkoutTemplateExerciseEntity::class
    ],
    version = 6
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mutationDao(): MutationDao
    abstract fun workoutDao(): WorkoutDao
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
