package features.routines.presentation

import androidx.lifecycle.viewModelScope
import core.result.DataError
import core.result.Result
import core.result.safeLocalCall
import core.result.onFailure
import core.viewmodel.BaseViewModel
import core.viewmodel.DurableEditorController
import features.routines.api.*
import features.exercises.api.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class RoutineEditorViewModel(
    private val editor: DurableEditorController<RoutineDraft>, private val repository: RoutineRepository,
    private val catalogue: ExerciseCatalog, private val usage: RoutineUsage,
) : BaseViewModel<RoutineEditorContract.State, RoutineEditorContract.Intent, RoutineEditorContract.Effect>(RoutineEditorContract.State()) {
    private var choicesJob: Job? = null
    init {
        editor.bind(viewModelScope, ::onOperationFailure) { saving -> updateState { copy(isSaving = saving) } }
        load()
        observeChoices()
    }
    override fun onOperationFailure(error: DataError.Local) {
        updateState { copy(isLoading = false, error = "Úložiště není dostupné. Zkuste akci znovu; návrh zůstává zachován.") }
    }
    override fun handleIntent(intent: RoutineEditorContract.Intent) {
        if (intent == RoutineEditorContract.Intent.RetryChoices) { observeChoices(); return }
        if (editor.isBusy) return
        when (intent) {
            is RoutineEditorContract.Intent.Mutate -> uiState.value.draft?.let { handleIntent(RoutineEditorContract.Intent.Edit(intent.transform(it))) }
            is RoutineEditorContract.Intent.Edit -> {
                if (uiState.value.draft == null) return
                updateState { copy(draft = intent.draft, error = null) }
                editor.edit(intent.draft)
            }
            RoutineEditorContract.Intent.Save -> publish(false)
            RoutineEditorContract.Intent.SaveCopy -> publish(true)
            RoutineEditorContract.Intent.Reload -> reload()
            RoutineEditorContract.Intent.RetryChoices -> Unit
            RoutineEditorContract.Intent.Retry -> if (uiState.value.draft == null) load() else {
                observeChoices()
                editor.edit(uiState.value.draft!!) { updateState { copy(error = null) } }
            }
            RoutineEditorContract.Intent.Back -> {
                if (uiState.value.draft == null) sendEffect(RoutineEditorContract.Effect.Back)
                else { updateState { copy(confirmExit = true) }; editor.barrier() }
            }
            RoutineEditorContract.Intent.Continue -> updateState { copy(confirmExit = false) }
            RoutineEditorContract.Intent.Discard -> editor.discard { sendEffect(RoutineEditorContract.Effect.Back) }
        }
    }
    private fun load() {
        updateState { copy(isLoading = true, error = null) }
        editor.open(onOpened = { draft ->
            updateState { copy(draft = draft, isLoading = false) }
            launchOperation { usage.observeDependentPlanCount(draft.id).collect { n -> updateState { copy(dependentPlans = n) } } }
        }, onLeaseAcquired = { updateState { copy(canDiscard = true) } }, onAlreadyOpen = {
            updateState { copy(isLoading = false, error = "Tento návrh je otevřený v jiné obrazovce. Vraťte se do původního editoru; po jeho zavření můžete zkusit znovu.") }
        })
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
                is Result.Success -> sendEffect(RoutineEditorContract.Effect.Saved(result.data.id))
                is Result.Error -> updateState { copy(conflict = result.error == RoutineError.REVISION_CONFLICT,
                    error = if (result.error == RoutineError.REVISION_CONFLICT) "Uložená verze se změnila. Načtěte aktuální nebo uložte svůj návrh jako kopii."
                    else "Nelze uložit: ${result.error}. Zkontrolujte také archivované odkazy.") }
            }
        }
    }
}
