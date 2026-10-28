package features.workout.domain

import core.result.Result
import core.result.RootError
import features.exercises.api.MuscleGroup
import kotlinx.serialization.Serializable

@Serializable
data class WorkoutExercise(
    val id: String, val sessionId: String, val exerciseId: String,
    val name: String, val muscleGroup: MuscleGroup, val restSeconds: Int,
    val orderIndex: Int, val orderReconstructed: Boolean = false
)
data class WorkoutStartPrescription(
    val title: String, val exercises: List<WorkoutExercisePrescription>,
    val sourceRoutineId: String? = null, val sourceRoutineRevision: Long? = null,
    val sourcePlan: PlanWorkoutSource? = null
)
data class WorkoutExercisePrescription(
    val id: String, val exerciseId: String, val name: String, val muscleGroup: MuscleGroup,
    val restSeconds: Int, val sets: List<WorkoutSetPrescription>
)
data class WorkoutSetPrescription(val weightKg: Double, val reps: Int, val setType: WorkoutSetType, val sourceSetId: String? = null)
enum class WorkoutError : RootError { VALIDATION, NOT_FOUND, ARCHIVED, REVISION_CONFLICT, REVIEW_REQUIRED, STORAGE }
sealed interface StartOutcome {
    val sessionId: String
    data class Created(override val sessionId: String) : StartOutcome
    data class ResumedExisting(override val sessionId: String) : StartOutcome
}
data class CompletionOutcome(val session: WorkoutSession, val firstCompletion: Boolean)
fun interface StartRoutineWorkout { suspend fun start(routineId: String): Result<StartOutcome, WorkoutError> }
fun interface StartPlanWorkout { suspend fun start(planId: String): Result<StartOutcome, WorkoutError> }
fun interface CompleteWorkout { suspend fun complete(sessionId: String): Result<CompletionOutcome, WorkoutError> }
/**
 * Low-level integration participants for the app-owned training workflow, never a presentation command.
 * [completeOnce] only completes the workout. The app must commit plan progress in the same writer transaction.
 * UI always calls [CompleteWorkout], which owns that combined invariant.
 */
interface WorkoutStore {
    suspend fun activeSession(): WorkoutSession?
    suspend fun createFromPrescription(prescription: WorkoutStartPrescription): Result<StartOutcome, WorkoutError>
    suspend fun completeOnce(sessionId: String): Result<CompletionOutcome, WorkoutError>
}
