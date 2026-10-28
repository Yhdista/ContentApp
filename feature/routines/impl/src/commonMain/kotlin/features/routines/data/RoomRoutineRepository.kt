package features.routines.data

import core.common.AppClock
import core.database.MutationDao
import core.database.SyncMutationEntity
import core.result.Result
import core.storage.CommandCoordinator
import features.exercises.api.ExerciseCatalog
import features.routines.api.Routine
import features.routines.api.RoutineError
import features.routines.api.RoutineRepository
import features.routines.api.RoutineValidation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
internal class RoomRoutineRepository(
    private val dao: RoutineDao,
    private val catalogue: ExerciseCatalog,
    private val writes: CommandCoordinator,
    private val outbox: MutationDao,
    private val clock: AppClock
) : RoutineRepository {
    override fun observeAll(includeArchived: Boolean): Flow<List<Routine>> =
        dao.observeAll(includeArchived).map { snapshots -> snapshots.map { it.toDomain() } }
    override fun observeById(id: String): Flow<Routine?> = dao.observeById(id).map { it?.toDomain() }
    override suspend fun getById(id: String): Routine? = dao.getById(id)?.toDomain()

    override suspend fun save(routine: Routine, expectedRevision: Long?): Result<Routine, RoutineError> = command {
        saveAggregate(routine, expectedRevision)
    }

    private suspend fun saveAggregate(routine: Routine, expectedRevision: Long?): Result<Routine, RoutineError> {
        RoutineValidation.validate(routine)?.let { return Result.Error(it) }
        val previous = dao.getById(routine.id)?.toDomain()
        if (routine.archived || previous?.archived == true) return Result.Error(RoutineError.ARCHIVED)
        if (previous == null && expectedRevision != null) return Result.Error(RoutineError.NOT_FOUND)
        if (previous != null && (expectedRevision == null || expectedRevision != previous.revision)) {
            return Result.Error(RoutineError.REVISION_CONFLICT)
        }
        val previousEntries = previous?.exercises.orEmpty().associateBy { it.id }
        // Look up each catalogue ID once, while holding the same writer transaction.
        for (exerciseId in routine.exercises.map { it.exerciseId }.distinct()) {
            val exercise = catalogue.getExerciseById(exerciseId) ?: return Result.Error(RoutineError.NOT_FOUND)
            if (exercise.archived && routine.exercises.any { entry ->
                    entry.exerciseId == exerciseId && previousEntries[entry.id]?.exerciseId != entry.exerciseId
                }) return Result.Error(RoutineError.ARCHIVED)
        }
        val saved = routine.copy(name = routine.name.trim(), revision = (previous?.revision ?: 0) + 1, archived = false)
        if (previous == null) {
            dao.insertRoutine(RoutineEntity(saved.id, saved.name, saved.notes, saved.revision, false))
        } else {
            if (dao.updateRoutine(saved.id, saved.name, saved.notes, previous.revision) != 1) {
                return Result.Error(RoutineError.REVISION_CONFLICT)
            }
            dao.deleteExercises(saved.id)
        }
        dao.insertExercises(saved.exercises.mapIndexed { index, entry ->
            RoutineExerciseEntity(entry.id, saved.id, entry.exerciseId, entry.restSeconds, index,
                entry.legacyValues?.exerciseName,entry.legacyValues?.targetSets,entry.legacyValues?.defaultWeightKg,entry.legacyValues?.defaultReps)
        })
        dao.insertSets(saved.exercises.flatMap { entry -> entry.sets.mapIndexed { index, set ->
            RoutineSetEntity(set.id, entry.id, set.weightKg, set.reps, set.setType.name, index)
        } })
        enqueueAggregate(saved, previous)
        return Result.Success(saved)
    }

    override suspend fun duplicate(id: String): Result<Routine, RoutineError> = command {
        val source = dao.getById(id)?.toDomain() ?: return@command Result.Error(RoutineError.NOT_FOUND)
        val copy = source.copy(id = newId(), revision = 0, archived = false, exercises = source.exercises.map { entry ->
            entry.copy(id = newId(), sets = entry.sets.map { it.copy(id = newId()) })
        })
        saveAggregate(copy, null)
    }

    override suspend fun archive(id: String, expectedRevision: Long): Result<Unit, RoutineError> = command {
        val previous = dao.getById(id)?.toDomain() ?: return@command Result.Error(RoutineError.NOT_FOUND)
        if (previous.archived) return@command Result.Error(RoutineError.ARCHIVED)
        if (previous.revision != expectedRevision) return@command Result.Error(RoutineError.REVISION_CONFLICT)
        if (dao.archive(id, expectedRevision) != 1) return@command Result.Error(RoutineError.REVISION_CONFLICT)
        enqueue("routine", id, "UPDATE", Json.encodeToString(previous.copy(revision = expectedRevision + 1, archived = true).payload()))
        Result.Success(Unit)
    }

    private suspend fun enqueueAggregate(saved: Routine, previous: Routine?) {
        val oldEntries = previous?.exercises.orEmpty().associateBy { it.id }
        val oldSets = previous?.exercises.orEmpty().flatMap { it.sets }.associateBy { it.id }
        val entryIds = saved.exercises.map { it.id }.toSet()
        val setIds = saved.exercises.flatMap { it.sets }.map { it.id }.toSet()
        for (id in oldSets.keys - setIds) enqueue("routine_set", id, "DELETE", Json.encodeToString(DeleteRoutineChildPayload(id)))
        for (id in oldEntries.keys - entryIds) enqueue("routine_exercise", id, "DELETE", Json.encodeToString(DeleteRoutineChildPayload(id)))
        enqueue("routine", saved.id, if (previous == null) "INSERT" else "UPDATE", Json.encodeToString(saved.payload()))
        saved.exercises.forEachIndexed { entryIndex, entry ->
            enqueue("routine_exercise", entry.id, if (entry.id in oldEntries) "UPDATE" else "INSERT",
                Json.encodeToString(RoutineExerciseSyncPayload(entry.id, saved.id, entry.exerciseId, entry.restSeconds, entryIndex, entry.legacyValues)))
            entry.sets.forEachIndexed { setIndex, set ->
                enqueue("routine_set", set.id, if (set.id in oldSets) "UPDATE" else "INSERT",
                    Json.encodeToString(RoutineSetSyncPayload(set.id, entry.id, set.weightKg, set.reps, set.setType, setIndex)))
            }
        }
    }

    private suspend fun enqueue(entity: String, id: String, operation: String, payload: String) {
        outbox.insertMutation(SyncMutationEntity(newId(), entity, id, operation, payload, clock.nowMillis()))
    }

    private suspend fun <T> command(block: suspend () -> Result<T, RoutineError>): Result<T, RoutineError> =
        try {
            writes.write { block() }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Result.Error(RoutineError.STORAGE)
        }

    private fun newId(): String = Uuid.random().toString()
}

fun routineFeatureModule(): Module = module {
    single<RoutineRepository> { RoomRoutineRepository(get(), get(), get(), get(), get()) }
}
