package features.workout.domain

import features.exercises.api.Exercise
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
enum class ExportFormat {
    CSV,
    JSON
}

object WorkoutDataExporter {

    fun exportToCsv(
        sessions: List<WorkoutSession>,
        exercises: List<Exercise>,
        sets: List<WorkoutSet>
    ): String {
        val exerciseMap = exercises.associateBy { it.id }
        val sessionMap = sessions.associateBy { it.id }

        val sb = StringBuilder()
        sb.append("Datum,Trénink,Cvik,Partie,Typ_Série,Série,Váha_kg,Opakování,RPE,Dokončeno,1RM_kg,Osobní_Rekord,Workout_Entry_ID,Pořadí_Cviku,Pauza_s,Předepsaná_Váha_kg,Předepsaná_Opakování,Předepsaný_Typ,Pořadí_Rekonstruováno,Session_ID,Set_ID,Exercise_ID,Source_Routine_ID,Source_Routine_Revision,Source_Plan_ID,Source_Plan_Revision,Source_Slot_ID,Source_Progress_Revision\n")

        val sortedSets = sets.sortedWith(
            compareBy<WorkoutSet> { sessionMap[it.sessionId]?.startTimeEpochMs ?: 0L }
                .thenBy { it.sessionId }
                .thenBy { it.exerciseOrder ?: Int.MAX_VALUE }
                .thenBy { it.setNumber }
        )

        for (set in sortedSets) {
            val session = sessionMap[set.sessionId]
            val exercise = exerciseMap[set.exerciseId]
            val dateStr = session?.let { s ->
                val totalSeconds = s.startTimeEpochMs / 1000
                val days = (totalSeconds / 86400).toInt()
                val remSec = (totalSeconds % 86400).toInt()
                val hours = (remSec / 3600).toString().padStart(2, '0')
                val minutes = ((remSec % 3600) / 60).toString().padStart(2, '0')
                "Den $days, $hours:$minutes"
            } ?: ""
            val sessionTitle = escapeCsv(session?.title ?: "Trénink")
            val exerciseName = escapeCsv(set.exerciseNameSnapshot ?: exercise?.name ?: "Neznámý cvik")
            val muscle = escapeCsv(set.muscleGroupSnapshot?.displayName ?: exercise?.muscleGroup?.displayName ?: "")
            val oneRepMax = if (set.isCompleted) {
                ((PersonalRecordCalculator.calculateOneRepMax(set.weightKg, set.reps) * 10).toInt() / 10.0).toString()
            } else {
                "0.0"
            }

            sb.append("${escapeCsv(dateStr)},$sessionTitle,$exerciseName,$muscle,${set.setType.name},${set.setNumber},${set.weightKg},${set.reps},${set.rpe ?: ""},${if (set.isCompleted) 1 else 0},$oneRepMax,${if (set.isPersonalRecord) 1 else 0},${escapeCsv(set.workoutExerciseId ?: "")},${set.exerciseOrder ?: ""},${set.restSecondsSnapshot ?: ""},${set.prescribedWeightKg ?: ""},${set.prescribedReps ?: ""},${set.prescribedSetType?.name ?: ""},${set.orderReconstructed},${escapeCsv(set.sessionId)},${escapeCsv(set.id)},${escapeCsv(set.exerciseId)},${escapeCsv(session?.sourceRoutineId ?: "")},${session?.sourceRoutineRevision ?: ""},${escapeCsv(session?.sourcePlan?.planId ?: "")},${session?.sourcePlan?.planRevision ?: ""},${escapeCsv(session?.sourcePlan?.slotId ?: "")},${session?.sourcePlan?.progressRevision ?: ""}\n")
        }

        return sb.toString()
    }

    fun exportToJson(
        sessions: List<WorkoutSession>, exercises: List<Exercise>, sets: List<WorkoutSet>,
        templates: List<WorkoutTemplate> = emptyList(), workoutExercises: List<WorkoutExercise> = emptyList()
    ): String = kotlinx.serialization.json.Json { prettyPrint=true; encodeDefaults=true }.encodeToString(
        WorkoutExport(sessions.maxOfOrNull { it.startTimeEpochMs } ?: 0L,sessions.size,exercises.size,sets.size,templates.size,
            sessions,exercises,sets.sortedWith(compareBy({ it.sessionId },{ it.exerciseOrder ?: Int.MAX_VALUE },{ it.setNumber })),templates,workoutExercises))

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

}

@Serializable
private data class WorkoutExport(
    val exportedAtEpochMs: Long,val sessionsCount: Int,val exercisesCount: Int,val setsCount: Int,val templatesCount: Int,
    val sessions: List<WorkoutSession>,val exercises: List<Exercise>,val sets: List<WorkoutSet>,val templates: List<WorkoutTemplate>,
    val workoutExercises: List<WorkoutExercise>
)
