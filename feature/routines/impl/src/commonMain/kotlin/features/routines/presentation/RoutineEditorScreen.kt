package features.routines.presentation

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import core.viewmodel.*
import org.koin.core.parameter.parametersOf
import org.jetbrains.compose.ui.tooling.preview.Preview
import ui.designsystem.nucleus.*
import ui.designsystem.molecules.*
import features.routines.api.*
import features.workout.domain.WorkoutSetType

@Composable fun RoutineEditorRoot(draftId: String, onSaved: (String) -> Unit, onBack: () -> Unit) {
    val vm: RoutineEditorViewModel = rememberScreenViewModel(parameters = { parametersOf(draftId) })
    val state by vm.uiState.collectAsStateWithLifecycle()
    EditorBackHandler(true) { vm.onIntent(RoutineEditorContract.Intent.Back) }
    CollectScreenEffects(vm.effect) { when(it) {
        is RoutineEditorContract.Effect.Saved -> onSaved(it.id)
        RoutineEditorContract.Effect.Back -> onBack()
    } }
    RoutineEditorScreen(state, vm::onIntent)
}
@Composable internal fun RoutineEditorScreen(state: RoutineEditorContract.State, onIntent: (RoutineEditorContract.Intent) -> Unit) {
    val enabled = !state.isSaving
    fun edit(transform: (RoutineDraft) -> RoutineDraft) = onIntent(RoutineEditorContract.Intent.Mutate(transform))
    AppPage("📋 Návrh rutiny", { onIntent(RoutineEditorContract.Intent.Back) }) {
        if(state.isLoading) item { AppLoading() }
        item { AppText("💾 Rozepsáno · automaticky ukládáno v tomto zařízení. Dokončete návrh tlačítkem Uložit.") }
        state.error?.let { item { EditorNotice(it) { onIntent(RoutineEditorContract.Intent.Retry) } } }
        if(state.draft == null && state.canDiscard && state.error != null) item { AppAction("Zahodit nečitelný návrh", enabled) { onIntent(RoutineEditorContract.Intent.Discard) } }
        if(state.conflict) item { AppCard {
            AppAction("Načíst aktuální verzi", enabled) { onIntent(RoutineEditorContract.Intent.Reload) }
            AppAction("Uložit můj návrh jako novou kopii", enabled) { onIntent(RoutineEditorContract.Intent.SaveCopy) }
        } }
        state.draft?.let { draft ->
            item { AppInput("Název", draft.name, enabled) { value -> edit { it.copy(name = value) } } }
            item { AppInput("Poznámka", draft.notes, enabled) { value -> edit { it.copy(notes = value) } } }

            item { AppText("Dotčené plány: ${state.dependentPlans}. Uložení změní jejich budoucí starty.") }
            item { AppChoice("Přidat cvik", state.choices.filter { !it.archived }.map { it.id to it.name }, enabled) { id ->
                val choice = state.choices.first { it.id == id }
                edit { it.copy(exercises = it.exercises + EntryDraft(exerciseId = id, rest = choice.defaultRestSeconds.toString())) }
            } }
            items(draft.exercises, key = { it.id }) { entry ->
                fun change(transform: (EntryDraft) -> EntryDraft) = edit { current -> current.copy(exercises = current.exercises.map { if(it.id == entry.id) transform(it) else it }) }
                AppCard {
                    val selected = state.choices.find { it.id == entry.exerciseId }
                    AppText((selected?.name ?: "Chybějící cvik") + if(selected?.archived == true) " · archivováno" else "", true)
                    AppChoice("Nahradit cvik", state.choices.filter { !it.archived }.map { it.id to it.name }, enabled) { id -> change { it.copy(exerciseId = id) } }
                    AppInput("Pauza (0–3600 s)", entry.rest, enabled) { value -> change { it.copy(rest = value) } }
                    RowActions(enabled, { edit { it.copy(exercises = it.exercises.moved(it.exercises.firstOrNull { e -> e.id == entry.id } ?: entry, -1)) } },
                        { edit { it.copy(exercises = it.exercises.moved(it.exercises.firstOrNull { e -> e.id == entry.id } ?: entry, 1)) } },
                        { edit { current -> current.copy(exercises = current.exercises.filterNot { it.id == entry.id }) } })
                    entry.legacyValues?.let { old ->
                        AppText("Původní hodnoty: ${old.targetSets} sérií, ${old.defaultWeightKg} kg × ${old.defaultReps}. Opravte předpis před startem.")
                        AppAction("Potvrdit opravený předpis", enabled) { change { it.copy(legacyValues = null) } }
                    }
                    entry.sets.forEachIndexed { index, set ->
                        fun setChange(transform: (SetDraft) -> SetDraft) = change { current -> current.copy(sets = current.sets.map { if(it.id == set.id) transform(it) else it }) }
                        AppText("Série ${index + 1}")
                        AppInput("Váha (kg)", set.weight, enabled) { value -> setChange { it.copy(weight = value) } }
                        AppInput("Opakování (1–999)", set.reps, enabled) { value -> setChange { it.copy(reps = value) } }
                        AppChoice(set.type.displayName, WorkoutSetType.entries.map { it.name to it.displayName }, enabled) { value -> setChange { it.copy(type = WorkoutSetType.valueOf(value)) } }
                        RowActions(enabled, { change { it.copy(sets = it.sets.moved(it.sets.firstOrNull { s -> s.id == set.id } ?: set, -1)) } },
                            { change { it.copy(sets = it.sets.moved(it.sets.firstOrNull { s -> s.id == set.id } ?: set, 1)) } },
                            { change { current -> current.copy(sets = current.sets.filterNot { it.id == set.id }) } })
                    }
                    AppAction("Přidat sérii", enabled && entry.sets.size < 20) { change { it.copy(sets = it.sets + (it.sets.lastOrNull()?.copy(id = newId()) ?: SetDraft())) } }
                    AppAction("Přidat 3 stejné série", enabled && entry.sets.size <= 17) { change { current -> current.copy(sets = current.sets + List(3) { current.sets.lastOrNull()?.copy(id = newId()) ?: SetDraft() }) } }
                }
            }

            item { AppAction(if(state.isSaving) "Ukládání…" else "Uložit", enabled) { onIntent(RoutineEditorContract.Intent.Save) } }
        }
    }
    if(state.confirmExit) AppConfirm("Návrh není publikován", "Pokračujte v úpravách, nebo návrh zahoďte. Uložená rutina se nezmění.", "Zahodit návrh",
        { onIntent(RoutineEditorContract.Intent.Discard) }, { onIntent(RoutineEditorContract.Intent.Continue) }, enabled)
}
private fun <T> List<T>.moved(item: T, delta: Int): List<T> {
    val from = indexOf(item); val to = from + delta
    if(from < 0 || to !in indices) return this
    return toMutableList().apply { removeAt(from); add(to, item) }
}
@Composable private fun RowActions(enabled: Boolean, up: () -> Unit, down: () -> Unit, remove: () -> Unit) {
    // Wrap naturally on narrow displays and at large font sizes.
    Column { AppAction("Posunout výše", enabled, up); AppAction("Posunout níže", enabled, down); AppAction("Odebrat", enabled, remove) }
}
@Preview @Composable private fun RoutineEditorLightPreview() { AppTheme(darkTheme = false) { RoutineEditorScreen(RoutineEditorContract.State(draft = RoutineDraft(name = "Síla A"), isLoading = false)) {} } }
@Preview @Composable private fun RoutineEditorDarkPreview() { AppTheme(darkTheme = true) { RoutineEditorScreen(RoutineEditorContract.State(draft = RoutineDraft(name = "Síla A"), isLoading = false)) {} } }
