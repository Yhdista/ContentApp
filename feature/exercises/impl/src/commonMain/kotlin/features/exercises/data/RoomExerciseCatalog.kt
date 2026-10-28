package features.exercises.data

import core.storage.CommandCoordinator
import core.common.AppClock
import core.database.MutationDao
import core.database.SyncMutationEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import features.exercises.api.Exercise
import features.exercises.api.ExerciseCatalog
import features.exercises.api.ExerciseCatalogImport
import features.exercises.api.MuscleGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.module.Module
import org.koin.dsl.module

@OptIn(ExperimentalUuidApi::class)
internal class RoomExerciseCatalog(
    private val dao: ExerciseDao,
    private val writes: CommandCoordinator,
    private val outbox: MutationDao,
    private val clock: AppClock
) : ExerciseCatalog, ExerciseCatalogImport {
    private val json = Json { encodeDefaults = true }
    override fun observeExercises(): Flow<List<Exercise>> = dao.getAllExercises().map { list -> list.map { it.toDomain() } }
    override suspend fun getAllExercises(): List<Exercise> = dao.getAllExercisesList().map { it.toDomain() }
    override suspend fun getExerciseById(id: String): Exercise? = dao.getExerciseById(id)?.toDomain()
    override suspend fun getExercisesCount(): Int = dao.getExercisesCount()
    override suspend fun create(name: String, muscleGroup: MuscleGroup, defaultRestSeconds: Int): Exercise = writes.write {
        val exercise = validated(Exercise("custom-ex-${Uuid.random()}", name, muscleGroup, defaultRestSeconds))
        dao.insertExercise(exercise.toEntity())
        enqueue(exercise, "INSERT")
        exercise
    }
    override suspend fun update(exercise: Exercise): Exercise = writes.write {
        val updated = validated(exercise)
        dao.getExerciseById(updated.id) ?: throw NoSuchElementException("Exercise not found")
        dao.insertExercise(updated.toEntity())
        enqueue(updated, "UPDATE")
        updated
    }
    override suspend fun archive(id: String) = writes.write {
        val exercise = dao.getExerciseById(id)?.toDomain() ?: throw NoSuchElementException("Exercise not found")
        if (!exercise.archived) {
            val archived = exercise.copy(archived = true)
            dao.insertExercise(archived.toEntity())
            enqueue(archived, "UPDATE")
        }
    }
    override suspend fun importExercises(exercises: List<Exercise>) = writes.write {
        dao.insertExercises(exercises.map { validated(it).toEntity() })
    }
    override suspend fun ensureDefaults() = writes.write {
        if (dao.getExercisesCount() == 0) dao.insertExercises(ExerciseDefaults.exercises.map { it.toEntity() })
    }
    private fun validated(exercise: Exercise): Exercise {
        require(exercise.id.isNotBlank() && exercise.name.trim().isNotEmpty() && exercise.defaultRestSeconds > 0)
        return exercise.copy(name = exercise.name.trim())
    }
    private suspend fun enqueue(exercise: Exercise, operation: String) {
        outbox.insertMutation(SyncMutationEntity(Uuid.random().toString(), "exercise", exercise.id, operation,
            json.encodeToString(exercise), clock.nowMillis()))
    }

}

fun exerciseFeatureModule(): Module = module {
    single { RoomExerciseCatalog(get(), get(), get(), get()) }
    single<ExerciseCatalog> { get<RoomExerciseCatalog>() }
    single<ExerciseCatalogImport> { get<RoomExerciseCatalog>() }
}
