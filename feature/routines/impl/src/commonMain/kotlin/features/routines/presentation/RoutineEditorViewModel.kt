package features.routines.presentation

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.job
import core.result.DataError
import core.result.Result
import core.storage.*
import core.viewmodel.BaseViewModel
import features.routines.api.*
import features.exercises.api.*

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import core.result.safeLocalCall
import core.result.onFailure
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class RoutineEditorViewModel(
    private val draftId: String, private val repository: RoutineRepository,
    private val drafts: OrderedDrafts, private val store: DraftStore, private val writes: CommandCoordinator,
    private val catalogue: ExerciseCatalog, private val usage: RoutineUsage,
) : BaseViewModel<RoutineEditorContract.State, RoutineEditorContract.Intent, RoutineEditorContract.Effect>(RoutineEditorContract.State()) {
    private var session: OrderedDrafts.Session? = null
    private var choicesJob: Job? = null
    init { load(); observeChoices() }
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
    override fun handleIntent(intent: RoutineEditorContract.Intent) {
        if (intent == RoutineEditorContract.Intent.RetryChoices) { observeChoices(); return }
        if (uiState.value.isSaving) return
        when (intent) {
            is RoutineEditorContract.Intent.Mutate -> uiState.value.draft?.let { handleIntent(RoutineEditorContract.Intent.Edit(intent.transform(it))) }
            is RoutineEditorContract.Intent.Edit -> {
                if (uiState.value.draft == null) return
                updateState { copy(draft = intent.draft, error = null) }
                // Enqueue before launching: ViewModel cancellation cannot drop an accepted keystroke.
                val ack = session?.put(Json.encodeToString(intent.draft)) ?: return
                launchOperation { ack.await() }
            }
            RoutineEditorContract.Intent.Save -> publish(false)
            RoutineEditorContract.Intent.SaveCopy -> publish(true)
            RoutineEditorContract.Intent.Reload -> reload()
            RoutineEditorContract.Intent.RetryChoices -> Unit
            RoutineEditorContract.Intent.Retry -> if (uiState.value.draft == null) load() else {
                observeChoices()
                val ack = session?.put(Json.encodeToString(uiState.value.draft!!)) ?: return
                launchOperation { ack.await(); updateState { copy(error = null) } }
            }
            RoutineEditorContract.Intent.Back -> {
                if (uiState.value.draft == null) sendEffect(RoutineEditorContract.Effect.Back)
                else {
                    updateState { copy(confirmExit = true) }
                    val ack = session?.barrier() ?: return
                    launchOperation { ack.await() }
                }
            }
            RoutineEditorContract.Intent.Continue -> updateState { copy(confirmExit = false) }
            RoutineEditorContract.Intent.Discard -> {
                val ack = session?.discard() ?: return
                updateState { copy(isSaving = true) }
                launchTerminal { ack.await(); sendEffect(RoutineEditorContract.Effect.Back) }
            }
        }
    }
    private fun load() = launchOperation {
        updateState { copy(isLoading = true, error = null) }
        val opened = session ?: try {
            drafts.open("routines", draftId, viewModelScope.coroutineContext.job).await().also { session = it }
        } catch (_: DraftAlreadyOpen) {
            updateState { copy(isLoading = false, error = "Tento návrh je otevřený v jiné obrazovce. Vraťte se do původního editoru; po jeho zavření můžete zkusit znovu.") }
            return@launchOperation
        }
        updateState { copy(canDiscard = true) }
        val draft = if (opened.payload != null) Json.decodeFromString<RoutineDraft>(opened.payload!!).also { require(it.version == 1) }
        else when {
            draftId == "new" -> RoutineDraft()
            draftId.startsWith("edit:") -> (repository.getById(draftId.removePrefix("edit:")) ?: error("Missing source")).toDraft()
            else -> error("Missing draft")
        }
        opened.put(Json.encodeToString(draft)).await()
        updateState { copy(draft = draft, isLoading = false) }
        launchOperation { usage.observeDependentPlanCount(draft.id).collect { n -> updateState { copy(dependentPlans = n) } } }
    }
    // Choice failures own only choice state; the durable draft and terminal command keep their owners.
    private fun observeChoices() {
        if (choicesJob?.isActive == true) return
        choicesJob = viewModelScope.launch {
            safeLocalCall {
                catalogue.observeExercises().collect { available ->
                    updateState { copy(choices = available, choicesError = null) }
                }
            }.onFailure {
                updateState { copy(choicesError = "Nabídku nelze obnovit. Zkuste znovu; rozepsaný návrh zůstává zachován.") }
            }
        }
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
    private class PublishFailure(val code: RoutineError) : RuntimeException()
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
                store.delete("routines", draftId)
                saved.id
            }
        }
        launchTerminal {
            try {
                val id = ack.await()
                sendEffect(RoutineEditorContract.Effect.Saved(id))
            } catch (e: PublishFailure) {
                updateState { copy(isSaving = false, conflict = e.code == RoutineError.REVISION_CONFLICT,
                    error = if(e.code == RoutineError.REVISION_CONFLICT) "Uložená verze se změnila. Načtěte aktuální nebo uložte svůj návrh jako kopii."
                    else "Nelze uložit: ${e.code}. Zkontrolujte také archivované odkazy.") }
            }
        }
    }
}
