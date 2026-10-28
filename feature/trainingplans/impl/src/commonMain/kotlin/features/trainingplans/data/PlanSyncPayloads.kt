package features.trainingplans.data

import core.common.AppClock
import core.database.MutationDao
import core.database.SyncMutationEntity
import features.trainingplans.api.PlanProgress
import features.trainingplans.api.TrainingPlan
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
internal data class TrainingPlanPayload(val id: String, val name: String, val notes: String, val revision: Long, val archived: Boolean)
@Serializable
internal data class PlanSlotPayload(val id: String, val planId: String, val label: String, val routineId: String, val orderIndex: Int)
@Serializable
internal data class DeletePlanSlotPayload(val id: String)

/** Granular record payloads; selection preferences never enter this outbox. */
internal class PlanOutbox(private val dao: MutationDao, private val clock: AppClock) {
    suspend fun plan(plan: TrainingPlan, operation: String) = enqueue("training_plan", plan.id, operation,
        Json.encodeToString(TrainingPlanPayload(plan.id, plan.name, plan.notes, plan.revision, plan.archived)))

    suspend fun definition(saved: TrainingPlan, previous: TrainingPlan?) {
        plan(saved, if (previous == null) "INSERT" else "UPDATE")
        val previousIds = previous?.slots.orEmpty().map { it.id }.toSet()
        val savedIds = saved.slots.map { it.id }.toSet()
        for (id in previousIds - savedIds) {
            enqueue("training_plan_slot", id, "DELETE", Json.encodeToString(DeletePlanSlotPayload(id)))
        }
        saved.slots.forEachIndexed { order, slot ->
            enqueue("training_plan_slot", slot.id, if (slot.id in previousIds) "UPDATE" else "INSERT",
                Json.encodeToString(PlanSlotPayload(slot.id, saved.id, slot.label, slot.routineId, order)))
        }
    }

    suspend fun progress(progress: PlanProgress, operation: String) =
        enqueue("training_plan_progress", progress.planId, operation, Json.encodeToString(progress))

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun enqueue(entity: String, id: String, operation: String, payload: String) {
        dao.insertMutation(SyncMutationEntity(Uuid.random().toString(), entity, id, operation, payload, clock.nowMillis()))
    }
}
