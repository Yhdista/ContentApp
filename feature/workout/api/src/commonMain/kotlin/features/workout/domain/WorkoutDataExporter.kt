package features.workout.domain

import features.exercises.api.Exercise
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
enum class ExportFormat {
    CSV,
    JSON
}

object WorkoutDataExporter {

    /** Actual columns stay empty for omitted targets; original order has its own columns. */
    fun exportToCsv(
        sessions: List<WorkoutSession>, exercises: List<Exercise>, sets: List<WorkoutSet>,
        prescriptions: List<WorkoutPrescriptionSet> = emptyList()
    ): String {
        val exerciseMap = exercises.associateBy { it.id }
        val sessionMap = sessions.associateBy { it.id }
        val actualById = sets.associateBy { it.id }
        val targetById = prescriptions.associateBy { it.id }
        val ids = (actualById.keys + targetById.keys).sortedWith(
            compareBy<String> { sessionMap[actualById[it]?.sessionId ?: targetById[it]?.sessionId]?.startTimeEpochMs ?: 0L }
                .thenBy { actualById[it]?.sessionId ?: targetById[it]?.sessionId }
                .thenBy { targetById[it]?.exerciseOrder ?: actualById[it]?.exerciseOrder ?: Int.MAX_VALUE }
                .thenBy { targetById[it]?.setNumber ?: actualById[it]?.setNumber }
                .thenBy { it })
        val sb = StringBuilder("Datum,Trénink,Cvik,Partie,Typ_Série,Série,Váha_kg,Opakování,RPE,Dokončeno,1RM_kg,Osobní_Rekord,Workout_Entry_ID,Pořadí_Cviku,Pauza_s,Předepsaná_Váha_kg,Předepsaná_Opakování,Předepsaný_Typ,Pořadí_Rekonstruováno,Session_ID,Set_ID,Exercise_ID,Source_Routine_ID,Source_Routine_Revision,Source_Plan_ID,Source_Plan_Revision,Source_Slot_ID,Source_Progress_Revision,Stav_Skutečnosti,Předepsaná_Série,Předepsané_Pořadí_Cviku,Source_Routine_Exercise_ID,Source_Routine_Set_ID\n")
        for (id in ids) {
            val set = actualById[id]
            val target = targetById[id]
            val sessionId = set?.sessionId ?: target!!.sessionId
            val exerciseId = set?.exerciseId ?: target!!.exerciseId
            val session = sessionMap[sessionId]
            val exercise = exerciseMap[exerciseId]
            val date = session?.let {
                val seconds = it.startTimeEpochMs / 1000
                val hours = ((seconds % 86400) / 3600).toString().padStart(2, '0')
                val minutes = ((seconds % 3600) / 60).toString().padStart(2, '0')
                "Den ${seconds / 86400}, $hours:$minutes"
            }
            val oneRepMax = set?.let { if (it.isCompleted)
                (PersonalRecordCalculator.calculateOneRepMax(it.weightKg, it.reps) * 10).toInt() / 10.0 else 0.0 }
            val columns = listOf(
                date, session?.title ?: "Trénink", target?.name ?: set?.exerciseNameSnapshot ?: exercise?.name ?: "Neznámý cvik",
                (target?.muscleGroup ?: set?.muscleGroupSnapshot ?: exercise?.muscleGroup)?.displayName,
                set?.setType?.name, set?.setNumber, set?.weightKg, set?.reps, set?.rpe,
                set?.let { if (it.isCompleted) 1 else 0 }, oneRepMax, set?.let { if (it.isPersonalRecord) 1 else 0 },
                set?.workoutExerciseId ?: target?.workoutExerciseId, set?.exerciseOrder, target?.restSeconds ?: set?.restSecondsSnapshot,
                target?.weightKg ?: set?.prescribedWeightKg, target?.reps ?: set?.prescribedReps,
                (target?.setType ?: set?.prescribedSetType)?.name, set?.orderReconstructed,
                sessionId, id, exerciseId, session?.sourceRoutineId, session?.sourceRoutineRevision,
                session?.sourcePlan?.planId, session?.sourcePlan?.planRevision, session?.sourcePlan?.slotId, session?.sourcePlan?.progressRevision,
                if (set == null) "OMITTED" else "ACTUAL", target?.setNumber, target?.exerciseOrder,
                target?.sourceRoutineExerciseId, target?.sourceRoutineSetId)
            sb.append(columns.joinToString(",") { escapeCsv(it?.toString() ?: "") }).append('\n')
        }
        return sb.toString()
    }

    fun exportToJson(
        sessions: List<WorkoutSession>, exercises: List<Exercise>, sets: List<WorkoutSet>,
        workoutExercises: List<WorkoutExercise> = emptyList(),
        prescriptions: List<WorkoutPrescriptionSet> = emptyList()
    ): String = kotlinx.serialization.json.Json { prettyPrint=true; encodeDefaults=true }.encodeToString(
        WorkoutExport(sessions.maxOfOrNull { it.startTimeEpochMs } ?: 0L,sessions.size,exercises.size,sets.size,
            sessions,exercises,sets.sortedWith(compareBy({ it.sessionId },{ it.exerciseOrder ?: Int.MAX_VALUE },{ it.setNumber })),workoutExercises,
            prescriptions.sortedWith(compareBy({ it.sessionId }, { it.exerciseOrder }, { it.setNumber }))))

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
    val exportedAtEpochMs: Long,val sessionsCount: Int,val exercisesCount: Int,val setsCount: Int,
    val sessions: List<WorkoutSession>,val exercises: List<Exercise>,val sets: List<WorkoutSet>,
    val workoutExercises: List<WorkoutExercise>, val prescriptions: List<WorkoutPrescriptionSet>
)
