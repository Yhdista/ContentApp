package features.workout

import features.workout.domain.Exercise
import features.workout.domain.MuscleGroup
import features.workout.domain.WorkoutDataExporter
import features.workout.domain.WorkoutSession
import features.workout.domain.WorkoutSet
import kotlin.test.Test
import kotlin.test.assertTrue

class WorkoutDataExporterTest {

    private val bench = Exercise("ex_bench", "Bench press", MuscleGroup.CHEST, defaultRestSeconds = 120)

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

        assertTrue(csv.contains("Datum,Trénink,Cvik,Partie,Série,Váha_kg,Opakování,RPE,Dokončeno,1RM_kg,Osobní_Rekord"))
        // Escaped title: "Bench, Day ""Heavy"""
        assertTrue(csv.contains("\"Bench, Day \"\"Heavy\"\"\""))
        assertTrue(csv.contains("Bench press"))
        assertTrue(csv.contains("Hrudník"))
        assertTrue(csv.contains("100.0,5,8.5,1"))
        assertTrue(csv.contains(",1\n")) // PR = 1
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
