package features.workout.domain

data class DayActivity(
    val dayOfWeekLetter: String, // "P", "Ú", "S", "Č", "P", "S", "N"
    val dayOfWeekName: String,   // "Po", "Út", "St", "Čt", "Pá", "So", "Ne"
    val dayNumber: Long,         // epochMs / 86_400_000L
    val isToday: Boolean,
    val hasWorkout: Boolean,
    val workoutCount: Int
)

data class WeeklyActivitySummary(
    val days: List<DayActivity>, // Vždy 7 dnů: Pondělí až Neděle aktuálního týdne
    val thisWeekWorkoutsCount: Int,
    val currentStreakDays: Int
)

object WeeklyActivityCalculator {

    private const val DAY_MS = 86_400_000L

    /**
     * Vypočítá týdenní přehled aktivity a aktuální streak tréninkových dnů.
     * Týden začíná v pondělí (ISO-8601).
     * Podporuje posun časového pásma [timeZoneOffsetMs] pro přesné přiřazení k lokálnímu kalendářnímu dni.
     */
    fun calculateWeeklyActivity(
        nowEpochMs: Long,
        sessions: List<WorkoutSession>,
        timeZoneOffsetMs: Long = 0L
    ): WeeklyActivitySummary {
        val localNowMs = nowEpochMs + timeZoneOffsetMs
        val currentDayIndex = localNowMs / DAY_MS
        // 1970-01-01 byl čtvrtek (hodnota 4 v cyklu 1..7: Po=1, Út=2, St=3, Čt=4, Pá=5, So=6, Ne=7)
        val dayOfWeekIso = (((currentDayIndex + 3) % 7 + 7) % 7 + 1).toInt()
        val mondayDayIndex = currentDayIndex - (dayOfWeekIso - 1)

        val dayNames = listOf(
            "Po" to "P",
            "Út" to "Ú",
            "St" to "S",
            "Čt" to "Č",
            "Pá" to "P",
            "So" to "S",
            "Ne" to "N"
        )

        // Seskupení dokončených tréninků podle lokálního dne
        val completedSessions = sessions.filter { it.isCompleted }
        val sessionDays = completedSessions.groupBy { (it.startTimeEpochMs + timeZoneOffsetMs) / DAY_MS }

        val days = (0..6).map { offset ->
            val dayIdx = mondayDayIndex + offset
            val (shortName, letter) = dayNames[offset]
            val daySessions = sessionDays[dayIdx].orEmpty()
            DayActivity(
                dayOfWeekLetter = letter,
                dayOfWeekName = shortName,
                dayNumber = dayIdx,
                isToday = dayIdx == currentDayIndex,
                hasWorkout = daySessions.isNotEmpty(),
                workoutCount = daySessions.size
            )
        }

        val thisWeekCount = days.sumOf { it.workoutCount }

        // Výpočet po sobě jdoucích dnů (streak):
        // Pokud dnes byl trénink, počítáme od dneška. Pokud dnes ještě nebyl, ale včera ano, streak trvá od včerejška.
        var streakDays = 0
        var checkDay = if (sessionDays[currentDayIndex]?.isNotEmpty() == true) currentDayIndex else currentDayIndex - 1
        while (sessionDays[checkDay]?.isNotEmpty() == true) {
            streakDays++
            checkDay--
        }

        return WeeklyActivitySummary(
            days = days,
            thisWeekWorkoutsCount = thisWeekCount,
            currentStreakDays = streakDays
        )
    }
}
