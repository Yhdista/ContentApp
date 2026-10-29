package features.trainingplans.presentation

import ui.designsystem.atoms.*
import ui.designsystem.molecules.*
import ui.designsystem.templates.*

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import core.viewmodel.*
import org.koin.core.parameter.parametersOf
import org.jetbrains.compose.ui.tooling.preview.Preview
import ui.designsystem.nucleus.*
import features.trainingplans.api.*
import features.workout.domain.WorkoutSetType

@Composable fun PlanEditorRoot(draftId: String, onSaved: (String) -> Unit, onBack: () -> Unit) {
    val vm: PlanEditorViewModel = rememberScreenViewModel(parameters = { parametersOf(draftId) })
    val state by vm.uiState.collectAsStateWithLifecycle()
    EditorBackHandler(true) { vm.onIntent(PlanEditorContract.Intent.Back) }
    CollectScreenEffects(vm.effect) { when(it) {
        is PlanEditorContract.Effect.Saved -> onSaved(it.id)
        PlanEditorContract.Effect.Back -> onBack()
    } }
    PlanEditorScreen(state, vm::onIntent)
}
@Composable internal fun PlanEditorScreen(state: PlanEditorContract.State, onIntent: (PlanEditorContract.Intent) -> Unit) {
    val enabled = !state.isSaving
    fun edit(transform: (PlanDraft) -> PlanDraft) = onIntent(PlanEditorContract.Intent.Mutate(transform))
    AppPage("🗓️ Návrh plánu", { onIntent(PlanEditorContract.Intent.Back) }) {
        if(state.isLoading) item { AppLoading() }
        item { AppText("💾 Rozepsáno · automaticky ukládáno v tomto zařízení. Dokončete návrh tlačítkem Uložit.") }
        state.choicesError?.let { item { EditorNotice(it) { onIntent(PlanEditorContract.Intent.RetryChoices) } } }
        state.error?.let { item { EditorNotice(it) { onIntent(PlanEditorContract.Intent.Retry) } } }
        if(state.draft == null && state.canDiscard && state.error != null) item { AppAction("Zahodit nečitelný návrh", enabled) { onIntent(PlanEditorContract.Intent.Discard) } }
        if(state.conflict) item { AppCard {
            AppAction("Načíst aktuální verzi", enabled) { onIntent(PlanEditorContract.Intent.Reload) }
            AppAction("Uložit můj návrh jako novou kopii", enabled) { onIntent(PlanEditorContract.Intent.SaveCopy) }
        } }
        state.draft?.let { draft ->
            item { AppInput("Název", draft.name, enabled) { value -> edit { it.copy(name = value) } } }
            item { AppInput("Poznámka", draft.notes, enabled, singleLine = false) { value -> edit { it.copy(notes = value) } } }

            item { AppChoice("Přidat slot s rutinou", state.choices.filter { !it.archived }.map { it.id to it.name }, enabled && draft.slots.size < 31) { id ->
                edit { it.copy(slots = it.slots + PlanSlot(newId(), "Den ${it.slots.size + 1}", id)) }
            } }
            items(draft.slots, key = { it.id }) { slot ->
                fun change(transform: (PlanSlot) -> PlanSlot) = edit { current -> current.copy(slots = current.slots.map { if(it.id == slot.id) transform(it) else it }) }
                AppCard {
                    val selected = state.choices.find { it.id == slot.routineId }
                    AppText((selected?.name ?: "Chybějící rutina") + if(selected?.archived == true) " · archivováno" else "", true)
                    AppInput("Označení slotu", slot.label, enabled) { value -> change { it.copy(label = value) } }
                    AppChoice("Nahradit rutinu", state.choices.filter { !it.archived }.map { it.id to it.name }, enabled) { value -> change { it.copy(routineId = value) } }
                    ReorderActions(enabled, { edit { it.copy(slots = it.slots.moved(it.slots.firstOrNull { s -> s.id == slot.id } ?: slot, -1)) } },
                        { edit { it.copy(slots = it.slots.moved(it.slots.firstOrNull { s -> s.id == slot.id } ?: slot, 1)) } },
                        { edit { current -> current.copy(slots = current.slots.filterNot { it.id == slot.id }) } })
                }
            }

            item { AppAction(if(state.isSaving) "Ukládání…" else "Uložit", enabled, variant = ActionVariant.Primary) { onIntent(PlanEditorContract.Intent.Save) } }
        }
    }
    if(state.confirmExit) AppConfirm("Návrh není publikován", "Pokračujte v úpravách, nebo návrh zahoďte. Uložená definice plánu se nezmění.", "Zahodit návrh", "Pokračovat v úpravách",
        { onIntent(PlanEditorContract.Intent.Discard) }, { onIntent(PlanEditorContract.Intent.Continue) }, enabled, confirmVariant = ActionVariant.Destructive)
}
@Preview @Composable private fun PlanEditorLightPreview() { AppTheme(darkTheme = false) { PlanEditorScreen(PlanEditorContract.State(draft = PlanDraft(name = "Síla A"), isLoading = false)) {} } }
@Preview @Composable private fun PlanEditorDarkPreview() { AppTheme(darkTheme = true) { PlanEditorScreen(PlanEditorContract.State(draft = PlanDraft(name = "Síla A"), isLoading = false)) {} } }
