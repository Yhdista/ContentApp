package features.workout.domain

import features.exercises.api.Exercise
object ExerciseStatsCalculator {

    fun calculate(
        exercise: Exercise,
        sessions: List<WorkoutSession>,
        sets: List<WorkoutSet>
    ): ExerciseStats {
        val completedSets = sets.filter { it.isCompleted && it.exerciseId == exercise.id }
        if (completedSets.isEmpty()) {
            return ExerciseStats(
                exercise = exercise,
                personalBestWeightKg = 0.0,
                personalBestReps = 0,
                estimated1RMKg = 0.0,
                totalVolumeKg = 0.0,
                totalCompletedSets = 0,
                sessionsHistory = emptyList()
            )
        }

        val sessionMap = sessions.associateBy { it.id }

        // Group sets by sessionId
        val groupedSets = completedSets.groupBy { it.sessionId }

        val sessionEntries = groupedSets.map { (sessionId, sessionSets) ->
            val session = sessionMap[sessionId]
            val date = session?.startTimeEpochMs ?: 0L
            val title = session?.title ?: "Trénink"
            val sortedSets = sessionSets.sortedBy { it.setNumber }

            val maxWeightSet = sortedSets.maxByOrNull { it.weightKg }
            val maxWeight = maxWeightSet?.weightKg ?: 0.0

            val max1RM = sortedSets.maxOfOrNull {
                PersonalRecordCalculator.calculateOneRepMax(it.weightKg, it.reps)
            } ?: 0.0

            val vol = sortedSets.sumOf { it.weightKg * it.reps }

            ExerciseHistorySessionEntry(
                sessionId = sessionId,
                sessionTitle = title,
                dateEpochMs = date,
                sets = sortedSets,
                maxWeightKg = maxWeight,
                max1RMKg = max1RM,
                totalVolumeKg = vol
            )
        }.sortedByDescending { it.dateEpochMs }

        // All-time bests
        val bestSetBy1RM = completedSets.maxByOrNull {
            PersonalRecordCalculator.calculateOneRepMax(it.weightKg, it.reps)
        }
        val best1RM = bestSetBy1RM?.let {
            PersonalRecordCalculator.calculateOneRepMax(it.weightKg, it.reps)
        } ?: 0.0

        val bestWeightSet = completedSets.maxByOrNull { it.weightKg }
        val pbWeight = bestWeightSet?.weightKg ?: 0.0
        val pbReps = bestWeightSet?.reps ?: 0

        val totalLifetimeVolume = completedSets.sumOf { it.weightKg * it.reps }

        return ExerciseStats(
            exercise = exercise,
            personalBestWeightKg = pbWeight,
            personalBestReps = pbReps,
            estimated1RMKg = best1RM,
            totalVolumeKg = totalLifetimeVolume,
            totalCompletedSets = completedSets.size,
            sessionsHistory = sessionEntries
        )
    }
}
