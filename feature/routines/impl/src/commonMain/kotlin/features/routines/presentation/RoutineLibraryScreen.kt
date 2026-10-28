package features.routines.presentation

import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.items
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import core.viewmodel.rememberScreenViewModel
import core.viewmodel.CollectScreenEffects
import org.koin.core.parameter.parametersOf
import ui.designsystem.nucleus.*
import ui.designsystem.molecules.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import features.routines.api.*

@Composable fun RoutineListRoot(onDetail: (String) -> Unit, onEditor: (String) -> Unit) {
    RoutineLibraryRoot(null, onDetail, onEditor, {}, {})
}
@Composable fun RoutineDetailRoot(id: String, onDetail: (String) -> Unit, onEditor: (String) -> Unit,
    onWorkout: () -> Unit, onBack: () -> Unit) {
    RoutineLibraryRoot(id, onDetail, onEditor, onWorkout, onBack)
}
@Composable private fun RoutineLibraryRoot(id: String?, onDetail: (String) -> Unit, onEditor: (String) -> Unit,
    onWorkout: () -> Unit, onBack: () -> Unit) {
    val vm: RoutineLibraryViewModel = rememberScreenViewModel(parameters = { parametersOf(id) })
    val state by vm.uiState.collectAsStateWithLifecycle()
    CollectScreenEffects(vm.effect) { when(it) {
        RoutineLibraryContract.Effect.Workout -> onWorkout()
        is RoutineLibraryContract.Effect.Detail -> onDetail(it.id)
    } }
    RoutineLibraryScreen(state, id != null, vm::onIntent, onDetail, onEditor, onBack)
}
@Composable internal fun RoutineLibraryScreen(state: RoutineLibraryContract.State, detailMode: Boolean,
    onIntent: (RoutineLibraryContract.Intent) -> Unit, onDetail: (String) -> Unit,
    onEditor: (String) -> Unit, onBack: () -> Unit) {
    AppPage(if(detailMode) state.detail?.name ?: "Detail" else "📋 Rutiny", if(detailMode) onBack else null) {
        if(state.isLoading) item { AppLoading() }
        state.error?.let { item { EditorNotice(it) { onIntent(RoutineLibraryContract.Intent.Retry) } } }
        state.warning?.let { item { AppCard { AppText(it) } } }
        if(state.resumed) item { AppCard { AppText("Již probíhá trénink. Pokračujte v existujícím; nový nebyl vytvořen."); AppAction("Pokračovat v existujícím") { onIntent(RoutineLibraryContract.Intent.Resume) } } }
        if(!detailMode) {
            item { AppAction("Nový nebo rozepsaný návrh rutiny") { onEditor("new") } }
            if(!state.isLoading && state.error == null && state.items.isEmpty()) item { EmptyLibrary("Zatím nemáte rutiny. Přidejte cviky a jednotlivé série.") }
            items(state.items, key = { it.id }) { item -> AppCard {
                AppText(item.name, true); AppText(item.notes)
                AppText(if(item.archived) "🗃️ Archivováno" else "Revize ${item.revision}")
                AppAction("Otevřít") { onDetail(item.id) }
            } }
        } else {
            state.detail?.let { detail ->
                item { AppText(detail.notes) }
                item { AppAction("Spustit rutinu", !detail.archived && !state.busy && state.warning == null) { onIntent(RoutineLibraryContract.Intent.Start) } }
                item { AppAction("Upravit / pokračovat v návrhu", !detail.archived && !state.busy) { onEditor("edit:" + detail.id) } }
                item { AppAction("Duplikovat", !state.busy) { onIntent(RoutineLibraryContract.Intent.Duplicate) } }
                item { AppAction("Archivovat", !detail.archived && !state.busy) { onIntent(RoutineLibraryContract.Intent.Archive) } }

                item { AppText("Dotčené plány: ${state.dependentPlans}. Změny ovlivní budoucí starty, historie zůstává zachována.") }
                items(detail.exercises, key = { it.id }) { entry ->
                    AppCard {
                        AppText(state.choices.find { it.id == entry.exerciseId }?.name ?: "Chybějící cvik", true)
                        AppText("Pauza ${entry.restSeconds} s")
                        entry.sets.forEach { AppText("${it.weightKg} kg × ${it.reps} · ${it.setType.displayName}") }
                        entry.legacyValues?.let { AppText("Převedený předpis: ${it.targetSets} sérií · ${it.defaultWeightKg} kg × ${it.defaultReps}. Původní hodnoty jsou zachovány.") }
                    }
                }

            }
        }
    }
    if(state.archiveConfirmation) AppConfirm("Archivovat?", "Archivace zablokuje budoucí starty. Historie a již spuštěný workout se nezmění. Dotčené plány: ${state.dependentPlans}.", "Archivovat",
        { onIntent(RoutineLibraryContract.Intent.ConfirmArchive) }, { onIntent(RoutineLibraryContract.Intent.CancelArchive) }, !state.busy)
}
@Preview @Composable private fun RoutineLibraryLightPreview() { AppTheme(darkTheme = false) {
    RoutineLibraryScreen(RoutineLibraryContract.State(isLoading = false, items = listOf(Routine("preview", "Síla · Den A", "Postupně, s rezervou", exercises = emptyList()))), false, {}, {}, {}, {})
} }
@Preview @Composable private fun RoutineLibraryDarkPreview() { AppTheme(darkTheme = true) {
    RoutineLibraryScreen(RoutineLibraryContract.State(isLoading = false, error = "Data se nepodařilo načíst."), false, {}, {}, {}, {})
} }
