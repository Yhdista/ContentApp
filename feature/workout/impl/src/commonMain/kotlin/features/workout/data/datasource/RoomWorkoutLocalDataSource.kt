package features.workout.data.datasource

import features.workout.data.dao.WorkoutDao
import features.workout.data.mapper.toDomain
import features.workout.data.mapper.toEntity
import features.exercises.api.Exercise
import features.exercises.api.ExerciseCatalog
import features.workout.domain.WorkoutSession
import features.workout.domain.WorkoutSet
import features.workout.domain.WorkoutTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

internal class RoomWorkoutLocalDataSource(
    private val workoutDao: WorkoutDao,
    private val catalogue: ExerciseCatalog,
    private val routines: features.routines.api.RoutineRepository
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

    override suspend fun completeSession(sessionId: String, endTimeEpochMs: Long, totalVolumeKg: Double) =
        workoutDao.completeSession(sessionId, endTimeEpochMs, totalVolumeKg)

    override suspend fun deleteSessionWithSets(sessionId: String) {
        workoutDao.deleteSetsForSession(sessionId)
        workoutDao.deleteExercises(sessionId)
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

    override suspend fun routinePrescription(id: String): features.workout.domain.WorkoutStartPrescription {
        val routine=routines.getById(id) ?: throw NoSuchElementException("Routine not found")
        check(!routine.archived)
        require(features.routines.api.RoutineValidation.validate(routine)==null)
        return features.workout.domain.WorkoutStartPrescription(routine.name,routine.exercises.map { e ->
            val exercise=catalogue.getExerciseById(e.exerciseId) ?: throw NoSuchElementException("Exercise not found")
            check(!exercise.archived)
            features.workout.domain.WorkoutExercisePrescription(e.id,e.exerciseId,exercise.name,exercise.muscleGroup,e.restSeconds,
                e.sets.map { features.workout.domain.WorkoutSetPrescription(it.weightKg,it.reps,it.setType) })
        },routine.id,routine.revision)
    }
    @OptIn(kotlin.uuid.ExperimentalUuidApi::class)
    override suspend fun saveWorkoutAsRoutine(sessionId: String, name: String): WorkoutTemplate {
        val sets=getSetsForSessionSnapshot(sessionId)
        val routine=features.routines.api.Routine(kotlin.uuid.Uuid.random().toString(),name,"Vytvořeno z tréninku",exercises=
            sets.groupBy { it.workoutExerciseId ?: it.exerciseId }.values.map { entrySets ->
                val first=entrySets.first()
                features.routines.api.RoutineExercise(kotlin.uuid.Uuid.random().toString(),first.exerciseId,first.restSecondsSnapshot ?: 90,
                    entrySets.map { features.routines.api.RoutineSet(kotlin.uuid.Uuid.random().toString(),it.weightKg,it.reps,it.setType) })
            })
        return when(val result=routines.save(routine,null)) {
            is core.result.Result.Success -> project(result.data,catalogue.getAllExercises().associateBy { it.id })
            is core.result.Result.Error -> error("Routine save failed: ${result.error}")
        }
    }

    // Transitional UI projection only: routines own all persisted template data.
    override fun observeTemplates() = combine(routines.observeAll(),catalogue.observeExercises()) { routines,exercises ->
        val byId=exercises.associateBy { it.id }
        routines.map { project(it,byId) }
    }
    override suspend fun getTemplatesSnapshot(): List<WorkoutTemplate> {
        val byId=catalogue.getAllExercises().associateBy { it.id }
        return routines.observeAll(true).first().map { project(it,byId) }
    }
    override suspend fun getTemplateById(templateId: String) = routines.getById(templateId)?.let {
        project(it,catalogue.getAllExercises().associateBy { exercise -> exercise.id })
    }
    override suspend fun getTemplatesCount() = routines.observeAll(true).first().size
    override suspend fun saveTemplate(template: WorkoutTemplate) {
        val routine=features.routines.api.Routine(template.id,template.name,template.description,exercises=template.exercises.mapIndexed { index,e ->
            features.routines.api.RoutineExercise("${template.id}:$index",e.exerciseId,catalogue.getExerciseById(e.exerciseId)?.defaultRestSeconds ?: 90,
                List(e.targetSets) { n -> features.routines.api.RoutineSet("${template.id}:$index:$n",e.defaultWeightKg,e.defaultReps) })
        })
        when(val result=routines.save(routine,null)) {
            is core.result.Result.Success -> Unit
            is core.result.Result.Error -> error("Routine save failed: ${result.error}")
        }
    }
    override suspend fun saveTemplates(templates: List<WorkoutTemplate>) { templates.forEach { saveTemplate(it) } }
    private fun project(routine: features.routines.api.Routine, catalogueById: Map<String,Exercise>) = WorkoutTemplate(routine.id,routine.name,routine.notes,
        routine.exercises.mapIndexed { index,e -> features.workout.domain.WorkoutTemplateExercise(e.exerciseId,
            catalogueById[e.exerciseId]?.name ?: "Neznámý cvik",e.sets.size,e.sets.firstOrNull()?.weightKg ?: 0.0,
            e.sets.firstOrNull()?.reps ?: 0,index) })
}
