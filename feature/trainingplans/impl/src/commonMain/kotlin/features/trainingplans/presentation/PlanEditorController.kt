package features.trainingplans.presentation

import core.storage.CommandCoordinator
import core.storage.OrderedDrafts
import core.viewmodel.DraftCodec
import core.viewmodel.DurableEditorController
import features.trainingplans.api.TrainingPlanRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal fun planEditorController(id: String, repository: TrainingPlanRepository, drafts: OrderedDrafts, writes: CommandCoordinator) =
    DurableEditorController(drafts, writes, "trainingplans", id, object : DraftCodec<PlanDraft> {
        override fun encode(draft: PlanDraft): String = Json.encodeToString(draft)
        override fun decode(payload: String): PlanDraft = Json.decodeFromString<PlanDraft>(payload).also { require(it.version == 1) }
    }) { draftId ->
        when {
            draftId == "new" -> PlanDraft()
            draftId.startsWith("edit:") -> (repository.getById(draftId.removePrefix("edit:")) ?: error("Missing source")).toDraft()
            else -> error("Missing draft")
        }
    }
