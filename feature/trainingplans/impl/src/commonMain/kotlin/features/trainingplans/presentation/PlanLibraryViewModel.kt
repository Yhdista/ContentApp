package features.trainingplans.presentation

import core.result.*
import core.viewmodel.BaseViewModel
import features.trainingplans.api.*
import features.workout.domain.*
import features.routines.api.*

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal class PlanLibraryViewModel(private val id: String?, private val repository: TrainingPlanRepository, private val routines: RoutineRepository, private val start: StartPlanWorkout, private val startRoutine: StartRoutineWorkout) :
    BaseViewModel<PlanLibraryContract.State, PlanLibraryContract.Intent, PlanLibraryContract.Effect>(PlanLibraryContract.State()) {
    private var observation: Job? = null
    init { load() }
    override fun onOperationFailure(error: DataError.Local) { updateState { copy(isLoading = false, busy = false, error = "Nelze načíst nebo uložit data. Zkuste znovu.") } }
    override fun handleIntent(intent: PlanLibraryContract.Intent) {
        if(uiState.value.busy) return
        when(intent) {
            PlanLibraryContract.Intent.Retry -> load()
            PlanLibraryContract.Intent.Start -> id?.let { command { handleStart(start.start(it)) } }
            PlanLibraryContract.Intent.Resume -> sendEffect(PlanLibraryContract.Effect.Workout)
            PlanLibraryContract.Intent.Duplicate -> id?.let { command { when(val result = repository.duplicate(it)) {
                is Result.Success -> sendEffect(PlanLibraryContract.Effect.Detail(result.data.id))
                is Result.Error -> updateState { copy(error = "Nelze vytvořit kopii: ${result.error}") }
            } } }
            PlanLibraryContract.Intent.Archive -> updateState { copy(archiveConfirmation = true) }
            PlanLibraryContract.Intent.CancelArchive -> updateState { copy(archiveConfirmation = false) }
            PlanLibraryContract.Intent.ConfirmArchive -> uiState.value.detail?.let { item -> command {
                repository.archive(item.id,item.revision).show(); updateState { copy(archiveConfirmation = false) }
            } }
            PlanLibraryContract.Intent.SelectActive -> command { repository.selectActive(id).show() }
            PlanLibraryContract.Intent.ClearActive -> command { repository.selectActive(null).show() }
            PlanLibraryContract.Intent.Skip -> uiState.value.progress?.let { p -> command { repository.skipNext(p.planId, p.progressRevision).show() } }
            is PlanLibraryContract.Intent.ConfirmNext -> uiState.value.progress?.let { p -> command { repository.confirmNext(p.planId, intent.slotId, p.progressRevision).show() } }
            is PlanLibraryContract.Intent.StartStandalone -> command { handleStart(startRoutine.start(intent.routineId)) }
        }
    }
    private fun command(block: suspend () -> Unit) {
        updateState { copy(busy = true, error = null) }
        launchOperation { block(); updateState { copy(busy = false) } }
    }
    private fun <T, E: RootError> Result<T,E>.show() { if(this is Result.Error) updateState { copy(error = "Akce se nezdařila: $error") } }
    private fun handleStart(result: Result<StartOutcome,WorkoutError>) { when(result) {
        is Result.Error -> updateState { copy(error = "Nelze spustit: ${result.error}. Zkontrolujte předpis a archivované odkazy.") }
        is Result.Success -> when(result.data) {
            is StartOutcome.Created -> sendEffect(PlanLibraryContract.Effect.Workout)
            is StartOutcome.ResumedExisting -> updateState { copy(resumed = true) }
        }
    } }
    private fun load() {
        observation?.cancel()
        updateState { copy(isLoading = true, error = null) }
        observation = launchOperation {

            combine(repository.observeAll(true), routines.observeAll(true), repository.observeActive(),
                if(id == null) flowOf(null) else repository.observeProgress(id)) { all, choices, active, progress ->
                val detail = all.find { it.id == id }
                val warning = when {
                    detail?.archived == true -> "Archivovaný plán nelze spustit."
                    progress?.needsReview == true -> "Postup vyžaduje kontrolu. Výslovně vyberte další slot."
                    detail != null && detail.slots.any { s -> choices.none { it.id == s.routineId && !it.archived } } -> "Plán obsahuje archivovanou nebo chybějící rutinu. Nahraďte ji v editoru nebo přeskočte aktuální den."
                    else -> null
                }
                uiState.value.copy(items = all, detail = detail, choices = choices, progress = progress, activeId = active?.id,
                    warning = warning, isLoading = false, error = if(id != null && detail == null) "Plán nebyl nalezen." else null)
            }.collect { value -> updateState { value } }
        }
    }
}
