package features.trainingplans.presentation

import features.trainingplans.api.*
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
internal fun newId() = Uuid.random().toString()
@Serializable
internal data class PlanDraft(val id: String = newId(), val expectedRevision: Long? = null,
    val name: String = "", val notes: String = "", val slots: List<PlanSlot> = emptyList(), val version: Int = 1) {
    fun aggregate(copy: Boolean = false): TrainingPlan? = TrainingPlan(if(copy) newId() else id, name, notes,
        slots = if(copy) slots.map { it.copy(id = newId()) } else slots).takeIf { PlanValidation.validate(it) == null }
}
internal fun TrainingPlan.toDraft() = PlanDraft(id,revision,name,notes,slots)
