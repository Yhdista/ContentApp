package features.exercises.data

import core.storage.CommandCoordinator
import features.exercises.api.Exercise
import features.exercises.api.ExerciseCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.module.Module
import org.koin.dsl.module

internal class RoomExerciseCatalog(
    private val dao: ExerciseDao,
    private val writes: CommandCoordinator
) : ExerciseCatalog {
    override fun observeExercises(): Flow<List<Exercise>> = dao.getAllExercises().map { list -> list.map { it.toDomain() } }
    override suspend fun getAllExercises(): List<Exercise> = dao.getAllExercisesList().map { it.toDomain() }
    override suspend fun getExerciseById(id: String): Exercise? = dao.getExerciseById(id)?.toDomain()
    override suspend fun getExercisesCount(): Int = dao.getExercisesCount()
    override suspend fun saveExercise(exercise: Exercise) = writes.write { dao.insertExercise(exercise.toEntity()) }
    override suspend fun saveExercises(exercises: List<Exercise>) = writes.write { dao.insertExercises(exercises.map { it.toEntity() }) }
}

fun exerciseFeatureModule(): Module = module {
    single<ExerciseCatalog> { RoomExerciseCatalog(get(), get()) }
}
