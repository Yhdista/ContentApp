package features.workout.data

import core.common.AppClock
import core.database.MutationDao
import core.database.SyncMutationEntity
import features.workout.data.dao.WorkoutDao
import features.workout.data.entity.ExerciseEntity
import features.workout.data.entity.WorkoutSessionEntity
import features.workout.data.entity.WorkoutSetEntity
import features.workout.data.entity.WorkoutTemplateEntity
import features.workout.data.entity.WorkoutTemplateExerciseEntity
import features.workout.domain.Exercise
import features.workout.domain.MuscleGroup
import features.workout.domain.PersonalRecordCalculator
import features.workout.domain.WorkoutRepository
import features.workout.domain.WorkoutSession
import features.workout.domain.WorkoutSet
import features.workout.domain.WorkoutTemplate
import features.workout.domain.WorkoutTemplateExercise
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

    override suspend fun createCustomExercise(
        name: String,
        muscleGroup: MuscleGroup,
        defaultRestSeconds: Int
    ): Exercise {
        val now = clock.nowMillis()
        val exerciseId = "custom-ex-$now-${(1000..9999).random()}"
        val exercise = Exercise(
            id = exerciseId,
            name = name.trim(),
            muscleGroup = muscleGroup,
            defaultRestSeconds = defaultRestSeconds
        )
        workoutDao.insertExercise(ExerciseEntity.fromDomain(exercise))

        mutationDao.insertMutation(
            SyncMutationEntity(
                id = "mut-$exerciseId",
                entityName = "exercise",
                recordId = exerciseId,
                operation = "INSERT",
                payloadJson = "{\"name\":\"${name.trim()}\",\"muscleGroup\":\"${muscleGroup.name}\",\"defaultRestSeconds\":$defaultRestSeconds}",
                createdAt = now
            )
        )
        return exercise
    }

    override fun getActiveSession(): Flow<WorkoutSession?> {
        return workoutDao.getActiveSession().map { it?.toDomain() }
    }

    override fun getCompletedSessions(): Flow<List<WorkoutSession>> {
        return workoutDao.getCompletedSessions().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getPendingSyncMutationsCount(): Flow<Int> {
        return mutationDao.getPendingMutations().map { it.size }
    }

    override suspend fun getSessionById(sessionId: String): WorkoutSession? {
        return workoutDao.getSessionById(sessionId)?.toDomain()
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
        val isPR = if (set.isCompleted && set.weightKg > 0.0 && set.reps > 0) {
            val historical = workoutDao.getHistoricalCompletedSets(set.exerciseId, set.sessionId)
            if (historical.isEmpty()) {
                true
            } else {
                val max1RM = historical.maxOfOrNull { PersonalRecordCalculator.calculateOneRepMax(it.weightKg, it.reps) } ?: 0.0
                PersonalRecordCalculator.isPersonalRecord(set.weightKg, set.reps, max1RM)
            }
        } else {
            false
        }
        val setWithPR = set.copy(isPersonalRecord = isPR)
        workoutDao.insertOrUpdateSet(WorkoutSetEntity.fromDomain(setWithPR))
        val now = clock.nowMillis()
        mutationDao.insertMutation(
            SyncMutationEntity(
                id = "mut-upd-${set.id}-$now",
                entityName = "workout_set",
                recordId = set.id,
                operation = "UPDATE",
                payloadJson = "{\"weight\":${set.weightKg},\"reps\":${set.reps},\"rpe\":${set.rpe},\"isCompleted\":${set.isCompleted},\"isPR\":$isPR}",
                createdAt = now
            )
        )
    }

    override suspend fun getHistoricalMax1RM(exerciseId: String, currentSessionId: String): Double {
        val historical = workoutDao.getHistoricalCompletedSets(exerciseId, currentSessionId)
        return historical.maxOfOrNull { PersonalRecordCalculator.calculateOneRepMax(it.weightKg, it.reps) } ?: 0.0
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

    override fun getTemplates(): Flow<List<WorkoutTemplate>> {
        return workoutDao.getAllTemplates().map { list ->
            list.map { entity ->
                val exercises = workoutDao.getTemplateExercises(entity.id).map { it.toDomain() }
                entity.toDomain(exercises)
            }
        }
    }

    override suspend fun ensureDefaultTemplates() {
        if (workoutDao.getTemplatesCount() == 0) {
            val tPush = WorkoutTemplate(
                id = "tmpl_push",
                name = "Push Day (Hrudník & Ramena)",
                description = "Trénink zaměřený na tlakové cviky: Bench press a tlaky na ramena.",
                exercises = listOf(
                    WorkoutTemplateExercise("ex_bench", "Bench press", targetSets = 3, defaultWeightKg = 60.0, defaultReps = 10, orderIndex = 0),
                    WorkoutTemplateExercise("ex_ohp", "Tlaky na ramena (OHP)", targetSets = 3, defaultWeightKg = 40.0, defaultReps = 10, orderIndex = 1)
                )
            )
            val tPull = WorkoutTemplate(
                id = "tmpl_pull",
                name = "Pull Day (Záda & Biceps)",
                description = "Tahový trénink: Mrtvý tah a shyby na hrazdě.",
                exercises = listOf(
                    WorkoutTemplateExercise("ex_deadlift", "Mrtvý tah", targetSets = 3, defaultWeightKg = 100.0, defaultReps = 6, orderIndex = 0),
                    WorkoutTemplateExercise("ex_pullup", "Shyby na hrazdě", targetSets = 3, defaultWeightKg = 0.0, defaultReps = 8, orderIndex = 1),
                    WorkoutTemplateExercise("ex_biceps", "Bicepsový zdvih", targetSets = 3, defaultWeightKg = 25.0, defaultReps = 12, orderIndex = 2)
                )
            )
            val tLegs = WorkoutTemplate(
                id = "tmpl_legs",
                name = "Leg Day (Nohy & Core)",
                description = "Komplexní trénink nohou: Dřepy a plank.",
                exercises = listOf(
                    WorkoutTemplateExercise("ex_squat", "Dřep s činkou", targetSets = 4, defaultWeightKg = 80.0, defaultReps = 8, orderIndex = 0),
                    WorkoutTemplateExercise("ex_plank", "Plank", targetSets = 3, defaultWeightKg = 0.0, defaultReps = 60, orderIndex = 1)
                )
            )

            for (tmpl in listOf(tPush, tPull, tLegs)) {
                workoutDao.insertTemplate(WorkoutTemplateEntity.fromDomain(tmpl))
                workoutDao.insertTemplateExercises(tmpl.exercises.map { WorkoutTemplateExerciseEntity.fromDomain(tmpl.id, it) })
            }
        }
    }

    override suspend fun createTemplate(
        name: String,
        description: String,
        exercises: List<WorkoutTemplateExercise>
    ): WorkoutTemplate {
        val now = clock.nowMillis()
        val templateId = "tmpl-$now-${(1000..9999).random()}"
        val template = WorkoutTemplate(
            id = templateId,
            name = name.trim(),
            description = description.trim(),
            exercises = exercises.mapIndexed { idx, ex -> ex.copy(orderIndex = idx) }
        )
        workoutDao.insertTemplate(WorkoutTemplateEntity.fromDomain(template))
        workoutDao.insertTemplateExercises(template.exercises.map { WorkoutTemplateExerciseEntity.fromDomain(templateId, it) })

        mutationDao.insertMutation(
            SyncMutationEntity(
                id = "mut-$templateId",
                entityName = "workout_template",
                recordId = templateId,
                operation = "INSERT",
                payloadJson = "{\"name\":\"${name.trim()}\",\"exercisesCount\":${exercises.size}}",
                createdAt = now
            )
        )
        return template
    }

    override suspend fun startWorkoutFromTemplate(templateId: String): WorkoutSession {
        val templateEntity = workoutDao.getTemplateById(templateId)
        val templateExercises = workoutDao.getTemplateExercises(templateId)
        val title = templateEntity?.name ?: "Trénink ze šablony"
        val session = startWorkout(title = title)

        for (ex in templateExercises) {
            val lastPerf = getLastPerformance(ex.exerciseId)
            val weight = if (ex.defaultWeightKg > 0.0) ex.defaultWeightKg else 20.0
            val reps = if (ex.defaultReps > 0) ex.defaultReps else 10
            for (i in 1..ex.targetSets) {
                val setId = "set-${session.id}-${ex.exerciseId}-$i-${clock.nowMillis()}"
                val setEntity = WorkoutSetEntity(
                    id = setId,
                    sessionId = session.id,
                    exerciseId = ex.exerciseId,
                    setNumber = i,
                    weightKg = weight,
                    reps = reps,
                    rpe = null,
                    isCompleted = false,
                    previousPerformance = lastPerf
                )
                workoutDao.insertOrUpdateSet(setEntity)
            }
        }
        return session
    }

    override suspend fun saveSessionAsTemplate(sessionId: String, templateName: String): WorkoutTemplate {
        val sessionSets = workoutDao.getSetsForSession(sessionId).first()
        val exercisesMap = sessionSets.groupBy { it.exerciseId }
        val templateExercises = mutableListOf<WorkoutTemplateExercise>()

        var orderIndex = 0
        for ((exerciseId, sets) in exercisesMap) {
            val exEntity = workoutDao.getExerciseById(exerciseId)
            val exName = exEntity?.name ?: "Cvik"
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
}
