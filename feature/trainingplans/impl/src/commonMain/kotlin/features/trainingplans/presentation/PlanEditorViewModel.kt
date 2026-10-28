package features.trainingplans.presentation

import androidx.lifecycle.viewModelScope
import core.result.DataError
import core.result.Result
import core.result.safeLocalCall
import core.result.onFailure
import core.viewmodel.BaseViewModel
import core.viewmodel.DurableEditorController
import features.trainingplans.api.*
import features.routines.api.*
import features.exercises.api.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class PlanEditorViewModel(
    private val editor: DurableEditorController<PlanDraft>, private val repository: TrainingPlanRepository,
    private val routines: RoutineRepository,
) : BaseViewModel<PlanEditorContract.State, PlanEditorContract.Intent, PlanEditorContract.Effect>(PlanEditorContract.State()) {
    private var choicesJob: Job? = null
    init {
        editor.bind(viewModelScope, ::onOperationFailure) { saving -> updateState { copy(isSaving = saving) } }
        load()
        observeChoices()
    }
    override fun onOperationFailure(error: DataError.Local) {
        updateState { copy(isLoading = false, error = "Úložiště není dostupné. Zkuste akci znovu; návrh zůstává zachován.") }
    }
    override fun handleIntent(intent: PlanEditorContract.Intent) {
        if (intent == PlanEditorContract.Intent.RetryChoices) { observeChoices(); return }
        if (editor.isBusy) return
        when (intent) {
            is PlanEditorContract.Intent.Mutate -> uiState.value.draft?.let { handleIntent(PlanEditorContract.Intent.Edit(intent.transform(it))) }
            is PlanEditorContract.Intent.Edit -> {
                if (uiState.value.draft == null) return
                updateState { copy(draft = intent.draft, error = null) }
                editor.edit(intent.draft)
            }
            PlanEditorContract.Intent.Save -> publish(false)
            PlanEditorContract.Intent.SaveCopy -> publish(true)
            PlanEditorContract.Intent.Reload -> reload()
            PlanEditorContract.Intent.RetryChoices -> Unit
            PlanEditorContract.Intent.Retry -> if (uiState.value.draft == null) load() else {
                observeChoices()
                editor.edit(uiState.value.draft!!) { updateState { copy(error = null) } }
            }
            PlanEditorContract.Intent.Back -> {
                if (uiState.value.draft == null) sendEffect(PlanEditorContract.Effect.Back)
                else { updateState { copy(confirmExit = true) }; editor.barrier() }
            }
            PlanEditorContract.Intent.Continue -> updateState { copy(confirmExit = false) }
            PlanEditorContract.Intent.Discard -> editor.discard { sendEffect(PlanEditorContract.Effect.Back) }
        }
    }
    private fun load() {
        updateState { copy(isLoading = true, error = null) }
        editor.open(onOpened = { draft ->
            updateState { copy(draft = draft, isLoading = false) }
        }, onLeaseAcquired = { updateState { copy(canDiscard = true) } }, onAlreadyOpen = {
            updateState { copy(isLoading = false, error = "Tento návrh je otevřený v jiné obrazovce. Vraťte se do původního editoru; po jeho zavření můžete zkusit znovu.") }
        })
    }
    // Choice failures own only choice state; the durable draft and terminal command keep their owners.
    private fun observeChoices() {
        if (choicesJob?.isActive == true) return
        choicesJob = viewModelScope.launch {
            safeLocalCall {
                routines.observeAll(true).collect { available ->
                    updateState { copy(choices = available, choicesError = null) }
                }
            }.onFailure {
                updateState { copy(choicesError = "Nabídku nelze obnovit. Zkuste znovu; rozepsaný návrh zůstává zachován.") }
            }
        }
    }
    private fun reload() {
        val current = uiState.value.draft ?: return
        if (current.expectedRevision == null) return
        editor.reload(load = { (repository.getById(current.id) ?: error("Missing source")).toDraft() }) { fresh ->
            updateState { copy(draft = fresh, conflict = false, error = null) }
        }
    }
    private fun publish(asCopy: Boolean) {
        val draft = uiState.value.draft ?: return
        val aggregate = draft.aggregate(asCopy)
        if (aggregate == null) { updateState { copy(error = "Zkontrolujte název, položky a číselné hodnoty. Návrh zůstává rozepsaný.") }; return }
        updateState { copy(error = null) }
        editor.publish(save = { repository.save(aggregate, if (asCopy) null else draft.expectedRevision) }) { result ->
            when (result) {
                is Result.Success -> sendEffect(PlanEditorContract.Effect.Saved(result.data.id))
                is Result.Error -> updateState { copy(conflict = result.error == PlanError.REVISION_CONFLICT,
                    error = if (result.error == PlanError.REVISION_CONFLICT) "Uložená verze se změnila. Načtěte aktuální nebo uložte svůj návrh jako kopii."
                    else "Nelze uložit: ${result.error}. Zkontrolujte také archivované odkazy.") }
            }
        }
    }
}
