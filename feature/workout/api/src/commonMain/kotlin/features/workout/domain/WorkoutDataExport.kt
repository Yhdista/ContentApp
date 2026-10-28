package features.workout.domain

/** Export formatting is independent of the workout editing contract. */
interface WorkoutDataExport {
    suspend fun exportWorkoutDataCsv(): String
    suspend fun exportWorkoutDataJson(): String
}
