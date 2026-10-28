package features.workout.data

import core.storage.CommandCoordinator
import features.workout.data.datasource.WorkoutLocalDataSource
import features.workout.domain.WorkoutDataExport
import features.workout.domain.WorkoutDataExporter
import features.workout.domain.WorkoutSession
import features.workout.domain.WorkoutSet
import features.exercises.api.Exercise

internal class LocalWorkoutDataExport(
    private val localDataSource: WorkoutLocalDataSource,
    private val writes: CommandCoordinator
) : WorkoutDataExport {
    private data class Snapshot(
        val sessions: List<WorkoutSession>,
        val exercises: List<Exercise>,
        val sets: List<WorkoutSet>,
        val entries: List<features.workout.domain.WorkoutExercise>,
        val prescriptions: List<features.workout.domain.WorkoutPrescriptionSet>
    )

    private suspend fun snapshot() = writes.write {
        val sessions=localDataSource.getAllSessions()
        Snapshot(sessions,localDataSource.getAllExercises(),localDataSource.getAllSets(),
            localDataSource.getAllWorkoutExercises(), localDataSource.getAllPrescriptions())
    }

    override suspend fun exportWorkoutDataCsv(): String {
        val data = snapshot()
        return WorkoutDataExporter.exportToCsv(data.sessions, data.exercises, data.sets, data.prescriptions)
    }

    override suspend fun exportWorkoutDataJson(): String {
        val data = snapshot()
        return WorkoutDataExporter.exportToJson(data.sessions, data.exercises, data.sets, data.entries, data.prescriptions)
    }
}
