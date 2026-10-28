package features.workout.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import features.workout.data.entity.WorkoutSessionEntity
import features.workout.data.entity.WorkoutSetEntity
import features.workout.data.entity.WorkoutExerciseEntity
import features.workout.data.entity.WorkoutPrescriptionSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    // Aktivní sezení a historie
    @Query("SELECT * FROM workout_sessions WHERE isCompleted = 0 ORDER BY startTimeEpochMs DESC LIMIT 1")
    fun getActiveSession(): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_sessions WHERE isCompleted = 0 ORDER BY startTimeEpochMs DESC LIMIT 1")
    suspend fun getActiveSessionSync(): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE isCompleted = 1 ORDER BY startTimeEpochMs DESC")
    fun getCompletedSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions ORDER BY startTimeEpochMs DESC")
    suspend fun getAllSessionsList(): List<WorkoutSessionEntity>

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): WorkoutSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSessionEntity)

    @Query("UPDATE workout_sessions SET isCompleted = 1, endTimeEpochMs = :endTime, totalVolumeKg = :volume WHERE id = :sessionId")
    suspend fun completeSession(sessionId: String, endTime: Long, volume: Double)

    @Query("DELETE FROM workout_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: String)

    // Série
    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY exerciseId, setNumber ASC")
    fun getSetsForSession(sessionId: String): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY exerciseId, setNumber ASC")
    suspend fun getSetsForSessionSnapshot(sessionId: String): List<WorkoutSetEntity>

    @Query("SELECT * FROM workout_sets WHERE id = :setId LIMIT 1")
    suspend fun getSetById(setId: String): WorkoutSetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSet(set: WorkoutSetEntity)

    @Query("DELETE FROM workout_sets WHERE id = :setId")
    suspend fun deleteSet(setId: String)

    @Query("DELETE FROM workout_sets WHERE sessionId = :sessionId")
    suspend fun deleteSetsForSession(sessionId: String)

    @Query("""
        SELECT actual.* FROM workout_sets AS actual
        JOIN workout_sessions AS session ON session.id = actual.sessionId
        LEFT JOIN workout_exercises AS entry ON entry.id = actual.workoutExerciseId AND entry.sessionId = session.id
        WHERE actual.exerciseId = :exerciseId AND actual.isCompleted = 1 AND session.isCompleted = 1
        ORDER BY COALESCE(session.endTimeEpochMs, session.startTimeEpochMs) DESC,
            session.startTimeEpochMs DESC, session.id DESC,
            COALESCE(entry.orderIndex, -1) DESC, actual.setNumber DESC, actual.id DESC
        LIMIT 1
    """)
    suspend fun getLastPerformanceForExercise(exerciseId: String): WorkoutSetEntity?

    @Query("SELECT * FROM workout_sets WHERE exerciseId = :exerciseId AND isCompleted = 1 AND sessionId != :currentSessionId")
    suspend fun getHistoricalCompletedSets(exerciseId: String, currentSessionId: String): List<WorkoutSetEntity>

    @Query("SELECT * FROM workout_sets WHERE exerciseId = :exerciseId AND isCompleted = 1 ORDER BY id ASC")
    fun getAllCompletedSetsForExercise(exerciseId: String): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets WHERE isCompleted = 1 ORDER BY sessionId, exerciseId, setNumber ASC")
    suspend fun getAllCompletedSets(): List<WorkoutSetEntity>

    @Query("SELECT * FROM workout_sets ORDER BY sessionId, setNumber, id")
    suspend fun getAllSetsSnapshot(): List<WorkoutSetEntity>
    @Query("SELECT * FROM workout_exercises ORDER BY sessionId, orderIndex, id")
    suspend fun getAllExercisesSnapshot(): List<WorkoutExerciseEntity>
    @Query("SELECT * FROM workout_exercises WHERE sessionId = :sessionId ORDER BY orderIndex, id")
    suspend fun getExercisesSnapshot(sessionId: String): List<WorkoutExerciseEntity>
    @Query("SELECT * FROM workout_exercises WHERE sessionId = :sessionId ORDER BY orderIndex, id")
    fun observeExercises(sessionId: String): Flow<List<WorkoutExerciseEntity>>
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExercise(entry: WorkoutExerciseEntity)
    @Query("DELETE FROM workout_exercises WHERE sessionId = :sessionId")
    suspend fun deleteExercises(sessionId: String)
    @Query("DELETE FROM workout_exercises WHERE id = :id")
    suspend fun deleteExercise(id: String)
    // Immutable targets can only be inserted, or removed with a cancelled whole session.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPrescription(target: WorkoutPrescriptionSetEntity)
    @Query("SELECT * FROM workout_prescriptions WHERE sessionId = :sessionId ORDER BY exerciseOrder, setNumber, id")
    fun observePrescription(sessionId: String): Flow<List<WorkoutPrescriptionSetEntity>>
    @Query("SELECT * FROM workout_prescriptions ORDER BY sessionId, exerciseOrder, setNumber, id")
    suspend fun getAllPrescriptions(): List<WorkoutPrescriptionSetEntity>
    @Query("DELETE FROM workout_prescriptions WHERE sessionId = :sessionId")
    suspend fun deletePrescriptionForSession(sessionId: String)
}
