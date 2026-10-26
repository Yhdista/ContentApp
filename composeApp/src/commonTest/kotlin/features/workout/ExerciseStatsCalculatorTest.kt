package features.workout

import features.workout.domain.Exercise
import features.workout.domain.ExerciseStatsCalculator
import features.workout.domain.MuscleGroup
import features.workout.domain.WorkoutSession
import features.workout.domain.WorkoutSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExerciseStatsCalculatorTest {

    private val benchExercise = Exercise(
        id = "ex_bench",
        name = "Bench press",
        muscleGroup = MuscleGroup.CHEST,
        defaultRestSeconds = 120
    )

    private val squatExercise = Exercise(
        id = "ex_squat",
        name = "Dřep",
        muscleGroup = MuscleGroup.LEGS,
        defaultRestSeconds = 180
    )

    @Test
    fun testCalculateWithEmptySetsReturnsZeroStats() {
        val stats = ExerciseStatsCalculator.calculate(
            exercise = benchExercise,
            sessions = emptyList(),
            sets = emptyList()
        )

        assertEquals(benchExercise, stats.exercise)
        assertEquals(0.0, stats.personalBestWeightKg)
        assertEquals(0, stats.personalBestReps)
        assertEquals(0.0, stats.estimated1RMKg)
        assertEquals(0.0, stats.totalVolumeKg)
        assertEquals(0, stats.totalCompletedSets)
        assertTrue(stats.sessionsHistory.isEmpty())
    }

    @Test
    fun testCalculateWithMultipleSessionsComputesPBsAndVolume() {
        val session1 = WorkoutSession(
            id = "s1",
            title = "Push 1",
            startTimeEpochMs = 100000L,
            isCompleted = true
        )
        val session2 = WorkoutSession(
            id = "s2",
            title = "Push 2",
            startTimeEpochMs = 200000L,
            isCompleted = true
        )

        val sets = listOf(
            WorkoutSet("st1", "s1", "ex_bench", 1, 80.0, 8, isCompleted = true),
            WorkoutSet("st2", "s1", "ex_bench", 2, 80.0, 8, isCompleted = true),
            WorkoutSet("st3", "s2", "ex_bench", 1, 85.0, 5, isCompleted = true),
            WorkoutSet("st4", "s2", "ex_bench", 2, 90.0, 3, isCompleted = true),
            // Uncompleted set should be ignored
            WorkoutSet("st5", "s2", "ex_bench", 3, 95.0, 1, isCompleted = false),
            // Another exercise should be ignored
            WorkoutSet("st6", "s2", "ex_squat", 1, 100.0, 5, isCompleted = true)
        )

        val stats = ExerciseStatsCalculator.calculate(
            exercise = benchExercise,
            sessions = listOf(session1, session2),
            sets = sets
        )

        assertEquals(90.0, stats.personalBestWeightKg)
        assertEquals(3, stats.personalBestReps)
        // 80 * (1 + 8/30) = 101.333 vs 90 * (1 + 3/30) = 99.0
        assertTrue(stats.estimated1RMKg > 101.3 && stats.estimated1RMKg < 101.4)
        // Volume: 80*8 + 80*8 + 85*5 + 90*3 = 640 + 640 + 425 + 270 = 1975.0
        assertEquals(1975.0, stats.totalVolumeKg)
        assertEquals(4, stats.totalCompletedSets)

        // Session history ordered descending by date: session2 (200000L) then session1 (100000L)
        assertEquals(2, stats.sessionsHistory.size)
        assertEquals("s2", stats.sessionsHistory[0].sessionId)
        assertEquals("Push 2", stats.sessionsHistory[0].sessionTitle)
        assertEquals(90.0, stats.sessionsHistory[0].maxWeightKg)
        assertEquals(2, stats.sessionsHistory[0].sets.size)

        assertEquals("s1", stats.sessionsHistory[1].sessionId)
        assertEquals("Push 1", stats.sessionsHistory[1].sessionTitle)
        assertEquals(80.0, stats.sessionsHistory[1].maxWeightKg)
    }
}
