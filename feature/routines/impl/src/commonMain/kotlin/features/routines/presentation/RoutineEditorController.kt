package features.routines.presentation

import core.storage.CommandCoordinator
import core.storage.OrderedDrafts
import core.viewmodel.DraftCodec
import core.viewmodel.DurableEditorController
import features.routines.api.RoutineRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal fun routineEditorController(id: String, repository: RoutineRepository, drafts: OrderedDrafts, writes: CommandCoordinator) =
    DurableEditorController(drafts, writes, "routines", id, object : DraftCodec<RoutineDraft> {
        override fun encode(draft: RoutineDraft): String = Json.encodeToString(draft)
        override fun decode(payload: String): RoutineDraft = Json.decodeFromString<RoutineDraft>(payload).also { require(it.version == 1) }
    }) { draftId ->
        when {
            draftId == "new" -> RoutineDraft()
            draftId.startsWith("edit:") -> (repository.getById(draftId.removePrefix("edit:")) ?: error("Missing source")).toDraft()
            else -> error("Missing draft")
        }
    }
