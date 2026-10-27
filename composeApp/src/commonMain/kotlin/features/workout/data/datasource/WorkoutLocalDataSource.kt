package features.workout.data.datasource

import features.workout.data.entity.ExerciseEntity
import features.workout.data.entity.WorkoutSessionEntity
import features.workout.data.entity.WorkoutSetEntity
import features.workout.data.entity.WorkoutTemplateEntity
import features.workout.data.entity.WorkoutTemplateExerciseEntity
import kotlinx.coroutines.flow.Flow

interface WorkoutLocalDataSource {
    // Exercises
    fun getAllExercises(): Flow<List<ExerciseEntity>>
    suspend fun getExerciseById(id: String): ExerciseEntity?
    suspend fun getExercisesCount(): Int
    suspend fun insertExercise(exercise: ExerciseEntity)
    suspend fun insertExercises(exercises: List<ExerciseEntity>)
    suspend fun getAllExercisesList(): List<ExerciseEntity>

    // Sessions
    fun getActiveSession(): Flow<WorkoutSessionEntity?>
    suspend fun getActiveSessionSync(): WorkoutSessionEntity?
    fun getCompletedSessions(): Flow<List<WorkoutSessionEntity>>
    suspend fun getSessionById(sessionId: String): WorkoutSessionEntity?
    suspend fun insertSession(session: WorkoutSessionEntity)
    suspend fun completeSession(sessionId: String, endTime: Long, volume: Double)
    suspend fun deleteSession(sessionId: String)
    suspend fun getAllSessionsList(): List<WorkoutSessionEntity>

    // Sets
    fun getSetsForSession(sessionId: String): Flow<List<WorkoutSetEntity>>
    suspend fun insertOrUpdateSet(set: WorkoutSetEntity)
    suspend fun deleteSet(setId: String)
    suspend fun deleteSetsForSession(sessionId: String)
    suspend fun getLastPerformanceForExercise(exerciseId: String): WorkoutSetEntity?
    suspend fun getHistoricalCompletedSets(exerciseId: String, currentSessionId: String): List<WorkoutSetEntity>
    fun getAllCompletedSetsForExercise(exerciseId: String): Flow<List<WorkoutSetEntity>>
    suspend fun getAllCompletedSets(): List<WorkoutSetEntity>

    // Templates
    fun getAllTemplates(): Flow<List<WorkoutTemplateEntity>>
    suspend fun getTemplateById(templateId: String): WorkoutTemplateEntity?
    suspend fun getTemplateExercises(templateId: String): List<WorkoutTemplateExerciseEntity>
    suspend fun insertTemplate(template: WorkoutTemplateEntity)
    suspend fun insertTemplateExercises(exercises: List<WorkoutTemplateExerciseEntity>)
    suspend fun getTemplatesCount(): Int
}
