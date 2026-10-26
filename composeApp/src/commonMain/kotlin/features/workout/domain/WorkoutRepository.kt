package features.workout.domain

import kotlinx.coroutines.flow.Flow

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
    suspend fun updateSet(set: WorkoutSet)
    suspend fun deleteSet(setId: String)
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
    suspend fun exportWorkoutDataCsv(): String
    suspend fun exportWorkoutDataJson(): String
}
