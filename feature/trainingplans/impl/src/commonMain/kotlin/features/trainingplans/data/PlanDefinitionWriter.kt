package features.trainingplans.data

import features.routines.api.RoutineRepository
import features.trainingplans.api.*

/** Exceptions preserve typed failures and abort this command's savepoint after a partial write. */
internal class PlanCommandFailure(val error: PlanError) : RuntimeException()
internal fun fail(error: PlanError): Nothing = throw PlanCommandFailure(error)
internal fun requireCas(rows: Int) { if (rows != 1) fail(PlanError.REVISION_CONFLICT) }

/** Called only inside the shared coordinator, including routine API lookups and outbox writes. */
internal class PlanDefinitionWriter(
    private val dao: TrainingPlanDao,
    private val routines: RoutineRepository,
    private val outbox: PlanOutbox
) {
    suspend fun save(plan: TrainingPlan, expectedRevision: Long?): TrainingPlan {
        PlanValidation.validate(plan)?.let { fail(it) }
        val previous = dao.getById(plan.id)?.toDomain()
        if (plan.archived || previous?.archived == true) fail(PlanError.ARCHIVED)
        if (previous == null && expectedRevision != null) fail(PlanError.NOT_FOUND)
        if (previous != null && previous.revision != expectedRevision) fail(PlanError.REVISION_CONFLICT)
        val previousSlots = previous?.slots.orEmpty().associateBy { it.id }
        val references = plan.slots.map { it.routineId }.distinct().associateWith {
            routines.getById(it) ?: fail(PlanError.NOT_FOUND)
        }
        for (slot in plan.slots) {
            if (references.getValue(slot.routineId).archived && previousSlots[slot.id]?.routineId != slot.routineId) {
                fail(PlanError.ARCHIVED)
            }
        }
        val oldProgress = if (previous == null) null else dao.getProgress(plan.id)?.toDomain() ?: fail(PlanError.STORAGE)
        val saved = plan.copy(name = plan.name.trim(), revision = (previous?.revision ?: 0) + 1,
            slots = plan.slots.map { it.copy(label = it.label.trim()) })
        if (previous == null) {
            dao.insertPlan(TrainingPlanEntity(saved.id, saved.name, saved.notes, saved.revision, false))
        } else {
            requireCas(dao.updatePlan(saved.id, saved.name, saved.notes, previous.revision))
            dao.deleteSlots(saved.id)
        }
        dao.insertSlots(saved.slots.mapIndexed { index, slot -> PlanSlotEntity(slot.id, saved.id, slot.label, slot.routineId, index) })
        outbox.definition(saved, previous)
        val progress = PlanProgressRules.afterSave(saved, oldProgress)
        if (oldProgress == null) {
            dao.insertProgress(progress.entity())
            outbox.progress(progress, "INSERT")
        } else {
            persistProgress(progress, oldProgress)
        }
        return saved
    }

    suspend fun persistProgress(progress: PlanProgress, previous: PlanProgress) {
        if (progress == previous) return
        requireCas(dao.updateProgress(progress.planId, progress.nextSlotId, progress.progressRevision, progress.needsReview, previous.progressRevision))
        outbox.progress(progress, "UPDATE")
    }
}
