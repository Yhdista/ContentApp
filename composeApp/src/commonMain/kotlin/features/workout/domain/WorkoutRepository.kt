package features.workout.domain

import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    fun getExercises(): Flow<List<Exercise>>
    suspend fun ensureDefaultExercises()
    fun getActiveSession(): Flow<WorkoutSession?>
    fun getCompletedSessions(): Flow<List<WorkoutSession>>
    suspend fun getSessionById(sessionId: String): WorkoutSession?
    fun getSetsForSession(sessionId: String): Flow<List<WorkoutSet>>
    suspend fun startWorkout(title: String = "Trénink"): WorkoutSession
    suspend fun addSet(sessionId: String, exerciseId: String, weightKg: Double, reps: Int, rpe: Double? = null): WorkoutSet
    suspend fun updateSet(set: WorkoutSet)
    suspend fun deleteSet(setId: String)
    suspend fun completeWorkout(sessionId: String): WorkoutSession?
    suspend fun cancelWorkout(sessionId: String)
    suspend fun getLastPerformance(exerciseId: String): String?
}
