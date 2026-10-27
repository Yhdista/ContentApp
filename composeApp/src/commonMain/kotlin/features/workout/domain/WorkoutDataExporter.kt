package features.workout.domain

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
        sb.append("Datum,Trénink,Cvik,Partie,Typ_Série,Série,Váha_kg,Opakování,RPE,Dokončeno,1RM_kg,Osobní_Rekord\n")

        val sortedSets = sets.sortedWith(
            compareBy<WorkoutSet> { sessionMap[it.sessionId]?.startTimeEpochMs ?: 0L }
                .thenBy { it.sessionId }
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
            val exerciseName = escapeCsv(exercise?.name ?: "Neznámý cvik")
            val muscle = escapeCsv(exercise?.muscleGroup?.displayName ?: "")
            val oneRepMax = if (set.isCompleted) {
                ((PersonalRecordCalculator.calculateOneRepMax(set.weightKg, set.reps) * 10).toInt() / 10.0).toString()
            } else {
                "0.0"
            }

            sb.append("${escapeCsv(dateStr)},$sessionTitle,$exerciseName,$muscle,${set.setType.name},${set.setNumber},${set.weightKg},${set.reps},${set.rpe ?: ""},${if (set.isCompleted) 1 else 0},$oneRepMax,${if (set.isPersonalRecord) 1 else 0}\n")
        }

        return sb.toString()
    }

    fun exportToJson(
        sessions: List<WorkoutSession>,
        exercises: List<Exercise>,
        sets: List<WorkoutSet>,
        templates: List<WorkoutTemplate> = emptyList()
    ): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"exportedAtEpochMs\": ${sessions.maxOfOrNull { it.startTimeEpochMs } ?: 0L},\n")
        sb.append("  \"sessionsCount\": ${sessions.size},\n")
        sb.append("  \"exercisesCount\": ${exercises.size},\n")
        sb.append("  \"setsCount\": ${sets.size},\n")
        sb.append("  \"templatesCount\": ${templates.size},\n")

        // Sessions
        sb.append("  \"sessions\": [\n")
        sessions.forEachIndexed { i, s ->
            sb.append("    {\n")
            sb.append("      \"id\": \"${escapeJson(s.id)}\",\n")
            sb.append("      \"title\": \"${escapeJson(s.title)}\",\n")
            sb.append("      \"startTimeEpochMs\": ${s.startTimeEpochMs},\n")
            sb.append("      \"endTimeEpochMs\": ${s.endTimeEpochMs ?: "null"},\n")
            sb.append("      \"isCompleted\": ${s.isCompleted},\n")
            sb.append("      \"totalVolumeKg\": ${s.totalVolumeKg}\n")
            sb.append("    }${if (i < sessions.size - 1) "," else ""}\n")
        }
        sb.append("  ],\n")

        // Exercises
        sb.append("  \"exercises\": [\n")
        exercises.forEachIndexed { i, e ->
            sb.append("    {\n")
            sb.append("      \"id\": \"${escapeJson(e.id)}\",\n")
            sb.append("      \"name\": \"${escapeJson(e.name)}\",\n")
            sb.append("      \"muscleGroup\": \"${e.muscleGroup.name}\",\n")
            sb.append("      \"defaultRestSeconds\": ${e.defaultRestSeconds}\n")
            sb.append("    }${if (i < exercises.size - 1) "," else ""}\n")
        }
        sb.append("  ],\n")

        // Sets
        sb.append("  \"sets\": [\n")
        sets.forEachIndexed { i, st ->
            sb.append("    {\n")
            sb.append("      \"id\": \"${escapeJson(st.id)}\",\n")
            sb.append("      \"sessionId\": \"${escapeJson(st.sessionId)}\",\n")
            sb.append("      \"exerciseId\": \"${escapeJson(st.exerciseId)}\",\n")
            sb.append("      \"setType\": \"${st.setType.name}\",\n")
            sb.append("      \"setNumber\": ${st.setNumber},\n")
            sb.append("      \"weightKg\": ${st.weightKg},\n")
            sb.append("      \"reps\": ${st.reps},\n")
            sb.append("      \"rpe\": ${st.rpe ?: "null"},\n")
            sb.append("      \"isCompleted\": ${st.isCompleted},\n")
            sb.append("      \"isPersonalRecord\": ${st.isPersonalRecord}\n")
            sb.append("    }${if (i < sets.size - 1) "," else ""}\n")
        }
        sb.append("  ],\n")

        // Templates
        sb.append("  \"templates\": [\n")
        templates.forEachIndexed { i, tmpl ->
            sb.append("    {\n")
            sb.append("      \"id\": \"${escapeJson(tmpl.id)}\",\n")
            sb.append("      \"name\": \"${escapeJson(tmpl.name)}\",\n")
            sb.append("      \"description\": \"${escapeJson(tmpl.description)}\",\n")
            sb.append("      \"exercisesCount\": ${tmpl.exercises.size},\n")
            sb.append("      \"exercises\": [\n")
            tmpl.exercises.forEachIndexed { j, tex ->
                sb.append("        {\n")
                sb.append("          \"exerciseId\": \"${escapeJson(tex.exerciseId)}\",\n")
                sb.append("          \"exerciseName\": \"${escapeJson(tex.exerciseName)}\",\n")
                sb.append("          \"orderIndex\": ${tex.orderIndex},\n")
                sb.append("          \"targetSets\": ${tex.targetSets},\n")
                sb.append("          \"defaultWeightKg\": ${tex.defaultWeightKg},\n")
                sb.append("          \"defaultReps\": ${tex.defaultReps}\n")
                sb.append("        }${if (j < tmpl.exercises.size - 1) "," else ""}\n")
            }
            sb.append("      ]\n")
            sb.append("    }${if (i < templates.size - 1) "," else ""}\n")
        }
        sb.append("  ]\n")

        sb.append("}")
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun escapeJson(value: String): String {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
