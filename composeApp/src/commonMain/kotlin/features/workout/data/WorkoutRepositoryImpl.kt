package features.workout.data

import core.common.AppClock
import core.database.MutationDao
import core.database.SyncMutationEntity
import features.workout.data.dao.WorkoutDao
import features.workout.data.entity.ExerciseEntity
import features.workout.data.entity.WorkoutSessionEntity
import features.workout.data.entity.WorkoutSetEntity
import features.workout.domain.Exercise
import features.workout.domain.MuscleGroup
import features.workout.domain.WorkoutRepository
import features.workout.domain.WorkoutSession
import features.workout.domain.WorkoutSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class WorkoutRepositoryImpl(
    private val workoutDao: WorkoutDao,
    private val mutationDao: MutationDao,
    private val clock: AppClock
) : WorkoutRepository {

    override fun getExercises(): Flow<List<Exercise>> {
        return workoutDao.getAllExercises().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun ensureDefaultExercises() {
        if (workoutDao.getExercisesCount() == 0) {
            val defaults = listOf(
                Exercise("ex_bench", "Bench press", MuscleGroup.CHEST, defaultRestSeconds = 120),
                Exercise("ex_squat", "Dřep s činkou", MuscleGroup.LEGS, defaultRestSeconds = 180),
                Exercise("ex_deadlift", "Mrtvý tah", MuscleGroup.BACK, defaultRestSeconds = 180),
                Exercise("ex_ohp", "Tlaky na ramena (OHP)", MuscleGroup.SHOULDERS, defaultRestSeconds = 120),
                Exercise("ex_pullup", "Shyby na hrazdě", MuscleGroup.BACK, defaultRestSeconds = 90),
                Exercise("ex_biceps", "Bicepsový zdvih", MuscleGroup.ARMS, defaultRestSeconds = 60),
                Exercise("ex_plank", "Plank", MuscleGroup.CORE, defaultRestSeconds = 60)
            )
            workoutDao.insertExercises(defaults.map { ExerciseEntity.fromDomain(it) })
        }
    }

    override fun getActiveSession(): Flow<WorkoutSession?> {
        return workoutDao.getActiveSession().map { it?.toDomain() }
    }

    override fun getCompletedSessions(): Flow<List<WorkoutSession>> {
        return workoutDao.getCompletedSessions().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getSetsForSession(sessionId: String): Flow<List<WorkoutSet>> {
        return workoutDao.getSetsForSession(sessionId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun startWorkout(title: String): WorkoutSession {
        val existing = workoutDao.getActiveSessionSync()
        if (existing != null) {
            return existing.toDomain()
        }
        val now = clock.nowMillis()
        val sessionId = "workout-$now-${(1000..9999).random()}"
        val session = WorkoutSession(
            id = sessionId,
            title = title,
            startTimeEpochMs = now,
            endTimeEpochMs = null,
            isCompleted = false,
            totalVolumeKg = 0.0
        )
        workoutDao.insertSession(WorkoutSessionEntity.fromDomain(session))

        mutationDao.insertMutation(
            SyncMutationEntity(
                id = "mut-$sessionId",
                entityName = "workout_session",
                recordId = sessionId,
                operation = "INSERT",
                payloadJson = "{\"title\":\"$title\",\"startTimeEpochMs\":$now}",
                createdAt = now
            )
        )
        return session
    }

    override suspend fun addSet(
        sessionId: String,
        exerciseId: String,
        weightKg: Double,
        reps: Int,
        rpe: Double?
    ): WorkoutSet {
        val currentSets = workoutDao.getSetsForSession(sessionId).first()
        val exerciseSets = currentSets.filter { it.exerciseId == exerciseId }
        val nextSetNumber = (exerciseSets.maxOfOrNull { it.setNumber } ?: 0) + 1
        val lastPerf = getLastPerformance(exerciseId)

        val now = clock.nowMillis()
        val setId = "set-$now-${(1000..9999).random()}"
        val set = WorkoutSet(
            id = setId,
            sessionId = sessionId,
            exerciseId = exerciseId,
            setNumber = nextSetNumber,
            weightKg = weightKg,
            reps = reps,
            rpe = rpe,
            isCompleted = false,
            previousPerformance = lastPerf
        )
        workoutDao.insertOrUpdateSet(WorkoutSetEntity.fromDomain(set))

        mutationDao.insertMutation(
            SyncMutationEntity(
                id = "mut-$setId",
                entityName = "workout_set",
                recordId = setId,
                operation = "INSERT",
                payloadJson = "{\"sessionId\":\"$sessionId\",\"exerciseId\":\"$exerciseId\",\"weight\":$weightKg,\"reps\":$reps}",
                createdAt = now
            )
        )
        return set
    }

    override suspend fun updateSet(set: WorkoutSet) {
        workoutDao.insertOrUpdateSet(WorkoutSetEntity.fromDomain(set))
        val now = clock.nowMillis()
        mutationDao.insertMutation(
            SyncMutationEntity(
                id = "mut-upd-${set.id}-$now",
                entityName = "workout_set",
                recordId = set.id,
                operation = "UPDATE",
                payloadJson = "{\"weight\":${set.weightKg},\"reps\":${set.reps},\"rpe\":${set.rpe},\"isCompleted\":${set.isCompleted}}",
                createdAt = now
            )
        )
    }

    override suspend fun deleteSet(setId: String) {
        workoutDao.deleteSet(setId)
        val now = clock.nowMillis()
        mutationDao.insertMutation(
            SyncMutationEntity(
                id = "mut-del-$setId-$now",
                entityName = "workout_set",
                recordId = setId,
                operation = "DELETE",
                payloadJson = "{}",
                createdAt = now
            )
        )
    }

    override suspend fun completeWorkout(sessionId: String): WorkoutSession? {
        val now = clock.nowMillis()
        val sets = workoutDao.getSetsForSession(sessionId).first()
        val totalVolume = sets
            .filter { it.isCompleted }
            .sumOf { it.weightKg * it.reps }

        workoutDao.completeSession(sessionId, now, totalVolume)

        mutationDao.insertMutation(
            SyncMutationEntity(
                id = "mut-comp-$sessionId-$now",
                entityName = "workout_session",
                recordId = sessionId,
                operation = "COMPLETE",
                payloadJson = "{\"endTime\":$now,\"totalVolumeKg\":$totalVolume}",
                createdAt = now
            )
        )

        return workoutDao.getCompletedSessions().first().firstOrNull { it.id == sessionId }?.toDomain()
    }

    override suspend fun cancelWorkout(sessionId: String) {
        workoutDao.deleteSetsForSession(sessionId)
        workoutDao.deleteSession(sessionId)
    }

    override suspend fun getLastPerformance(exerciseId: String): String? {
        val last = workoutDao.getLastPerformanceForExercise(exerciseId) ?: return null
        val weightStr = if (last.weightKg % 1.0 == 0.0) last.weightKg.toInt().toString() else last.weightKg.toString()
        return "$weightStr kg × ${last.reps}"
    }
}
