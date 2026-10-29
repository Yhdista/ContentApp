package features.workout.domain

import features.exercises.api.Exercise
import kotlinx.coroutines.flow.Flow

/**
 * Local storage is the source of truth. Commands commit data and outbox atomically.
 * Accepted commands survive caller cancellation, but do not survive process termination before commit.
 * Storage/missing-record exceptions propagate; presentation maps them with safeLocalCall.
 * Edit callbacks must be pure and cannot change record identity.
 * Completion is exclusively owned by the app-provided [CompleteWorkout] workflow, including plan progress.
 */
interface WorkoutRepository {
    fun getExercises(): Flow<List<Exercise>>
    fun getActiveSession(): Flow<WorkoutSession?>
    fun getCompletedSessions(): Flow<List<WorkoutSession>>
    fun getPendingSyncMutationsCount(): Flow<Int>
    suspend fun getSessionById(sessionId: String): WorkoutSession?
    /** Actual rows only; omitted targets never contribute to performance or conversion. */
    fun getSetsForSession(sessionId: String): Flow<List<WorkoutSet>>
    suspend fun startWorkout(title: String = "Trénink"): WorkoutSession
    /** Complete immutable original targets, independent of editable actual rows. */
    fun getPrescriptionForSession(sessionId: String): Flow<List<WorkoutPrescriptionSet>>
    fun getWorkoutExercises(sessionId: String): Flow<List<WorkoutExercise>>
    /** Creates a fresh occurrence and its first actual set in one accepted command, including both outbox rows. */
    suspend fun addExerciseWithInitialSet(sessionId: String, exerciseId: String, weightKg: Double, reps: Int): WorkoutExercise
    /** Entry-specific commands reject stale IDs instead of treating them as catalogue IDs. */
    suspend fun addSetToEntry(sessionId: String, workoutExerciseId: String, weightKg: Double, reps: Int, rpe: Double? = null): WorkoutSet
    suspend fun removeWorkoutExercise(sessionId: String, workoutExerciseId: String)
    /** Applies an edit to the latest persisted value in the command transaction. */
    suspend fun updateSet(setId: String, update: (WorkoutSet) -> WorkoutSet): WorkoutSet
    suspend fun deleteSet(setId: String)
    suspend fun cancelWorkout(sessionId: String)
    /** Last completed set in the latest completed session, ordered by session time and occurrence/set order. */
    suspend fun getLastPerformance(exerciseId: String): String?
    suspend fun getHistoricalMax1RM(exerciseId: String, currentSessionId: String): Double
    fun getExerciseStats(exerciseId: String): Flow<ExerciseStats?>
    suspend fun getAllCompletedSets(): List<WorkoutSet>
}
