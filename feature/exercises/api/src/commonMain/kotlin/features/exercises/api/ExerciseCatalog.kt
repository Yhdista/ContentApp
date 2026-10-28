package features.exercises.api

import kotlinx.coroutines.flow.Flow

/** Catalogue records retain their stable IDs. Writes join the application's command transaction.
 * The command caller owns any associated outbox record. Archived persistence arrives with schema 7.
 */
interface ExerciseCatalog {
    fun observeExercises(): Flow<List<Exercise>>
    suspend fun getAllExercises(): List<Exercise>
    suspend fun getExerciseById(id: String): Exercise?
    suspend fun getExercisesCount(): Int
    suspend fun saveExercise(exercise: Exercise)
    suspend fun saveExercises(exercises: List<Exercise>)
}
