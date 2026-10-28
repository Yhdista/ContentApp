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
) : ExerciseCatalog {
    override fun observeExercises(): Flow<List<Exercise>> = dao.getAllExercises().map { list -> list.map { it.toDomain() } }
    override suspend fun getAllExercises(): List<Exercise> = dao.getAllExercisesList().map { it.toDomain() }
    override suspend fun getExerciseById(id: String): Exercise? = dao.getExerciseById(id)?.toDomain()
    override suspend fun getExercisesCount(): Int = dao.getExercisesCount()
    override suspend fun saveExercise(exercise: Exercise) = writes.write { dao.insertExercise(exercise.toEntity()) }
    override suspend fun archive(id: String) = writes.write {
        val exercise=dao.getExerciseById(id) ?: throw NoSuchElementException("Exercise not found")
        if (!exercise.archived) {
            val archived = exercise.copy(archived = true)
            dao.insertExercise(archived)
            outbox.insertMutation(SyncMutationEntity(Uuid.random().toString(), "exercise", id, "UPDATE",
                Json.encodeToString(archived.toDomain()), clock.nowMillis()))
        }
    }
    override suspend fun saveExercises(exercises: List<Exercise>) = writes.write { dao.insertExercises(exercises.map { it.toEntity() }) }
}

fun exerciseFeatureModule(): Module = module {
    single<ExerciseCatalog> { RoomExerciseCatalog(get(), get(), get(), get()) }
}
