package app.database

import core.storage.EditorDraftDao
import core.storage.EditorDraftEntity
import features.routines.data.RoutineDao
import features.routines.data.RoutineEntity
import features.routines.data.RoutineExerciseEntity
import features.routines.data.RoutineSetEntity
import core.database.MutationDao
import core.database.SyncMutationEntity
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import features.workout.data.dao.WorkoutDao
import features.exercises.data.ExerciseEntity
import features.exercises.data.ExerciseDao
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
        WorkoutTemplateExerciseEntity::class,
        RoutineEntity::class,
        RoutineExerciseEntity::class,
        RoutineSetEntity::class,
        EditorDraftEntity::class
    ],
    version = 7,
    exportSchema = true
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mutationDao(): MutationDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun routineDao(): RoutineDao
    abstract fun editorDraftDao(): EditorDraftDao
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
