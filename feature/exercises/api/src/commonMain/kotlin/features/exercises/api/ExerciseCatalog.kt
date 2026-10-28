package features.exercises.api

import kotlinx.coroutines.flow.Flow

/** Catalogue records retain their stable IDs. Writes join the application's command transaction.
 * Save callers own any associated outbox record; archive emits its own update atomically.
 */
interface ExerciseCatalog {
    fun observeExercises(): Flow<List<Exercise>>
    suspend fun getAllExercises(): List<Exercise>
    suspend fun getExerciseById(id: String): Exercise?
    suspend fun getExercisesCount(): Int
    suspend fun saveExercise(exercise: Exercise)
    suspend fun archive(id: String) {
        val exercise = getExerciseById(id) ?: throw NoSuchElementException("Exercise not found")
        saveExercise(exercise.copy(archived = true))
    }
    suspend fun saveExercises(exercises: List<Exercise>)
}
