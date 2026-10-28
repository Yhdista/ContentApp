package features.routines.presentation

import core.result.*
import core.viewmodel.BaseViewModel
import features.routines.api.*
import features.workout.domain.*
import features.exercises.api.*

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal class RoutineLibraryViewModel(private val id: String?, private val repository: RoutineRepository, private val catalogue: ExerciseCatalog, private val usage: RoutineUsage, private val start: StartRoutineWorkout) :
    BaseViewModel<RoutineLibraryContract.State, RoutineLibraryContract.Intent, RoutineLibraryContract.Effect>(RoutineLibraryContract.State()) {
    private var observation: Job? = null
    init { load() }
    override fun onOperationFailure(error: DataError.Local) { updateState { copy(isLoading = false, busy = false, error = "Nelze načíst nebo uložit data. Zkuste znovu.") } }
    override fun handleIntent(intent: RoutineLibraryContract.Intent) {
        if(uiState.value.busy) return
        when(intent) {
            RoutineLibraryContract.Intent.Retry -> load()
            RoutineLibraryContract.Intent.Start -> id?.let { command { handleStart(start.start(it)) } }
            RoutineLibraryContract.Intent.Resume -> sendEffect(RoutineLibraryContract.Effect.Workout)
            RoutineLibraryContract.Intent.Duplicate -> id?.let { command { when(val result = repository.duplicate(it)) {
                is Result.Success -> sendEffect(RoutineLibraryContract.Effect.Detail(result.data.id))
                is Result.Error -> updateState { copy(error = "Nelze vytvořit kopii: ${result.error}") }
            } } }
            RoutineLibraryContract.Intent.Archive -> updateState { copy(archiveConfirmation = true) }
            RoutineLibraryContract.Intent.CancelArchive -> updateState { copy(archiveConfirmation = false) }
            RoutineLibraryContract.Intent.ConfirmArchive -> uiState.value.detail?.let { item -> command {
                repository.archive(item.id,item.revision).show(); updateState { copy(archiveConfirmation = false) }
            } }
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
            is StartOutcome.Created -> sendEffect(RoutineLibraryContract.Effect.Workout)
            is StartOutcome.ResumedExisting -> updateState { copy(resumed = true) }
        }
    } }
    private fun load() {
        observation?.cancel()
        updateState { copy(isLoading = true, error = null) }
        observation = launchOperation {
            if(id != null) launch { usage.observeDependentPlanCount(id).collect { n -> updateState { copy(dependentPlans = n) } } }
            combine(repository.observeAll(true), catalogue.observeExercises()) { all, choices -> all to choices }.collect { (all, choices) ->
                val detail = all.find { it.id == id }
                val warning = when {
                    detail?.archived == true -> "Archivovanou rutinu nelze spustit. Můžete vytvořit kopii."
                    detail != null && (RoutineValidation.validate(detail) != null || detail.exercises.any { e -> choices.none { it.id == e.exerciseId && !it.archived } }) -> "Rutina vyžaduje opravu před startem: neplatný předpis nebo archivovaný/chybějící cvik."
                    else -> null
                }
                updateState { copy(items = all, detail = detail, choices = choices, warning = warning, isLoading = false,
                    error = if(id != null && detail == null) "Rutina nebyla nalezena." else null) }
            }
        }
    }
}
