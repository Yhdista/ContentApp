package features.workout.data

import core.common.AppClock
import core.database.MutationDao
import core.database.SyncMutationEntity
import core.result.Result
import core.storage.CommandCoordinator
import features.workout.data.dao.WorkoutDao
import features.workout.data.mapper.*
import features.workout.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** 🏋️ Snapshots and performance share the app's writer transaction and granular outbox. */
@OptIn(ExperimentalUuidApi::class)
internal class RoomWorkoutStore(
    private val dao: WorkoutDao, private val writes: CommandCoordinator,
    private val outbox: MutationDao, private val clock: AppClock
) : WorkoutStore {
    override suspend fun activeSession() = dao.getActiveSessionSync()?.toDomain()
    override suspend fun createFromPrescription(prescription: WorkoutStartPrescription): Result<StartOutcome, WorkoutError> = command {
        activeSession()?.let { return@command StartOutcome.ResumedExisting(it.id) }
        require(prescription.title.trim().length in 1..100 && prescription.exercises.size in 1..50)
        require(prescription.exercises.map { it.id }.distinct().size == prescription.exercises.size)
        prescription.exercises.forEach { e ->
            require(e.id.isNotBlank() && e.exerciseId.isNotBlank() && e.restSeconds in 0..3600 && e.sets.size in 1..20)
            e.sets.forEach { require(it.weightKg.isFinite() && it.weightKg >= 0 && it.reps in 1..999) }
        }
        val session = WorkoutSession(newId(), prescription.title, clock.nowMillis(), sourceRoutineId=prescription.sourceRoutineId,
            sourceRoutineRevision=prescription.sourceRoutineRevision, sourcePlan=prescription.sourcePlan)
        dao.insertSession(session.toEntity()); enqueue("workout_session",session.id,"INSERT",Json.encodeToString(session))
        val previous = prescription.exercises.map { it.exerciseId }.distinct().associateWith { id ->
            dao.getLastPerformanceForExercise(id)?.let { "${it.weightKg} kg × ${it.reps}" }
        }
        prescription.exercises.forEachIndexed { index, e ->
            // Prescription entry IDs describe source instances; persisted IDs are fresh for every workout.
            val entry=WorkoutExercise(newId(),session.id,e.exerciseId,e.name,e.muscleGroup,e.restSeconds,index)
            dao.insertExercise(entry.toEntity()); enqueue("workout_exercise",entry.id,"INSERT",Json.encodeToString(entry))
            e.sets.forEachIndexed { setIndex, p ->
                val set=WorkoutSet(newId(),session.id,e.exerciseId,setIndex+1,p.weightKg,p.reps,setType=p.setType,
                    previousPerformance=previous[e.exerciseId],workoutExerciseId=entry.id,prescribedWeightKg=p.weightKg,prescribedReps=p.reps,prescribedSetType=p.setType)
                dao.insertOrUpdateSet(set.toEntity()); enqueue("workout_set",set.id,"INSERT",Json.encodeToString(set))
            }
        }
        StartOutcome.Created(session.id)
    }
    override suspend fun completeOnce(sessionId: String): Result<CompletionOutcome, WorkoutError> = command {
        val session=dao.getSessionById(sessionId)?.toDomain() ?: throw MissingWorkout()
        if(session.isCompleted) return@command CompletionOutcome(session,false)
        val volume=dao.getSetsForSessionSnapshot(sessionId).filter { it.isCompleted }.sumOf { it.weightKg*it.reps }
        val completed=session.copy(isCompleted=true,endTimeEpochMs=clock.nowMillis(),totalVolumeKg=volume)
        dao.completeSession(sessionId,completed.endTimeEpochMs!!,volume)
        enqueue("workout_session",sessionId,"COMPLETE",Json.encodeToString(completed))
        CompletionOutcome(completed,true)
    }
    private suspend fun enqueue(entity: String,id: String,operation: String,payload: String) =
        outbox.insertMutation(SyncMutationEntity(newId(),entity,id,operation,payload,clock.nowMillis()))
    private suspend fun <T> command(block: suspend () -> T): Result<T,WorkoutError> = try {
        Result.Success(writes.write { block() })
    } catch(e: CancellationException) { throw e
    } catch(_: MissingWorkout) { Result.Error(WorkoutError.NOT_FOUND)
    } catch(_: IllegalArgumentException) { Result.Error(WorkoutError.VALIDATION)
    } catch(_: Exception) { Result.Error(WorkoutError.STORAGE) }
    private class MissingWorkout : RuntimeException()
    private fun newId()=Uuid.random().toString()
}
