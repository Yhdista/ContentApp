package features.workout.domain

import features.exercises.api.MuscleGroup
import features.exercises.api.Exercise
import kotlinx.coroutines.flow.Flow

/**
 * Local storage is the source of truth. Commands commit data and outbox atomically.
 * Accepted commands survive caller cancellation, but do not survive process termination before commit.
 * Storage/missing-record exceptions propagate; presentation maps them with safeLocalCall.
 * Edit callbacks must be pure and cannot change record identity.
 */
interface WorkoutRepository {
    fun getExercises(): Flow<List<Exercise>>
    suspend fun ensureDefaultExercises()
    suspend fun createCustomExercise(name: String, muscleGroup: MuscleGroup, defaultRestSeconds: Int = 90): Exercise
    fun getActiveSession(): Flow<WorkoutSession?>
    fun getCompletedSessions(): Flow<List<WorkoutSession>>
    fun getPendingSyncMutationsCount(): Flow<Int>
    suspend fun getSessionById(sessionId: String): WorkoutSession?
    fun getSetsForSession(sessionId: String): Flow<List<WorkoutSet>>
    suspend fun startWorkout(title: String = "Trénink"): WorkoutSession
    suspend fun addSet(sessionId: String, exerciseId: String, weightKg: Double, reps: Int, rpe: Double? = null): WorkoutSet
    /** Applies an edit to the latest persisted value in the command transaction. */
    suspend fun updateSet(setId: String, update: (WorkoutSet) -> WorkoutSet): WorkoutSet
    suspend fun deleteSet(setId: String)
    suspend fun removeExerciseFromWorkout(sessionId: String, exerciseId: String)
    suspend fun completeWorkout(sessionId: String): WorkoutSession?
    suspend fun cancelWorkout(sessionId: String)
    suspend fun getLastPerformance(exerciseId: String): String?
    suspend fun getHistoricalMax1RM(exerciseId: String, currentSessionId: String): Double
    fun getTemplates(): Flow<List<WorkoutTemplate>>
    suspend fun ensureDefaultTemplates()
    suspend fun createTemplate(name: String, description: String, exercises: List<WorkoutTemplateExercise>): WorkoutTemplate
    suspend fun startWorkoutFromTemplate(templateId: String): WorkoutSession
    suspend fun saveSessionAsTemplate(sessionId: String, templateName: String): WorkoutTemplate
    fun getExerciseStats(exerciseId: String): Flow<ExerciseStats?>
    suspend fun getAllCompletedSets(): List<WorkoutSet>
}
