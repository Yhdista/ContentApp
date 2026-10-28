package features.workout

import core.common.formatCleanWeight
import features.exercises.api.Exercise
import features.exercises.api.MuscleGroup
import features.workout.domain.WorkoutDataExporter
import features.workout.domain.WorkoutSession
import features.workout.domain.WorkoutSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorkoutDataExporterTest {

    private val bench = Exercise("ex_bench", "Bench press", MuscleGroup.CHEST, defaultRestSeconds = 120)

    @Test
    fun testFormatCleanWeightRemovesTrailingZeroOnly() {
        assertEquals("80", 80.0.formatCleanWeight())
        assertEquals("82.5", 82.5.formatCleanWeight())
        assertEquals("0", 0.0.formatCleanWeight())
    }

    @Test
    fun testExportToCsvProducesValidHeaderAndEscapedRows() {
        val session = WorkoutSession(
            id = "s1",
            title = "Bench, Day \"Heavy\"",
            startTimeEpochMs = 1700000000000L,
            isCompleted = true
        )
        val set = WorkoutSet(
            id = "st1",
            sessionId = "s1",
            exerciseId = "ex_bench",
            setNumber = 1,
            weightKg = 100.0,
            reps = 5,
            rpe = 8.5,
            isCompleted = true,
            isPersonalRecord = true
        )

        val csv = WorkoutDataExporter.exportToCsv(
            sessions = listOf(session),
            exercises = listOf(bench),
            sets = listOf(set)
        )

        assertTrue(csv.contains("Datum,Trénink,Cvik,Partie,Typ_Série,Série,Váha_kg,Opakování,RPE,Dokončeno,1RM_kg,Osobní_Rekord"))
        // Escaped title: "Bench, Day ""Heavy"""
        assertTrue(csv.contains("\"Bench, Day \"\"Heavy\"\"\""))
        assertTrue(csv.contains("Bench press"))
        assertTrue(csv.contains("Hrudník"))
        assertTrue(csv.contains("NORMAL,1,100.0,5,8.5,1"))
        assertTrue(csv.lines()[1].contains(",1,")) // PR = 1
    }

    @Test
    fun testExportToJsonProducesValidJsonStructure() {
        val session = WorkoutSession(
            id = "s1",
            title = "Test Session",
            startTimeEpochMs = 1000L,
            isCompleted = true,
            totalVolumeKg = 500.0
        )
        val set = WorkoutSet(
            id = "st1",
            sessionId = "s1",
            exerciseId = "ex_bench",
            setNumber = 1,
            weightKg = 100.0,
            reps = 5,
            isCompleted = true
        )

        val json = WorkoutDataExporter.exportToJson(
            sessions = listOf(session),
            exercises = listOf(bench),
            sets = listOf(set)
        )

        assertTrue(json.contains("\"sessionsCount\": 1"))
        assertTrue(json.contains("\"exercisesCount\": 1"))
        assertTrue(json.contains("\"setsCount\": 1"))
        assertTrue(json.contains("\"id\": \"s1\""))
        assertTrue(json.contains("\"name\": \"Bench press\""))
        assertTrue(json.contains("\"weightKg\": 100.0"))
    }
}
