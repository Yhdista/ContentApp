package features.trainingplans.data

import core.common.AppClock
import core.database.MutationDao
import core.result.Result
import core.storage.CommandCoordinator
import features.routines.api.RoutineRepository
import features.trainingplans.api.*
import features.workout.domain.PlanWorkoutSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
internal class RoomTrainingPlanRepository(
    private val dao: TrainingPlanDao,
    private val routines: RoutineRepository,
    private val writes: CommandCoordinator,
    outboxDao: MutationDao,
    clock: AppClock
) : TrainingPlanRepository {
    private val outbox = PlanOutbox(outboxDao, clock)
    private val definitions = PlanDefinitionWriter(dao, routines, outbox)

    override fun observeAll(includeArchived: Boolean) = dao.observeAll(includeArchived).map { rows -> rows.map { it.toDomain() } }
    override fun observeById(id: String) = dao.observeById(id).map { it?.toDomain() }
    override suspend fun getById(id: String) = dao.getById(id)?.toDomain()
    override fun observeProgress(planId: String) = dao.observeProgress(planId).map { it?.toDomain() }
    override suspend fun getProgress(planId: String) = dao.getProgress(planId)?.toDomain()
    override fun observeActive() = dao.observeActive().map { it?.toDomain() }

    override suspend fun save(plan: TrainingPlan, expectedRevision: Long?): Result<TrainingPlan, PlanError> = command {
        definitions.save(plan, expectedRevision)
    }

    override suspend fun duplicate(id: String): Result<TrainingPlan, PlanError> = command {
        val original = findPlan(id)
        definitions.save(original.copy(id = newId(), revision = 0, archived = false,
            slots = original.slots.map { it.copy(id = newId()) }), null)
    }

    override suspend fun archive(id: String, expectedRevision: Long): Result<Unit, PlanError> = command {
        val plan = activePlan(id)
        if (plan.revision != expectedRevision) fail(PlanError.REVISION_CONFLICT)
        requireCas(dao.archive(id, expectedRevision))
        dao.clearSelection(id)
        outbox.plan(plan.copy(revision = plan.revision + 1, archived = true), "UPDATE")
    }

    override suspend fun selectActive(planId: String?): Result<Unit, PlanError> = command {
        if (planId != null) activePlan(planId)
        dao.selectActive(PlanSelectionEntity(planId = planId))
    }

    override suspend fun skipNext(planId: String, expectedProgressRevision: Long): Result<PlanProgress, PlanError> = command {
        val plan = activePlan(planId)
        val current = progress(planId, expectedProgressRevision)
        currentSlot(plan, current)
        PlanProgressRules.next(plan, current).also { definitions.persistProgress(it, current) }
    }

    override suspend fun confirmNext(planId: String, slotId: String, expectedProgressRevision: Long): Result<PlanProgress, PlanError> = command {
        val plan = activePlan(planId)
        val current = progress(planId, expectedProgressRevision)
        if (plan.slots.none { it.id == slotId }) fail(PlanError.NOT_FOUND)
        current.copy(nextSlotId = slotId, progressRevision = current.progressRevision + 1, needsReview = false)
            .also { definitions.persistProgress(it, current) }
    }

    override suspend fun prepareStart(planId: String): Result<PlanStartContext, PlanError> = command {
        val plan = activePlan(planId)
        val progress = progress(planId)
        val slot = currentSlot(plan, progress)
        val routine = routines.getById(slot.routineId) ?: fail(PlanError.NOT_FOUND)
        if (routine.archived) fail(PlanError.ARCHIVED)
        PlanStartContext(plan, slot, PlanWorkoutSource(plan.id, plan.revision, slot.id, progress.progressRevision))
    }

    override suspend fun recordCompleted(source: PlanWorkoutSource): Result<ProgressOutcome, PlanError> = command {
        // Workout prescription is already captured. Changes to the source routine do not affect completion.
        val plan = findPlan(source.planId)
        val current = progress(plan.id)
        PlanProgressRules.completed(plan, current, source).also { definitions.persistProgress(it.progress, current) }
    }

    private suspend fun findPlan(id: String) = dao.getById(id)?.toDomain() ?: fail(PlanError.NOT_FOUND)
    private suspend fun activePlan(id: String) = findPlan(id).also { if (it.archived) fail(PlanError.ARCHIVED) }
    private suspend fun progress(planId: String, expected: Long? = null): PlanProgress {
        val current = dao.getProgress(planId)?.toDomain() ?: fail(PlanError.STORAGE)
        if (expected != null && current.progressRevision != expected) fail(PlanError.REVISION_CONFLICT)
        return current
    }
    private fun currentSlot(plan: TrainingPlan, progress: PlanProgress): PlanSlot {
        if (progress.needsReview) fail(PlanError.REVIEW_REQUIRED)
        return plan.slots.firstOrNull { it.id == progress.nextSlotId } ?: fail(PlanError.REVIEW_REQUIRED)
    }

    private suspend fun <T> command(block: suspend () -> T): Result<T, PlanError> = try {
        writes.write { Result.Success(block()) }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: PlanCommandFailure) {
        Result.Error(failure.error)
    } catch (_: Exception) {
        Result.Error(PlanError.STORAGE)
    }

    private fun newId() = Uuid.random().toString()
}

fun trainingPlanFeatureModule(): Module = module {
    single<TrainingPlanRepository> { RoomTrainingPlanRepository(get(), get(), get(), get(), get()) }
}
