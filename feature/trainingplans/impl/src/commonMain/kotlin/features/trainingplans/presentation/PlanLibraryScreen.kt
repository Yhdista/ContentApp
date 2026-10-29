package features.trainingplans.presentation

import ui.designsystem.atoms.*
import ui.designsystem.molecules.*
import ui.designsystem.templates.*

import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.items
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import core.viewmodel.rememberScreenViewModel
import core.viewmodel.CollectScreenEffects
import org.koin.core.parameter.parametersOf
import ui.designsystem.nucleus.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import features.trainingplans.api.*

@Composable fun PlanListRoot(onDetail: (String) -> Unit, onEditor: (String) -> Unit) {
    PlanLibraryRoot(null, onDetail, onEditor, {}, {}, {})
}
@Composable fun PlanDetailRoot(id: String, onDetail: (String) -> Unit, onEditor: (String) -> Unit,
    onWorkout: () -> Unit, onBack: () -> Unit, onRoutine: (String) -> Unit) {
    PlanLibraryRoot(id, onDetail, onEditor, onWorkout, onBack, onRoutine = onRoutine)
}
@Composable private fun PlanLibraryRoot(id: String?, onDetail: (String) -> Unit, onEditor: (String) -> Unit,
    onWorkout: () -> Unit, onBack: () -> Unit, onRoutine: (String) -> Unit = {}) {
    val vm: PlanLibraryViewModel = rememberScreenViewModel(parameters = { parametersOf(id) })
    val state by vm.uiState.collectAsStateWithLifecycle()
    CollectScreenEffects(vm.effect) { when(it) {
        PlanLibraryContract.Effect.Workout -> onWorkout()
        is PlanLibraryContract.Effect.Detail -> onDetail(it.id)
    } }
    PlanLibraryScreen(state, id != null, vm::onIntent, onDetail, onEditor, onBack, onRoutine = onRoutine)
}
@Composable internal fun PlanLibraryScreen(state: PlanLibraryContract.State, detailMode: Boolean,
    onIntent: (PlanLibraryContract.Intent) -> Unit, onDetail: (String) -> Unit,
    onEditor: (String) -> Unit, onBack: () -> Unit, onRoutine: (String) -> Unit = {}) {
    AppPage(if(detailMode) state.detail?.name ?: "Detail" else "🗓️ Plány", if(detailMode) onBack else null) {
        if(state.isLoading) item { AppLoading() }
        state.error?.let { item { EditorNotice(it) { onIntent(PlanLibraryContract.Intent.Retry) } } }
        state.warning?.let { item { AppCard { AppText(it) } } }
        if(state.resumed) item { AppCard { AppText("Již probíhá trénink. Pokračujte v existujícím; nový nebyl vytvořen."); AppAction("Pokračovat v existujícím") { onIntent(PlanLibraryContract.Intent.Resume) } } }
        if(!detailMode) {
            item { AppText(state.items.find { it.id == state.activeId }?.let { "🎯 Aktuální plán: ${it.name}" } ?: "🎯 Žádný aktuálně vybraný plán") }
            item { AppAction("Nový nebo rozepsaný plán") { onEditor("new") } }
            if(!state.isLoading && state.error == null && state.items.isEmpty()) item { EmptyLibrary("Zatím nemáte plán. Sestavte cyklus z uložených rutin.") }
            items(state.items, key = { it.id }) { item -> AppCard {
                AppText(item.name, true); AppText(item.notes)
                AppText(if(item.archived) "🗃️ Archivováno" else "Revize ${item.revision}")
                AppAction("Otevřít") { onDetail(item.id) }
            } }
        } else {
            state.detail?.let { detail ->
                item { AppText(detail.notes) }
                item { AppAction("Spustit příští den", !detail.archived && !state.busy && state.progress?.needsReview == false && state.progress.nextSlotId != null) { onIntent(PlanLibraryContract.Intent.Start) } }
                item { AppAction("Upravit / pokračovat v návrhu", !detail.archived && !state.busy) { onEditor("edit:" + detail.id) } }
                item { AppAction("Duplikovat", !state.busy) { onIntent(PlanLibraryContract.Intent.Duplicate) } }
                item { AppAction("Archivovat", !detail.archived && !state.busy) { onIntent(PlanLibraryContract.Intent.Archive) } }

                item { AppAction(if(state.activeId == detail.id) "Zrušit aktuální výběr" else "Zvolit jako aktuální plán", !detail.archived && !state.busy) {
                    onIntent(if(state.activeId == detail.id) PlanLibraryContract.Intent.ClearActive else PlanLibraryContract.Intent.SelectActive)
                } }
                item { AppAction("Přeskočit den", !detail.archived && state.progress?.needsReview == false && !state.busy) { onIntent(PlanLibraryContract.Intent.Skip) } }
                items(detail.slots, key = { it.id }) { slot ->
                    val routine = state.choices.find { it.id == slot.routineId }
                    AppCard {
                        AppText((if(state.progress?.nextSlotId == slot.id) "→ " else "") + slot.label, true)
                        AppText((routine?.name ?: "Chybějící rutina") + if(routine?.archived == true) " · archivováno" else "")
                        if(state.progress?.needsReview == true) AppAction("Potvrdit jako další slot", !detail.archived && routine?.archived == false && !state.busy) { onIntent(PlanLibraryContract.Intent.ConfirmNext(slot.id)) }
                        AppAction("Otevřít rutinu") { onRoutine(slot.routineId) }
                        AppAction("Spustit samostatně · bez posunu plánu", routine?.archived == false && !state.busy) { onIntent(PlanLibraryContract.Intent.StartStandalone(slot.routineId)) }
                    }
                }

            }
        }
    }
    if(state.archiveConfirmation) AppConfirm("Archivovat?", "Archivace zablokuje budoucí starty. Historie a již spuštěný workout se nezmění.", "Archivovat", "Pokračovat v úpravách",
        { onIntent(PlanLibraryContract.Intent.ConfirmArchive) }, { onIntent(PlanLibraryContract.Intent.CancelArchive) }, !state.busy, confirmVariant = ActionVariant.Destructive)
}
@Preview @Composable private fun PlanLibraryLightPreview() { AppTheme(darkTheme = false) {
    PlanLibraryScreen(PlanLibraryContract.State(isLoading = false, items = listOf(TrainingPlan("preview", "Síla · Den A", "Postupně, s rezervou", slots = emptyList()))), false, {}, {}, {}, {})
} }
@Preview @Composable private fun PlanLibraryDarkPreview() { AppTheme(darkTheme = true) {
    PlanLibraryScreen(PlanLibraryContract.State(isLoading = false, error = "Data se nepodařilo načíst."), false, {}, {}, {}, {})
} }
