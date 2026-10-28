package features.workout.data.datasource

import features.workout.data.dao.WorkoutDao
import features.workout.data.mapper.toDomain
import features.workout.data.mapper.toEntity
import features.workout.domain.Exercise
import features.workout.domain.WorkoutSession
import features.workout.domain.WorkoutSet
import features.workout.domain.WorkoutTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RoomWorkoutLocalDataSource(
    private val workoutDao: WorkoutDao
) : WorkoutLocalDataSource {

    override fun observeExercises(): Flow<List<Exercise>> =
        workoutDao.getAllExercises().map { list -> list.map { it.toDomain() } }

    override suspend fun getAllExercises(): List<Exercise> =
        workoutDao.getAllExercisesList().map { it.toDomain() }

    override suspend fun getExerciseById(id: String): Exercise? =
        workoutDao.getExerciseById(id)?.toDomain()

    override suspend fun getExercisesCount(): Int =
        workoutDao.getExercisesCount()

    override suspend fun saveExercise(exercise: Exercise) =
        workoutDao.insertExercise(exercise.toEntity())

    override suspend fun saveExercises(exercises: List<Exercise>) =
        workoutDao.insertExercises(exercises.map { it.toEntity() })

    override fun observeActiveSession(): Flow<WorkoutSession?> =
        workoutDao.getActiveSession().map { it?.toDomain() }

    override suspend fun getActiveSession(): WorkoutSession? =
        workoutDao.getActiveSessionSync()?.toDomain()

    override fun observeCompletedSessions(): Flow<List<WorkoutSession>> =
        workoutDao.getCompletedSessions().map { list -> list.map { it.toDomain() } }

    override suspend fun getAllSessions(): List<WorkoutSession> =
        workoutDao.getAllSessionsList().map { it.toDomain() }

    override suspend fun getSessionById(sessionId: String): WorkoutSession? =
        workoutDao.getSessionById(sessionId)?.toDomain()

    override suspend fun saveSession(session: WorkoutSession) =
        workoutDao.insertSession(session.toEntity())

    override suspend fun completeSession(sessionId: String, endTimeEpochMs: Long, totalVolumeKg: Double) =
        workoutDao.completeSession(sessionId, endTimeEpochMs, totalVolumeKg)

    override suspend fun deleteSessionWithSets(sessionId: String) {
        workoutDao.deleteSetsForSession(sessionId)
        workoutDao.deleteSession(sessionId)
    }

    override fun observeSetsForSession(sessionId: String): Flow<List<WorkoutSet>> =
        workoutDao.getSetsForSession(sessionId).map { list -> list.map { it.toDomain() } }

    override suspend fun getSetById(setId: String): WorkoutSet? =
        workoutDao.getSetById(setId)?.toDomain()

    override suspend fun getSetsForSessionSnapshot(sessionId: String): List<WorkoutSet> =
        workoutDao.getSetsForSessionSnapshot(sessionId).map { it.toDomain() }

    override suspend fun saveSet(set: WorkoutSet) =
        workoutDao.insertOrUpdateSet(set.toEntity())

    override suspend fun deleteSet(setId: String) =
        workoutDao.deleteSet(setId)

    override suspend fun getLastCompletedSetForExercise(exerciseId: String): WorkoutSet? =
        workoutDao.getLastPerformanceForExercise(exerciseId)?.toDomain()

    override suspend fun getHistoricalCompletedSets(exerciseId: String, currentSessionId: String): List<WorkoutSet> =
        workoutDao.getHistoricalCompletedSets(exerciseId, currentSessionId).map { it.toDomain() }

    override fun observeCompletedSetsForExercise(exerciseId: String): Flow<List<WorkoutSet>> =
        workoutDao.getAllCompletedSetsForExercise(exerciseId).map { list -> list.map { it.toDomain() } }

    override suspend fun getAllCompletedSets(): List<WorkoutSet> =
        workoutDao.getAllCompletedSets().map { it.toDomain() }

    override fun observeTemplates(): Flow<List<WorkoutTemplate>> =
        combine(
            workoutDao.getAllTemplates(),
            workoutDao.getAllTemplateExercises()
        ) { templates, allExercises ->
            val exercisesByTemplate = allExercises.groupBy { it.templateId }
            templates.map { tmpl ->
                val tmplExercises = exercisesByTemplate[tmpl.id]
                    .orEmpty()
                    .sortedBy { it.orderIndex }
                    .map { it.toDomain() }
                tmpl.toDomain(tmplExercises)
            }
        }

    override suspend fun getTemplatesSnapshot(): List<WorkoutTemplate> =
        workoutDao.getTemplatesSnapshot().map { template ->
            template.toDomain(workoutDao.getTemplateExercises(template.id).map { it.toDomain() })
        }

    override suspend fun getTemplateById(templateId: String): WorkoutTemplate? {
        val entity = workoutDao.getTemplateById(templateId) ?: return null
        val exercises = workoutDao.getTemplateExercises(templateId).map { it.toDomain() }
        return entity.toDomain(exercises)
    }

    override suspend fun getTemplatesCount(): Int =
        workoutDao.getTemplatesCount()

    override suspend fun saveTemplate(template: WorkoutTemplate) {
        workoutDao.insertTemplate(template.toEntity())
        workoutDao.insertTemplateExercises(template.exercises.map { it.toEntity(template.id) })
    }

    override suspend fun saveTemplates(templates: List<WorkoutTemplate>) {
        for (tmpl in templates) {
            saveTemplate(tmpl)
        }
    }
}
