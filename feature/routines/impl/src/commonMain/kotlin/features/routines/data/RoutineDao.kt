package features.routines.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Transaction
    @Query("SELECT * FROM routines WHERE :includeArchived OR archived = 0 ORDER BY name, id")
    fun observeAll(includeArchived: Boolean): Flow<List<RoutineSnapshot>>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :id")
    fun observeById(id: String): Flow<RoutineSnapshot?>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun getById(id: String): RoutineSnapshot?

    @Insert
    suspend fun insertRoutine(routine: RoutineEntity)

    @Query("UPDATE routines SET name = :name, notes = :notes, revision = revision + 1 WHERE id = :id AND revision = :expectedRevision AND archived = 0")
    suspend fun updateRoutine(id: String, name: String, notes: String, expectedRevision: Long): Int

    @Query("UPDATE routines SET archived = 1, revision = revision + 1 WHERE id = :id AND revision = :expectedRevision AND archived = 0")
    suspend fun archive(id: String, expectedRevision: Long): Int

    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun deleteExercises(routineId: String)

    // ABORT protects child IDs owned by another aggregate; REPLACE could silently steal them.
    @Insert
    suspend fun insertExercises(exercises: List<RoutineExerciseEntity>)

    @Insert
    suspend fun insertSets(sets: List<RoutineSetEntity>)
}
