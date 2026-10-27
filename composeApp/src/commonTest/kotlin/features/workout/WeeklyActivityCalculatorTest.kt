package features.workout

import features.workout.domain.WeeklyActivityCalculator
import features.workout.domain.WorkoutSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WeeklyActivityCalculatorTest {

    private val dayMs = 86_400_000L
    // Referenční den: Středa (např. 1700000000000 ms -> 1700000000000 / 86400000 = 19675)
    // 19675: (19675 + 3) % 7 + 1 = 19678 % 7 + 1 = 6 + 1 = 7 (Neděle)
    // Zvolme pevné dny:
    // Pondělí = 1000 * 7 * dayMs + 4 * dayMs = den index 7004
    // (7004 + 3) % 7 + 1 = 7007 % 7 + 1 = 0 + 1 = 1 (Pondělí)
    private val mondayDayIdx = 7004L
    private val mondayMs = mondayDayIdx * dayMs
    private val tuesdayMs = (mondayDayIdx + 1) * dayMs
    private val wednesdayMs = (mondayDayIdx + 2) * dayMs

    @Test
    fun testWeeklyActivityBuildsSevenDaysMondayToSunday() {
        val summary = WeeklyActivityCalculator.calculateWeeklyActivity(
            nowEpochMs = wednesdayMs,
            sessions = emptyList()
        )

        assertEquals(7, summary.days.size)
        assertEquals("P", summary.days[0].dayOfWeekLetter)
        assertEquals("Po", summary.days[0].dayOfWeekName)
        assertEquals("Ú", summary.days[1].dayOfWeekLetter)
        assertEquals("S", summary.days[2].dayOfWeekLetter)
        assertEquals("Č", summary.days[3].dayOfWeekLetter)
        assertEquals("P", summary.days[4].dayOfWeekLetter)
        assertEquals("S", summary.days[5].dayOfWeekLetter)
        assertEquals("N", summary.days[6].dayOfWeekLetter)
        assertEquals(0, summary.thisWeekWorkoutsCount)
        assertEquals(0, summary.currentStreakDays)
    }

    @Test
    fun testWeeklyActivityIdentifiesToday() {
        val summary = WeeklyActivityCalculator.calculateWeeklyActivity(
            nowEpochMs = wednesdayMs, // Středa = index 2 v poli (0=Po, 1=Út, 2=St)
            sessions = emptyList()
        )

        assertFalse(summary.days[0].isToday)
        assertFalse(summary.days[1].isToday)
        assertTrue(summary.days[2].isToday)
        assertFalse(summary.days[3].isToday)
    }

    @Test
    fun testWeeklyActivityCountsWorkoutsAndCalculatesStreak() {
        val sessionMon = WorkoutSession("s1", "Pondělí", mondayMs, mondayMs + 3600000, isCompleted = true)
        val sessionTue = WorkoutSession("s2", "Úterý", tuesdayMs, tuesdayMs + 3600000, isCompleted = true)
        val sessionWed = WorkoutSession("s3", "Středa", wednesdayMs, wednesdayMs + 3600000, isCompleted = true)

        val summary = WeeklyActivityCalculator.calculateWeeklyActivity(
            nowEpochMs = wednesdayMs,
            sessions = listOf(sessionMon, sessionTue, sessionWed)
        )

        assertEquals(3, summary.thisWeekWorkoutsCount)
        assertEquals(3, summary.currentStreakDays)
        assertTrue(summary.days[0].hasWorkout)
        assertTrue(summary.days[1].hasWorkout)
        assertTrue(summary.days[2].hasWorkout)
        assertFalse(summary.days[3].hasWorkout)
    }

    @Test
    fun testStreakMaintainedWhenTodayNotYetWorkedOut() {
        // Dnes je středa, ale trénink byl v pondělí a úterý. Streak je 2 dny (protože den ještě neskončil).
        val sessionMon = WorkoutSession("s1", "Pondělí", mondayMs, mondayMs + 3600000, isCompleted = true)
        val sessionTue = WorkoutSession("s2", "Úterý", tuesdayMs, tuesdayMs + 3600000, isCompleted = true)

        val summary = WeeklyActivityCalculator.calculateWeeklyActivity(
            nowEpochMs = wednesdayMs,
            sessions = listOf(sessionMon, sessionTue)
        )

        assertEquals(2, summary.thisWeekWorkoutsCount)
        assertEquals(2, summary.currentStreakDays)
        assertTrue(summary.days[0].hasWorkout)
        assertTrue(summary.days[1].hasWorkout)
        assertFalse(summary.days[2].hasWorkout)
    }
}
