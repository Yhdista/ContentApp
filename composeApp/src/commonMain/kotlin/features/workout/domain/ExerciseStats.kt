package features.workout.domain

data class ExerciseHistorySessionEntry(
    val sessionId: String,
    val sessionTitle: String,
    val dateEpochMs: Long,
    val sets: List<WorkoutSet>,
    val maxWeightKg: Double,
    val max1RMKg: Double,
    val totalVolumeKg: Double
) {
    fun formattedDate(): String {
        return if (dateEpochMs <= 0L) "" else {
            val totalSeconds = dateEpochMs / 1000
            val days = (totalSeconds / 86400).toInt()
            val remSec = (totalSeconds % 86400).toInt()
            val hours = (remSec / 3600).toString().padStart(2, '0')
            val minutes = ((remSec % 3600) / 60).toString().padStart(2, '0')
            "Den $days, $hours:$minutes"
        }
    }
}

data class ExerciseStats(
    val exercise: Exercise,
    val personalBestWeightKg: Double,
    val personalBestReps: Int,
    val estimated1RMKg: Double,
    val totalVolumeKg: Double,
    val totalCompletedSets: Int,
    val sessionsHistory: List<ExerciseHistorySessionEntry>
)
