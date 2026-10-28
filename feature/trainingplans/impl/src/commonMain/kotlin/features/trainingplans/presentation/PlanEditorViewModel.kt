package features.trainingplans.presentation

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.job
import core.result.DataError
import core.result.Result
import core.storage.*
import core.viewmodel.BaseViewModel
import features.trainingplans.api.*
import features.routines.api.*

import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class PlanEditorViewModel(
    private val draftId: String, private val repository: TrainingPlanRepository,
    private val drafts: OrderedDrafts, private val store: DraftStore, private val writes: CommandCoordinator,
    private val routines: RoutineRepository,
) : BaseViewModel<PlanEditorContract.State, PlanEditorContract.Intent, PlanEditorContract.Effect>(PlanEditorContract.State()) {
    private var session: OrderedDrafts.Session? = null
    init { load() }
    override fun onOperationFailure(error: DataError.Local) {
        updateState { copy(isLoading = false, error = "Úložiště není dostupné. Zkuste akci znovu; návrh zůstává zachován.") }
    }
    // Only this operation may release its terminal lock. Background autosave/usage errors cannot.
    private fun launchTerminal(block: suspend () -> Unit) = launchOperation {
        try { block() }
        catch (failure: Exception) {
            updateState { copy(isSaving = false) }
            throw failure
        }
    }
    override fun handleIntent(intent: PlanEditorContract.Intent) {
        if (uiState.value.isSaving) return
        when (intent) {
            is PlanEditorContract.Intent.Mutate -> uiState.value.draft?.let { handleIntent(PlanEditorContract.Intent.Edit(intent.transform(it))) }
            is PlanEditorContract.Intent.Edit -> {
                if (uiState.value.draft == null) return
                updateState { copy(draft = intent.draft, error = null) }
                // Enqueue before launching: ViewModel cancellation cannot drop an accepted keystroke.
                val ack = session?.put(Json.encodeToString(intent.draft)) ?: return
                launchOperation { ack.await() }
            }
            PlanEditorContract.Intent.Save -> publish(false)
            PlanEditorContract.Intent.SaveCopy -> publish(true)
            PlanEditorContract.Intent.Reload -> reload()
            PlanEditorContract.Intent.Retry -> if (uiState.value.draft == null) load() else {
                val ack = session?.put(Json.encodeToString(uiState.value.draft!!)) ?: return
                launchOperation { ack.await(); updateState { copy(error = null) } }
            }
            PlanEditorContract.Intent.Back -> {
                if (uiState.value.draft == null) sendEffect(PlanEditorContract.Effect.Back)
                else {
                    updateState { copy(confirmExit = true) }
                    val ack = session?.barrier() ?: return
                    launchOperation { ack.await() }
                }
            }
            PlanEditorContract.Intent.Continue -> updateState { copy(confirmExit = false) }
            PlanEditorContract.Intent.Discard -> {
                val ack = session?.discard() ?: return
                updateState { copy(isSaving = true) }
                launchTerminal { ack.await(); sendEffect(PlanEditorContract.Effect.Back) }
            }
        }
    }
    private fun load() = launchOperation {
        updateState { copy(isLoading = true, error = null) }
        val opened = session ?: try {
            drafts.open("trainingplans", draftId, viewModelScope.coroutineContext.job).await().also { session = it }
        } catch (_: DraftAlreadyOpen) {
            updateState { copy(isLoading = false, error = "Tento návrh je otevřený v jiné obrazovce. Vraťte se do původního editoru; po jeho zavření můžete zkusit znovu.") }
            return@launchOperation
        }
        updateState { copy(canDiscard = true) }
        val draft = if (opened.payload != null) Json.decodeFromString<PlanDraft>(opened.payload!!).also { require(it.version == 1) }
        else when {
            draftId == "new" -> PlanDraft()
            draftId.startsWith("edit:") -> (repository.getById(draftId.removePrefix("edit:")) ?: error("Missing source")).toDraft()
            else -> error("Missing draft")
        }
        opened.put(Json.encodeToString(draft)).await()
        val choices = routines.observeAll(true).first()
        updateState { copy(draft = draft, choices = choices, isLoading = false) }

    }
    private fun reload() {
        val current = uiState.value.draft ?: return
        if(current.expectedRevision == null) return
        updateState { copy(isSaving = true) }
        launchTerminal {
            val fresh = (repository.getById(current.id) ?: error("Missing source")).toDraft()
            session!!.put(Json.encodeToString(fresh)).await()
            updateState { copy(draft = fresh, conflict = false, error = null, isSaving = false) }
        }
    }
    private class PublishFailure(val code: PlanError) : RuntimeException()
    private fun publish(asCopy: Boolean) {
        val draft = uiState.value.draft ?: return
        val aggregate = draft.aggregate(asCopy)
        if (aggregate == null) { updateState { copy(error = "Zkontrolujte název, položky a číselné hodnoty. Návrh zůstává rozepsaný.") }; return }
        updateState { copy(isSaving = true, error = null) }
        val ack = session!!.finish {
            writes.write {
                val saved = when(val result = repository.save(aggregate, if(asCopy) null else draft.expectedRevision)) {
                    is Result.Success -> result.data
                    is Result.Error -> throw PublishFailure(result.error)
                }
                store.delete("trainingplans", draftId)
                saved.id
            }
        }
        launchTerminal {
            try {
                val id = ack.await()
                sendEffect(PlanEditorContract.Effect.Saved(id))
            } catch (e: PublishFailure) {
                updateState { copy(isSaving = false, conflict = e.code == PlanError.REVISION_CONFLICT,
                    error = if(e.code == PlanError.REVISION_CONFLICT) "Uložená verze se změnila. Načtěte aktuální nebo uložte svůj návrh jako kopii."
                    else "Nelze uložit: ${e.code}. Zkontrolujte také archivované odkazy.") }
            }
        }
    }
}
