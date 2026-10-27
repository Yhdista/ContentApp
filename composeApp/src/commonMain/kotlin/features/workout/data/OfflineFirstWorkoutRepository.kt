package features.workout.data

import core.common.AppClock
import core.database.MutationDao
import core.database.SyncMutationEntity
import features.workout.data.datasource.WorkoutLocalDataSource
import features.workout.data.model.CompleteSessionSyncPayload
import features.workout.data.model.CreateTemplateSyncPayload
import features.workout.data.model.ExerciseSyncPayload
import features.workout.data.model.InsertSetSyncPayload
import features.workout.data.model.StartSessionSyncPayload
import features.workout.data.model.UpdateSetSyncPayload
import features.workout.domain.Exercise
import features.workout.domain.ExerciseStats
import features.workout.domain.ExerciseStatsCalculator
import features.workout.domain.MuscleGroup
import features.workout.domain.PersonalRecordCalculator
import features.workout.domain.WorkoutDataExporter
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

/**
 * Offline-first implementace repozitáře tréninků koordinující:
 * 1. Lokální perzistenci přes hluboký modul [WorkoutLocalDataSource] (čisté doménové modely)
 * 2. Transakční outbox synchronizaci přes [MutationDao] s typově bezpečnou JSON serializací
 */
class OfflineFirstWorkoutRepository(
    private val localDataSource: WorkoutLocalDataSource,
    private val mutationDao: MutationDao,
    private val clock: AppClock
) : WorkoutRepository {

    private val json = Json { encodeDefaults = true }

    override fun getExercises(): Flow<List<Exercise>> =
        localDataSource.observeExercises()

    override suspend fun ensureDefaultExercises() {
        if (localDataSource.getExercisesCount() == 0) {
            val defaults = listOf(
                Exercise("ex_bench", "Bench press", MuscleGroup.CHEST, defaultRestSeconds = 120),
                Exercise("ex_squat", "Dřep s činkou", MuscleGroup.LEGS, defaultRestSeconds = 180),
                Exercise("ex_deadlift", "Mrtvý tah", MuscleGroup.BACK, defaultRestSeconds = 180),
                Exercise("ex_ohp", "Tlaky na ramena (OHP)", MuscleGroup.SHOULDERS, defaultRestSeconds = 120),
                Exercise("ex_pullup", "Shyby na hrazdě", MuscleGroup.BACK, defaultRestSeconds = 90),
                Exercise("ex_biceps", "Bicepsový zdvih", MuscleGroup.ARMS, defaultRestSeconds = 60),
                Exercise("ex_plank", "Plank", MuscleGroup.CORE, defaultRestSeconds = 60)
            )
            localDataSource.saveExercises(defaults)
        }
    }

    override suspend fun createCustomExercise(
        name: String,
        muscleGroup: MuscleGroup,
        defaultRestSeconds: Int
    ): Exercise {
        val now = clock.nowMillis()
        val trimmedName = name.trim()
        val exerciseId = "custom-ex-$now-${(1000..9999).random()}"
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
                id = "mut-$exerciseId",
                entityName = "exercise",
                recordId = exerciseId,
                operation = "INSERT",
                payloadJson = json.encodeToString(payload),
                createdAt = now
            )
        )
        return exercise
    }

    override fun getActiveSession(): Flow<WorkoutSession?> =
        localDataSource.observeActiveSession()

    override fun getCompletedSessions(): Flow<List<WorkoutSession>> =
        localDataSource.observeCompletedSessions()

    override fun getPendingSyncMutationsCount(): Flow<Int> =
        mutationDao.getPendingMutations().map { it.size }

    override suspend fun getSessionById(sessionId: String): WorkoutSession? =
        localDataSource.getSessionById(sessionId)

    override fun getSetsForSession(sessionId: String): Flow<List<WorkoutSet>> =
        localDataSource.observeSetsForSession(sessionId)

    override suspend fun startWorkout(title: String): WorkoutSession {
        val existing = localDataSource.getActiveSession()
        if (existing != null) {
            return existing
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
        localDataSource.saveSession(session)

        val payload = StartSessionSyncPayload(
            title = title,
            startTimeEpochMs = now
        )
        mutationDao.insertMutation(
            SyncMutationEntity(
                id = "mut-$sessionId",
                entityName = "workout_session",
                recordId = sessionId,
                operation = "INSERT",
                payloadJson = json.encodeToString(payload),
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
        val currentSets = localDataSource.observeSetsForSession(sessionId).first()
        val exerciseSets = currentSets.filter { it.exerciseId == exerciseId }
        val nextSetNumber = (exerciseSets.maxOfOrNull { it.setNumber } ?: 0) + 1
        val lastPerformance = getLastPerformance(exerciseId)

        val previousSameExerciseSets = exerciseSets.filter { it.isCompleted }
        val sessionMax1RM = previousSameExerciseSets.maxOfOrNull {
            PersonalRecordCalculator.calculateOneRepMax(it.weightKg, it.reps)
        } ?: 0.0
        val historicalMax1RM = getHistoricalMax1RM(exerciseId, sessionId)
        val overallBest1RM = maxOf(sessionMax1RM, historicalMax1RM)
        val isPR = PersonalRecordCalculator.isPersonalRecord(weightKg, reps, overallBest1RM)

        val now = clock.nowMillis()
        val setId = "set-$sessionId-$exerciseId-$nextSetNumber-$now"
        val workoutSet = WorkoutSet(
            id = setId,
            sessionId = sessionId,
            exerciseId = exerciseId,
            setNumber = nextSetNumber,
            weightKg = weightKg,
            reps = reps,
            rpe = rpe,
            isCompleted = false,
            previousPerformance = lastPerformance,
            isPersonalRecord = isPR,
            setType = WorkoutSetType.NORMAL
        )
        localDataSource.saveSet(workoutSet)

        val payload = InsertSetSyncPayload(
            sessionId = sessionId,
            exerciseId = exerciseId,
            setNumber = nextSetNumber,
            weightKg = weightKg,
            reps = reps,
            setType = WorkoutSetType.NORMAL.name
        )
        mutationDao.insertMutation(
            SyncMutationEntity(
                id = "mut-$setId",
                entityName = "workout_set",
                recordId = setId,
                operation = "INSERT",
                payloadJson = json.encodeToString(payload),
                createdAt = now
            )
        )
        return workoutSet
    }

    override suspend fun updateSet(set: WorkoutSet) {
        val currentSets = localDataSource.observeSetsForSession(set.sessionId).first()
        val previousSameExerciseSets = currentSets
            .filter { it.exerciseId == set.exerciseId && it.id != set.id && it.isCompleted }
        val sessionMax1RM = previousSameExerciseSets.maxOfOrNull {
            PersonalRecordCalculator.calculateOneRepMax(it.weightKg, it.reps)
        } ?: 0.0
        val historicalMax1RM = getHistoricalMax1RM(set.exerciseId, set.sessionId)
        val overallBest1RM = maxOf(sessionMax1RM, historicalMax1RM)

        val isPR = if (set.isCompleted && set.setType != WorkoutSetType.WARMUP) {
            PersonalRecordCalculator.isPersonalRecord(set.weightKg, set.reps, overallBest1RM)
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
            setType = set.setType.name
        )
        mutationDao.insertMutation(
            SyncMutationEntity(
                id = "mut-upd-${set.id}-$now",
                entityName = "workout_set",
                recordId = set.id,
                operation = "UPDATE",
                payloadJson = json.encodeToString(payload),
                createdAt = now
            )
        )
    }

    override suspend fun deleteSet(setId: String) {
        localDataSource.deleteSet(setId)
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
        val sets = localDataSource.observeSetsForSession(sessionId).first()
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
                id = "mut-complete-$sessionId-$now",
                entityName = "workout_session",
                recordId = sessionId,
                operation = "COMPLETE",
                payloadJson = json.encodeToString(payload),
                createdAt = now
            )
        )
        return getSessionById(sessionId)
    }

    override suspend fun cancelWorkout(sessionId: String) {
        localDataSource.deleteSessionWithSets(sessionId)
        val now = clock.nowMillis()
        mutationDao.insertMutation(
            SyncMutationEntity(
                id = "mut-cancel-$sessionId-$now",
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
        return sets.maxOfOrNull {
            PersonalRecordCalculator.calculateOneRepMax(it.weightKg, it.reps)
        } ?: 0.0
    }

    override fun getTemplates(): Flow<List<WorkoutTemplate>> =
        localDataSource.observeTemplates()

    override suspend fun ensureDefaultTemplates() {
        if (localDataSource.getTemplatesCount() == 0) {
            val templates = listOf(
                WorkoutTemplate(
                    id = "tmpl_push",
                    name = "Push Day",
                    description = "Hrudník, ramena a triceps",
                    exercises = listOf(
                        WorkoutTemplateExercise("ex_bench", "Bench press", 3, 60.0, 8, 0),
                        WorkoutTemplateExercise("ex_ohp", "Tlaky na ramena (OHP)", 3, 40.0, 10, 1)
                    )
                ),
                WorkoutTemplate(
                    id = "tmpl_pull",
                    name = "Pull Day",
                    description = "Záda, biceps a zadní ramena",
                    exercises = listOf(
                        WorkoutTemplateExercise("ex_deadlift", "Mrtvý tah", 4, 100.0, 5, 0),
                        WorkoutTemplateExercise("ex_pullup", "Shyby na hrazdě", 3, 0.0, 8, 1),
                        WorkoutTemplateExercise("ex_biceps", "Bicepsový zdvih", 3, 15.0, 12, 2)
                    )
                ),
                WorkoutTemplate(
                    id = "tmpl_legs",
                    name = "Leg Day",
                    description = "Kvadricepsy, hamstringy a lýtka",
                    exercises = listOf(
                        WorkoutTemplateExercise("ex_squat", "Dřep s činkou", 4, 80.0, 8, 0),
                        WorkoutTemplateExercise("ex_plank", "Plank", 3, 0.0, 60, 1)
                    )
                )
            )
            localDataSource.saveTemplates(templates)
        }
    }

    override suspend fun createTemplate(
        name: String,
        description: String,
        exercises: List<WorkoutTemplateExercise>
    ): WorkoutTemplate {
        val now = clock.nowMillis()
        val trimmedName = name.trim()
        val templateId = "tmpl-$now-${(1000..9999).random()}"
        val template = WorkoutTemplate(
            id = templateId,
            name = trimmedName,
            description = description.trim(),
            exercises = exercises.mapIndexed { idx, ex -> ex.copy(orderIndex = idx) }
        )
        localDataSource.saveTemplate(template)

        val payload = CreateTemplateSyncPayload(
            name = trimmedName,
            exercisesCount = exercises.size
        )
        mutationDao.insertMutation(
            SyncMutationEntity(
                id = "mut-$templateId",
                entityName = "workout_template",
                recordId = templateId,
                operation = "INSERT",
                payloadJson = json.encodeToString(payload),
                createdAt = now
            )
        )
        return template
    }

    override suspend fun startWorkoutFromTemplate(templateId: String): WorkoutSession {
        val template = localDataSource.getTemplateById(templateId)
        val title = template?.name ?: "Trénink ze šablony"
        val session = startWorkout(title = title)

        for (ex in template?.exercises.orEmpty()) {
            val lastPerf = getLastPerformance(ex.exerciseId)
            val weight = if (ex.defaultWeightKg > 0.0) ex.defaultWeightKg else 20.0
            val reps = if (ex.defaultReps > 0) ex.defaultReps else 10
            for (i in 1..ex.targetSets) {
                val setId = "set-${session.id}-${ex.exerciseId}-$i-${clock.nowMillis()}"
                val set = WorkoutSet(
                    id = setId,
                    sessionId = session.id,
                    exerciseId = ex.exerciseId,
                    setNumber = i,
                    weightKg = weight,
                    reps = reps,
                    rpe = null,
                    isCompleted = false,
                    previousPerformance = lastPerf,
                    setType = WorkoutSetType.NORMAL
                )
                localDataSource.saveSet(set)
            }
        }
        return session
    }

    override suspend fun saveSessionAsTemplate(sessionId: String, templateName: String): WorkoutTemplate {
        val sessionSets = localDataSource.observeSetsForSession(sessionId).first()
        val exercisesMap = sessionSets.groupBy { it.exerciseId }
        val templateExercises = mutableListOf<WorkoutTemplateExercise>()

        var orderIndex = 0
        for ((exerciseId, sets) in exercisesMap) {
            val exercise = localDataSource.getExerciseById(exerciseId)
            val exName = exercise?.name ?: "Cvik"
            val avgWeight = sets.map { it.weightKg }.average().let { if (it.isNaN()) 20.0 else it }
            val avgReps = sets.map { it.reps }.average().toInt().coerceAtLeast(1)
            templateExercises.add(
                WorkoutTemplateExercise(
                    exerciseId = exerciseId,
                    exerciseName = exName,
                    targetSets = sets.size.coerceAtLeast(1),
                    defaultWeightKg = avgWeight,
                    defaultReps = avgReps,
                    orderIndex = orderIndex++
                )
            )
        }

        return createTemplate(
            name = templateName,
            description = "Vytvořeno z tréninku",
            exercises = templateExercises
        )
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

    override suspend fun exportWorkoutDataCsv(): String {
        val sessions = localDataSource.getAllSessions()
        val exercises = localDataSource.getAllExercises()
        val sets = localDataSource.getAllCompletedSets()
        return WorkoutDataExporter.exportToCsv(sessions, exercises, sets)
    }

    override suspend fun exportWorkoutDataJson(): String {
        val sessions = localDataSource.getAllSessions()
        val exercises = localDataSource.getAllExercises()
        val sets = localDataSource.getAllCompletedSets()
        val templates = localDataSource.observeTemplates().first()
        return WorkoutDataExporter.exportToJson(sessions, exercises, sets, templates)
    }
}
