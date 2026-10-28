package features.trainingplans.data

import androidx.room.*
import features.trainingplans.api.*

@Entity(tableName = "training_plans")
data class TrainingPlanEntity(@PrimaryKey val id: String, val name: String, val notes: String, val revision: Long, val archived: Boolean)

@Entity(tableName = "training_plan_slots", foreignKeys = [ForeignKey(
    entity = TrainingPlanEntity::class, parentColumns = ["id"], childColumns = ["planId"], onDelete = ForeignKey.CASCADE
)], indices = [Index("planId")])
data class PlanSlotEntity(@PrimaryKey val id: String, val planId: String, val label: String, val routineId: String, val orderIndex: Int)

@Entity(tableName = "training_plan_progress", foreignKeys = [ForeignKey(
    entity = TrainingPlanEntity::class, parentColumns = ["id"], childColumns = ["planId"], onDelete = ForeignKey.CASCADE
)])
data class PlanProgressEntity(@PrimaryKey val planId: String, val nextSlotId: String?, val progressRevision: Long, val needsReview: Boolean)

/** Device-local singleton preference; excluded from synchronization. */
@Entity(tableName = "training_plan_selection")
data class PlanSelectionEntity(@PrimaryKey val id: Int = 0, val planId: String?)

data class TrainingPlanSnapshot(
    @Embedded val plan: TrainingPlanEntity,
    @Relation(parentColumn = "id", entityColumn = "planId") val slots: List<PlanSlotEntity>
)

internal fun TrainingPlanSnapshot.toDomain() = TrainingPlan(plan.id, plan.name, plan.notes, plan.revision, plan.archived,
    slots.sortedBy { it.orderIndex }.map { PlanSlot(it.id, it.label, it.routineId) })
internal fun PlanProgressEntity.toDomain() = PlanProgress(planId, nextSlotId, progressRevision, needsReview)
internal fun PlanProgress.entity() = PlanProgressEntity(planId, nextSlotId, progressRevision, needsReview)
