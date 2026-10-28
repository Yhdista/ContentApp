package features.exercises.api

import kotlinx.coroutines.flow.Flow

/** User commands own validation, identity and granular outbox records in one accepted transaction.
 * Accepted commands survive cancellation of the caller. Reads include archived records for history.
 */
interface ExerciseCatalog {
    fun observeExercises(): Flow<List<Exercise>>
    suspend fun getAllExercises(): List<Exercise>
    suspend fun getExerciseById(id: String): Exercise?
    suspend fun getExercisesCount(): Int
    suspend fun create(name: String, muscleGroup: MuscleGroup, defaultRestSeconds: Int = 90): Exercise
    suspend fun update(exercise: Exercise): Exercise
    suspend fun archive(id: String)
}

/** Composition/sync integration, separate from user commands. Imported remote/bundled records keep IDs
 * and do not echo to the user outbox. Bundled defaults initialize only an entirely empty catalogue.
 */
interface ExerciseCatalogImport {
    suspend fun importExercises(exercises: List<Exercise>)
    suspend fun ensureDefaults()
}
