package features.workout.data.datasource

import features.exercises.api.Exercise
import features.workout.domain.WorkoutSession
import features.workout.domain.WorkoutSet
import kotlinx.coroutines.flow.Flow

/**
 * Hluboký modul lokálního zdroje dat (Deep Module).
 *
 * Skrývá detaily Room databáze, SQL dotazů, N+1 relací šablon i mapování `*Entity` <-> Domain.
 * Navenek komunikuje výhradně pomocí čistých doménových modelů.
 */
internal interface WorkoutLocalDataSource {
    // Exercises
    fun observeExercises(): Flow<List<Exercise>>
    suspend fun getAllExercises(): List<Exercise>
    suspend fun getExerciseById(id: String): Exercise?
    suspend fun getExercisesCount(): Int
    suspend fun saveExercise(exercise: Exercise)
    suspend fun saveExercises(exercises: List<Exercise>)

    // Sessions
    fun observeActiveSession(): Flow<WorkoutSession?>
    suspend fun getActiveSession(): WorkoutSession?
    fun observeCompletedSessions(): Flow<List<WorkoutSession>>
    suspend fun getAllSessions(): List<WorkoutSession>
    suspend fun getSessionById(sessionId: String): WorkoutSession?
    suspend fun saveSession(session: WorkoutSession)
    suspend fun deleteSessionWithSets(sessionId: String)

    fun observePrescription(sessionId: String): Flow<List<features.workout.domain.WorkoutPrescriptionSet>>
    suspend fun getAllPrescriptions(): List<features.workout.domain.WorkoutPrescriptionSet>
    fun observeWorkoutExercises(sessionId: String): Flow<List<features.workout.domain.WorkoutExercise>>
    suspend fun getWorkoutExercises(sessionId: String): List<features.workout.domain.WorkoutExercise>
    suspend fun saveWorkoutExercise(entry: features.workout.domain.WorkoutExercise)
    suspend fun deleteWorkoutExercise(id: String)
    // Sets
    fun observeSetsForSession(sessionId: String): Flow<List<WorkoutSet>>
    suspend fun getSetById(setId: String): WorkoutSet?
    suspend fun getSetsForSessionSnapshot(sessionId: String): List<WorkoutSet>
    suspend fun saveSet(set: WorkoutSet)
    suspend fun deleteSet(setId: String)
    suspend fun getLastCompletedSetForExercise(exerciseId: String): WorkoutSet?
    suspend fun getHistoricalCompletedSets(exerciseId: String, currentSessionId: String): List<WorkoutSet>
    fun observeCompletedSetsForExercise(exerciseId: String): Flow<List<WorkoutSet>>
    suspend fun getAllCompletedSets(): List<WorkoutSet>
    suspend fun getAllSets(): List<WorkoutSet>
    suspend fun getAllWorkoutExercises(): List<features.workout.domain.WorkoutExercise>

}
