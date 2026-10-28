package features.trainingplans.api

import core.result.Result
import core.result.RootError
import features.workout.domain.PlanWorkoutSource
import kotlinx.coroutines.flow.Flow

enum class PlanError : RootError { VALIDATION, NOT_FOUND, ARCHIVED, REVISION_CONFLICT, REVIEW_REQUIRED, STORAGE }

interface TrainingPlanRepository {
    fun observeAll(includeArchived: Boolean = false): Flow<List<TrainingPlan>>
    fun observeById(id: String): Flow<TrainingPlan?>
    suspend fun getById(id: String): TrainingPlan?
    fun observeProgress(planId: String): Flow<PlanProgress?>
    suspend fun getProgress(planId: String): PlanProgress?
    suspend fun save(plan: TrainingPlan, expectedRevision: Long?): Result<TrainingPlan, PlanError>
    suspend fun duplicate(id: String): Result<TrainingPlan, PlanError>
    suspend fun archive(id: String, expectedRevision: Long): Result<Unit, PlanError>
    /** Device-local preference; never resets progress and never emits an outbox mutation. */
    suspend fun selectActive(planId: String?): Result<Unit, PlanError>
    fun observeActive(): Flow<TrainingPlan?>
    suspend fun skipNext(planId: String, expectedProgressRevision: Long): Result<PlanProgress, PlanError>
    suspend fun confirmNext(planId: String, slotId: String, expectedProgressRevision: Long): Result<PlanProgress, PlanError>
    /** Only the current slot is eligible; no cursor changes. Joins the shared command transaction. */
    suspend fun prepareStart(planId: String): Result<PlanStartContext, PlanError>
    /** Caller invokes only for a first completion. ReviewRequired is a successful completion outcome. */
    suspend fun recordCompleted(source: PlanWorkoutSource): Result<ProgressOutcome, PlanError>
}
