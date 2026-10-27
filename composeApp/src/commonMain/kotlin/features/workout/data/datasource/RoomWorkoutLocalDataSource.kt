package features.workout.data.datasource

import features.workout.data.dao.WorkoutDao
import features.workout.data.entity.ExerciseEntity
import features.workout.data.entity.WorkoutSessionEntity
import features.workout.data.entity.WorkoutSetEntity
import features.workout.data.entity.WorkoutTemplateEntity
import features.workout.data.entity.WorkoutTemplateExerciseEntity
import kotlinx.coroutines.flow.Flow

class RoomWorkoutLocalDataSource(
    private val workoutDao: WorkoutDao
) : WorkoutLocalDataSource {

    override fun getAllExercises(): Flow<List<ExerciseEntity>> = workoutDao.getAllExercises()

    override suspend fun getExerciseById(id: String): ExerciseEntity? = workoutDao.getExerciseById(id)

    override suspend fun getExercisesCount(): Int = workoutDao.getExercisesCount()

    override suspend fun insertExercise(exercise: ExerciseEntity) = workoutDao.insertExercise(exercise)

    override suspend fun insertExercises(exercises: List<ExerciseEntity>) = workoutDao.insertExercises(exercises)

    override suspend fun getAllExercisesList(): List<ExerciseEntity> = workoutDao.getAllExercisesList()

    override fun getActiveSession(): Flow<WorkoutSessionEntity?> = workoutDao.getActiveSession()

    override suspend fun getActiveSessionSync(): WorkoutSessionEntity? = workoutDao.getActiveSessionSync()

    override fun getCompletedSessions(): Flow<List<WorkoutSessionEntity>> = workoutDao.getCompletedSessions()

    override suspend fun getSessionById(sessionId: String): WorkoutSessionEntity? = workoutDao.getSessionById(sessionId)

    override suspend fun insertSession(session: WorkoutSessionEntity) = workoutDao.insertSession(session)

    override suspend fun completeSession(sessionId: String, endTime: Long, volume: Double) =
        workoutDao.completeSession(sessionId, endTime, volume)

    override suspend fun deleteSession(sessionId: String) = workoutDao.deleteSession(sessionId)

    override suspend fun getAllSessionsList(): List<WorkoutSessionEntity> = workoutDao.getAllSessionsList()

    override fun getSetsForSession(sessionId: String): Flow<List<WorkoutSetEntity>> = workoutDao.getSetsForSession(sessionId)

    override suspend fun insertOrUpdateSet(set: WorkoutSetEntity) = workoutDao.insertOrUpdateSet(set)

    override suspend fun deleteSet(setId: String) = workoutDao.deleteSet(setId)

    override suspend fun deleteSetsForSession(sessionId: String) = workoutDao.deleteSetsForSession(sessionId)

    override suspend fun getLastPerformanceForExercise(exerciseId: String): WorkoutSetEntity? =
        workoutDao.getLastPerformanceForExercise(exerciseId)

    override suspend fun getHistoricalCompletedSets(exerciseId: String, currentSessionId: String): List<WorkoutSetEntity> =
        workoutDao.getHistoricalCompletedSets(exerciseId, currentSessionId)

    override fun getAllCompletedSetsForExercise(exerciseId: String): Flow<List<WorkoutSetEntity>> =
        workoutDao.getAllCompletedSetsForExercise(exerciseId)

    override suspend fun getAllCompletedSets(): List<WorkoutSetEntity> = workoutDao.getAllCompletedSets()

    override fun getAllTemplates(): Flow<List<WorkoutTemplateEntity>> = workoutDao.getAllTemplates()

    override suspend fun getTemplateById(templateId: String): WorkoutTemplateEntity? =
        workoutDao.getTemplateById(templateId)

    override suspend fun getTemplateExercises(templateId: String): List<WorkoutTemplateExerciseEntity> =
        workoutDao.getTemplateExercises(templateId)

    override suspend fun insertTemplate(template: WorkoutTemplateEntity) = workoutDao.insertTemplate(template)

    override suspend fun insertTemplateExercises(exercises: List<WorkoutTemplateExerciseEntity>) =
        workoutDao.insertTemplateExercises(exercises)

    override suspend fun getTemplatesCount(): Int = workoutDao.getTemplatesCount()
}
