package features.workout.data.datasource

import features.workout.data.dao.WorkoutDao
import features.workout.data.mapper.toDomain
import features.workout.data.mapper.toEntity
import features.exercises.api.Exercise
import features.exercises.api.ExerciseCatalog
import features.workout.domain.WorkoutSession
import features.workout.domain.WorkoutSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

internal class RoomWorkoutLocalDataSource(
    private val workoutDao: WorkoutDao,
    private val catalogue: ExerciseCatalog
) : WorkoutLocalDataSource {

    override fun observeExercises(): Flow<List<Exercise>> = catalogue.observeExercises()
    override suspend fun getAllExercises(): List<Exercise> = catalogue.getAllExercises()
    override suspend fun getExerciseById(id: String): Exercise? = catalogue.getExerciseById(id)
    override suspend fun getExercisesCount(): Int = catalogue.getExercisesCount()
    override suspend fun saveExercise(exercise: Exercise) = catalogue.saveExercise(exercise)
    override suspend fun saveExercises(exercises: List<Exercise>) = catalogue.saveExercises(exercises)

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

    override suspend fun deleteSessionWithSets(sessionId: String) {
        workoutDao.deleteSetsForSession(sessionId)
        workoutDao.deleteExercises(sessionId)
        workoutDao.deletePrescriptionForSession(sessionId)
        workoutDao.deleteSession(sessionId)
    }

    override fun observeSetsForSession(sessionId: String): Flow<List<WorkoutSet>> =
        combine(workoutDao.getSetsForSession(sessionId), workoutDao.observeExercises(sessionId)) { sets, entries ->
            decorate(sets.map { it.toDomain() }, entries.map { it.toDomain() })
        }

    override suspend fun getSetById(setId: String): WorkoutSet? =
        workoutDao.getSetById(setId)?.toDomain()

    override suspend fun getSetsForSessionSnapshot(sessionId: String): List<WorkoutSet> =
        decorate(workoutDao.getSetsForSessionSnapshot(sessionId).map { it.toDomain() }, getWorkoutExercises(sessionId))

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
        decorate(workoutDao.getAllCompletedSets().map { it.toDomain() },getAllWorkoutExercises())

    override suspend fun getAllSets() = decorate(workoutDao.getAllSetsSnapshot().map { it.toDomain() },getAllWorkoutExercises())
    override suspend fun getAllWorkoutExercises() = workoutDao.getAllExercisesSnapshot().map { it.toDomain() }

    override fun observePrescription(sessionId: String) = workoutDao.observePrescription(sessionId).map { rows -> rows.map { it.toDomain() } }
    override suspend fun getAllPrescriptions() = workoutDao.getAllPrescriptions().map { it.toDomain() }
    override fun observeWorkoutExercises(sessionId: String) = workoutDao.observeExercises(sessionId).map { list -> list.map { it.toDomain() } }
    override suspend fun getWorkoutExercises(sessionId: String) = workoutDao.getExercisesSnapshot(sessionId).map { it.toDomain() }
    override suspend fun saveWorkoutExercise(entry: features.workout.domain.WorkoutExercise) = workoutDao.insertExercise(entry.toEntity())
    override suspend fun deleteWorkoutExercise(id: String) = workoutDao.deleteExercise(id)

    private fun decorate(sets: List<WorkoutSet>, entries: List<features.workout.domain.WorkoutExercise>): List<WorkoutSet> {
        val byId=entries.associateBy { it.id }
        return sets.map { set -> byId[set.workoutExerciseId]?.let { e -> set.copy(
            exerciseNameSnapshot=e.name,muscleGroupSnapshot=e.muscleGroup,exerciseOrder=e.orderIndex,
            restSecondsSnapshot=e.restSeconds,orderReconstructed=e.orderReconstructed) } ?: set
        }.sortedWith(compareBy({ it.sessionId }, { it.exerciseOrder ?: Int.MAX_VALUE }, { it.setNumber }, { it.id }))
    }

}
