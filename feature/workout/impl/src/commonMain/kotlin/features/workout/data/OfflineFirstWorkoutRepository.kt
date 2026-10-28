package features.workout.data

import core.common.AppClock
import core.database.MutationDao
import core.database.SyncMutationEntity
import core.storage.CommandCoordinator
import features.workout.data.datasource.WorkoutLocalDataSource
import features.workout.data.model.CompleteSessionSyncPayload
import features.workout.data.model.ExerciseSyncPayload
import features.workout.data.model.StartSessionSyncPayload
import features.workout.data.model.UpdateSetSyncPayload
import features.exercises.api.Exercise
import features.workout.domain.ExerciseStats
import features.workout.domain.ExerciseStatsCalculator
import features.exercises.api.MuscleGroup
import features.workout.domain.PersonalRecordCalculator
import features.workout.domain.WorkoutRepository
import features.workout.domain.WorkoutSession
import features.workout.domain.WorkoutSet
import features.workout.domain.WorkoutSetType
import features.workout.domain.WorkoutTemplate
import features.workout.domain.WorkoutTemplateExercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Offline-first implementace repozitáře tréninků koordinující:
 * 1. Lokální perzistenci přes hluboký modul [WorkoutLocalDataSource] (čisté doménové modely)
 * 2. Transakční outbox synchronizaci přes [MutationDao] s klientskými UUID a typově bezpečnou JSON serializací
 */
@OptIn(ExperimentalUuidApi::class)
internal class OfflineFirstWorkoutRepository(
    private val localDataSource: WorkoutLocalDataSource,
    private val mutationDao: MutationDao,
    private val clock: AppClock,
    private val writes: CommandCoordinator,
    private val store: features.workout.domain.WorkoutStore
) : WorkoutRepository {

    private val json = Json { encodeDefaults = true }

    override fun getExercises(): Flow<List<Exercise>> =
        localDataSource.observeExercises()

    override suspend fun ensureDefaultExercises() = writes.write {
        if (localDataSource.getExercisesCount() == 0) {
            val defaults = WorkoutDefaults.exercises
            localDataSource.saveExercises(defaults)
        }
    }

    override suspend fun createCustomExercise(
        name: String,
        muscleGroup: MuscleGroup,
        defaultRestSeconds: Int
    ): Exercise = writes.write {
        val now = clock.nowMillis()
        val trimmedName = name.trim()
        require(trimmedName.isNotEmpty() && defaultRestSeconds > 0)
        val exerciseId = "custom-ex-${Uuid.random()}"
        val exercise = Exercise(
            id = exerciseId,
            name = trimmedName,
            muscleGroup = muscleGroup,
            defaultRestSeconds = defaultRestSeconds
        )
        localDataSource.saveExercise(exercise)

        val payload = ExerciseSyncPayload(
            name = trimmedName,
            muscleGroup = muscleGroup.name,
            defaultRestSeconds = defaultRestSeconds
        )
        mutationDao.insertMutation(
            SyncMutationEntity(
                id = "mut-${Uuid.random()}",
                entityName = "exercise",
                recordId = exerciseId,
                operation = "INSERT",
                payloadJson = json.encodeToString(payload),
                createdAt = now
            )
        )
        return@write exercise
    }

    override fun getActiveSession(): Flow<WorkoutSession?> =
        localDataSource.observeActiveSession()

    override fun getCompletedSessions(): Flow<List<WorkoutSession>> =
        localDataSource.observeCompletedSessions()

    override fun getPendingSyncMutationsCount(): Flow<Int> =
        mutationDao.getUnsentMutationsCount()

    override suspend fun getSessionById(sessionId: String): WorkoutSession? =
        localDataSource.getSessionById(sessionId)

    override fun getSetsForSession(sessionId: String): Flow<List<WorkoutSet>> =
        localDataSource.observeSetsForSession(sessionId)

    override suspend fun startWorkout(title: String): WorkoutSession = writes.write {
        val existing = localDataSource.getActiveSession()
        if (existing != null) {
            return@write existing
        }
        val now = clock.nowMillis()
        val sessionId = "workout-${Uuid.random()}"
        val session = WorkoutSession(
            id = sessionId,
            title = title,
            startTimeEpochMs = now,
            endTimeEpochMs = null,
            isCompleted = false,
            totalVolumeKg = 0.0
        )
        localDataSource.saveSession(session)

        val payload = StartSessionSyncPayload(
            title = title,
            startTimeEpochMs = now
        )
        mutationDao.insertMutation(
            SyncMutationEntity(
                id = "mut-${Uuid.random()}",
                entityName = "workout_session",
                recordId = sessionId,
                operation = "INSERT",
                payloadJson = json.encodeToString(payload),
                createdAt = now
            )
        )
        return@write session
    }

    override fun getWorkoutExercises(sessionId: String) = localDataSource.observeWorkoutExercises(sessionId)

    override suspend fun addExerciseEntry(sessionId: String, exerciseId: String): features.workout.domain.WorkoutExercise = writes.write {
        val session = localDataSource.getSessionById(sessionId) ?: throw NoSuchElementException("Session not found")
        check(!session.isCompleted)
        val exercise = localDataSource.getExerciseById(exerciseId)
        require(exercise?.archived != true)
        val entries = localDataSource.getWorkoutExercises(sessionId)
        val entry = features.workout.domain.WorkoutExercise(Uuid.random().toString(), sessionId, exerciseId,
            exercise?.name ?: "Neznámý cvik", exercise?.muscleGroup ?: MuscleGroup.CHEST,
            exercise?.defaultRestSeconds ?: 90, (entries.maxOfOrNull { it.orderIndex } ?: -1) + 1)
        localDataSource.saveWorkoutExercise(entry)
        mutationDao.insertMutation(SyncMutationEntity(Uuid.random().toString(),"workout_exercise",entry.id,"INSERT",json.encodeToString(entry),clock.nowMillis()))
        entry
    }

    override suspend fun addSetToEntry(sessionId: String, workoutExerciseId: String, weightKg: Double, reps: Int, rpe: Double?): WorkoutSet = writes.write {
        check(localDataSource.getWorkoutExercises(sessionId).any { it.id == workoutExerciseId }) { "Workout entry not found" }
        addSet(sessionId, workoutExerciseId, weightKg, reps, rpe)
    }

    override suspend fun removeWorkoutExercise(sessionId: String, workoutExerciseId: String) = writes.write {
        check(localDataSource.getWorkoutExercises(sessionId).any { it.id == workoutExerciseId }) { "Workout entry not found" }
        removeExerciseFromWorkout(sessionId, workoutExerciseId)
    }

    override suspend fun addSet(
        sessionId: String,
        exerciseId: String,
        weightKg: Double,
        reps: Int,
        rpe: Double?
    ): WorkoutSet = writes.write {
        require(weightKg.isFinite() && weightKg >= 0 && reps >= 0)
        require(rpe == null || (rpe.isFinite() && rpe in 0.0..10.0))
        val session = localDataSource.getSessionById(sessionId) ?: throw NoSuchElementException("Session not found")
        check(!session.isCompleted) { "Cannot edit a completed workout" }
        val currentSets = localDataSource.getSetsForSessionSnapshot(sessionId)
        val entries = localDataSource.getWorkoutExercises(sessionId)
        val entry = entries.firstOrNull { it.id == exerciseId }
            ?: entries.firstOrNull { it.exerciseId == exerciseId }
            ?: addExerciseEntry(sessionId, exerciseId)
        val exerciseSets = currentSets.filter { it.workoutExerciseId == entry.id }
        val nextSetNumber = (exerciseSets.maxOfOrNull { it.setNumber } ?: 0) + 1
        val lastPerformance = getLastPerformance(entry.exerciseId)
        val isPR = evaluateIsPersonalRecord(
            sessionSets = exerciseSets,
            excludeSetId = null,
            sessionId = sessionId,
            exerciseId = entry.exerciseId,
            weightKg = weightKg,
            reps = reps,
            setType = WorkoutSetType.NORMAL
        )

        val now = clock.nowMillis()
        val setId = "set-${Uuid.random()}"
        val workoutSet = WorkoutSet(
            id = setId,
            sessionId = sessionId,
            exerciseId = entry.exerciseId,
            setNumber = nextSetNumber,
            weightKg = weightKg,
            reps = reps,
            rpe = rpe,
            isCompleted = false,
            previousPerformance = lastPerformance,
            isPersonalRecord = isPR,
            setType = WorkoutSetType.NORMAL,
            workoutExerciseId = entry.id
        )
        localDataSource.saveSet(workoutSet)
        enqueueInsertSetMutation(workoutSet, now)
        return@write workoutSet
    }

    override suspend fun updateSet(setId: String, update: (WorkoutSet) -> WorkoutSet): WorkoutSet = writes.write {
        val current = localDataSource.getSetById(setId) ?: throw NoSuchElementException("Set not found")
        val session = localDataSource.getSessionById(current.sessionId) ?: throw NoSuchElementException("Session not found")
        check(!session.isCompleted) { "Cannot edit a completed workout" }
        val proposed = update(current)
        require(proposed.workoutExerciseId == current.workoutExerciseId && proposed.prescribedWeightKg == current.prescribedWeightKg &&
            proposed.prescribedReps == current.prescribedReps && proposed.prescribedSetType == current.prescribedSetType)
        val set = proposed
        require(set.id == current.id && set.sessionId == current.sessionId && set.exerciseId == current.exerciseId) {
            "An edit cannot change set identity"
        }
        require(set.weightKg.isFinite() && set.weightKg >= 0 && set.reps >= 0)
        val rpe = set.rpe
        require(rpe == null || (rpe.isFinite() && rpe in 0.0..10.0))
        val currentSets = localDataSource.getSetsForSessionSnapshot(set.sessionId)
        val exerciseSets = currentSets.filter { it.exerciseId == set.exerciseId }
        val isPR = if (set.isCompleted) {
            evaluateIsPersonalRecord(
                sessionSets = exerciseSets,
                excludeSetId = set.id,
                sessionId = set.sessionId,
                exerciseId = set.exerciseId,
                weightKg = set.weightKg,
                reps = set.reps,
                setType = set.setType
            )
        } else {
            false
        }

        val updatedSet = set.copy(isPersonalRecord = isPR)
        localDataSource.saveSet(updatedSet)

        val now = clock.nowMillis()
        val payload = UpdateSetSyncPayload(
            weightKg = set.weightKg,
            reps = set.reps,
            rpe = set.rpe,
            isCompleted = set.isCompleted,
            isPersonalRecord = isPR,
            setType = set.setType.name,
            setNumber = set.setNumber
        )
        mutationDao.insertMutation(
            SyncMutationEntity(
                id = "mut-upd-${Uuid.random()}",
                entityName = "workout_set",
                recordId = set.id,
                operation = "UPDATE",
                payloadJson = json.encodeToString(payload),
                createdAt = now
            )
        )
        updatedSet
    }

    private suspend fun evaluateIsPersonalRecord(
        sessionSets: List<WorkoutSet>,
        excludeSetId: String?,
        sessionId: String,
        exerciseId: String,
        weightKg: Double,
        reps: Int,
        setType: WorkoutSetType
    ): Boolean {
        if (setType == WorkoutSetType.WARMUP) return false
        val completedWorkingSetsInSession = sessionSets.filter {
            it.id != excludeSetId && it.isCompleted && it.setType != WorkoutSetType.WARMUP
        }
        val sessionMax1RM = completedWorkingSetsInSession.maxOfOrNull {
            PersonalRecordCalculator.calculateOneRepMax(it.weightKg, it.reps)
        } ?: 0.0
        val historicalMax1RM = getHistoricalMax1RM(exerciseId, sessionId)
        val overallBest1RM = maxOf(sessionMax1RM, historicalMax1RM)
        return PersonalRecordCalculator.isPersonalRecord(weightKg, reps, overallBest1RM)
    }

    private suspend fun enqueueInsertSetMutation(workoutSet: WorkoutSet, createdAt: Long) {
        mutationDao.insertMutation(
            SyncMutationEntity(
                id = "mut-${Uuid.random()}",
                entityName = "workout_set",
                recordId = workoutSet.id,
                operation = "INSERT",
                payloadJson = json.encodeToString(workoutSet),
                createdAt = createdAt
            )
        )
    }

    override suspend fun deleteSet(setId: String) = writes.write {
        val deleted = localDataSource.getSetById(setId) ?: return@write
        val session = localDataSource.getSessionById(deleted.sessionId) ?: throw NoSuchElementException("Session not found")
        check(!session.isCompleted) { "Cannot edit a completed workout" }
        localDataSource.deleteSet(setId)
        val now = clock.nowMillis()
        mutationDao.insertMutation(
            SyncMutationEntity(
                id = "mut-del-${Uuid.random()}",
                entityName = "workout_set",
                recordId = setId,
                operation = "DELETE",
                payloadJson = "{}",
                createdAt = now
            )
        )
        localDataSource.getSetsForSessionSnapshot(deleted.sessionId)
            .filter { it.workoutExerciseId == deleted.workoutExerciseId }
            .sortedBy { it.setNumber }
            .forEachIndexed { index, set ->
                if (set.setNumber != index + 1) {
                    updateSet(set.id) { it.copy(setNumber = index + 1) }
                }
            }
    }

    override suspend fun removeExerciseFromWorkout(sessionId: String, exerciseId: String) = writes.write {
        val session = localDataSource.getSessionById(sessionId) ?: throw NoSuchElementException("Session not found")
        check(!session.isCompleted) { "Cannot edit a completed workout" }
        val entries = localDataSource.getWorkoutExercises(sessionId)
        val entry = entries.firstOrNull { it.id == exerciseId } ?: entries.firstOrNull { it.exerciseId == exerciseId } ?: return@write
        localDataSource.getSetsForSessionSnapshot(sessionId).filter { it.workoutExerciseId == entry.id }.forEach { deleteSet(it.id) }
        localDataSource.deleteWorkoutExercise(entry.id)
        mutationDao.insertMutation(SyncMutationEntity(Uuid.random().toString(),"workout_exercise",entry.id,"DELETE","{}",clock.nowMillis()))
    }

    override suspend fun completeWorkout(sessionId: String): WorkoutSession? = writes.write {
        val session = localDataSource.getSessionById(sessionId) ?: return@write null
        if (session.isCompleted) return@write session
        val sets = localDataSource.getSetsForSessionSnapshot(sessionId)
        val completedSets = sets.filter { it.isCompleted }
        val totalVolume = completedSets.sumOf { it.weightKg * it.reps }
        val now = clock.nowMillis()

        localDataSource.completeSession(sessionId, now, totalVolume)

        val payload = CompleteSessionSyncPayload(
            isCompleted = true,
            endTimeEpochMs = now,
            totalVolumeKg = totalVolume
        )
        mutationDao.insertMutation(
            SyncMutationEntity(
                id = "mut-complete-${Uuid.random()}",
                entityName = "workout_session",
                recordId = sessionId,
                operation = "COMPLETE",
                payloadJson = json.encodeToString(payload),
                createdAt = now
            )
        )
        return@write getSessionById(sessionId)
    }

    override suspend fun cancelWorkout(sessionId: String) = writes.write {
        val session = localDataSource.getSessionById(sessionId) ?: throw NoSuchElementException("Session not found")
        check(!session.isCompleted) { "Cannot cancel a completed workout" }
        localDataSource.deleteSessionWithSets(sessionId)
        val now = clock.nowMillis()
        mutationDao.insertMutation(
            SyncMutationEntity(
                id = "mut-cancel-${Uuid.random()}",
                entityName = "workout_session",
                recordId = sessionId,
                operation = "DELETE",
                payloadJson = "{}",
                createdAt = now
            )
        )
    }

    override suspend fun getLastPerformance(exerciseId: String): String? {
        val lastSet = localDataSource.getLastCompletedSetForExercise(exerciseId) ?: return null
        return "${lastSet.weightKg} kg × ${lastSet.reps}"
    }

    override suspend fun getHistoricalMax1RM(exerciseId: String, currentSessionId: String): Double {
        val sets = localDataSource.getHistoricalCompletedSets(exerciseId, currentSessionId)
            .filter { it.setType != WorkoutSetType.WARMUP }
        return sets.maxOfOrNull {
            PersonalRecordCalculator.calculateOneRepMax(it.weightKg, it.reps)
        } ?: 0.0
    }

    override fun getTemplates(): Flow<List<WorkoutTemplate>> =
        localDataSource.observeTemplates()

    override suspend fun ensureDefaultTemplates() = writes.write {
        ensureDefaultExercises()
        if (localDataSource.getTemplatesCount() == 0) {
            val templates = WorkoutDefaults.templates
            localDataSource.saveTemplates(templates)
        }
    }

    override suspend fun createTemplate(
        name: String,
        description: String,
        exercises: List<WorkoutTemplateExercise>
    ): WorkoutTemplate = writes.write {
        val now = clock.nowMillis()
        val trimmedName = name.trim()
        val templateId = "tmpl-${Uuid.random()}"
        val template = WorkoutTemplate(
            id = templateId,
            name = trimmedName,
            description = description.trim(),
            exercises = exercises.mapIndexed { idx, ex -> ex.copy(orderIndex = idx) }
        )
        localDataSource.saveTemplate(template)

        return@write template
    }

    override suspend fun startWorkoutFromTemplate(templateId: String): WorkoutSession = writes.write {
        localDataSource.getActiveSession()?.let { return@write it }
        when (val result = store.createFromPrescription(localDataSource.routinePrescription(templateId))) {
            is core.result.Result.Success -> getSessionById(result.data.sessionId)!!
            is core.result.Result.Error -> error("Workout start failed: ${result.error}")
        }
    }

    override suspend fun saveSessionAsTemplate(sessionId: String, templateName: String): WorkoutTemplate = writes.write {
        localDataSource.saveWorkoutAsRoutine(sessionId, templateName)
    }

    override fun getExerciseStats(exerciseId: String): Flow<ExerciseStats?> {
        return combine(
            getExercises(),
            getCompletedSessions(),
            localDataSource.observeCompletedSetsForExercise(exerciseId)
        ) { exercises, sessions, sets ->
            val exercise = exercises.firstOrNull { it.id == exerciseId } ?: return@combine null
            ExerciseStatsCalculator.calculate(exercise, sessions, sets)
        }
    }

    override suspend fun getAllCompletedSets(): List<WorkoutSet> =
        localDataSource.getAllCompletedSets()

}
