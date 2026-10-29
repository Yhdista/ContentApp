package features.workout.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import core.common.formatCompactDecimal
import features.workout.domain.WorkoutSet
import features.workout.domain.WorkoutSetType
import ui.designsystem.atoms.*
import ui.designsystem.nucleus.AppStyle
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import ui.designsystem.nucleus.AppTheme

/** A set is a small form. Vertical fields retain their labels at narrow widths and large text. */
@Composable
internal fun SetRowItem(
    set: WorkoutSet,
    onToggleComplete: () -> Unit,
    onWeightChange: (Double) -> Unit,
    onRepsChange: (Int) -> Unit,
    onRpeChange: (Double?) -> Unit,
    onDelete: () -> Unit,
    onOpenPlateCalculator: () -> Unit = {},
    onSetTypeChange: (WorkoutSetType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var weightText by remember(set.id) { mutableStateOf(set.weightKg.formatCompactDecimal()) }
    var repsText by remember(set.id) { mutableStateOf(set.reps.toString()) }
    // Keep raw "-"/"1." while the ViewModel echoes the parsed numeric value.
    LaunchedEffect(set.weightKg) {
        if(weightText.toDoubleOrNull() != set.weightKg) weightText = set.weightKg.formatCompactDecimal()
    }
    LaunchedEffect(set.reps) {
        if(repsText.toIntOrNull() != set.reps) repsText = set.reps.toString()
    }
    var showOptions by remember { mutableStateOf(false) }
    val context = "série ${set.setNumber}"
    BoxWithConstraints(modifier.fillMaxWidth().testTag("set-${set.id}")) {
        val compact = maxWidth >= 280.dp && LocalDensity.current.fontScale <= 1.3f
        val weightField: @Composable (Modifier) -> Unit = { fieldModifier ->
            AppInput(if(compact) "Váha (kg)" else "Váha (kg), $context", weightText,
                modifier = fieldModifier.testTag("weight-${set.id}").semantics { contentDescription = "Váha (kg), $context" },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)) {
                weightText = it
                it.toDoubleOrNull()?.let(onWeightChange)
            }
        }
        val repsField: @Composable (Modifier) -> Unit = { fieldModifier ->
            AppInput(if(compact) "Opakování" else "Opakování, $context", repsText,
                modifier = fieldModifier.testTag("reps-${set.id}").semantics { contentDescription = "Opakování, $context" },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) {
                repsText = it
                it.toIntOrNull()?.let(onRepsChange)
            }
        }
        val rpeOptions = listOf(null, 7.0, 7.5, 8.0, 8.5, 9.0, 9.5, 10.0)
        val cycleRpe = { onRpeChange(rpeOptions[(rpeOptions.indexOf(set.rpe) + 1) % rpeOptions.size]) }
        val completion: @Composable () -> Unit = {
            AppToggle("Dokončeno, $context", set.isCompleted, onToggleComplete, Modifier.testTag("complete-${set.id}")) {
                AppText(if(compact) "✓" else if(set.isCompleted) "✓ Série dokončena" else "✓ Dokončit sérii",
                    color = if(set.isCompleted) AppStyle.colors.success.content else AppStyle.colors.onSurface)
            }
        }
        AppFrame(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if(compact) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        AppAction("${set.setNumber} · ${set.setType.shortLabel}", modifier = Modifier.semantics { contentDescription = "Možnosti $context, ${set.setType.displayName}" }) { showOptions = true }
                        Column(Modifier.weight(1f)) {
                            AppText("Předchozí", style = AppStyle.typography.labelSmall, color = AppStyle.colors.onSurfaceVariant)
                            AppText(set.previousPerformance ?: "—", style = AppStyle.typography.labelSmall)
                            if(set.isPersonalRecord && set.isCompleted && set.setType != WorkoutSetType.WARMUP) AppText("🏆 PR", color = AppStyle.colors.prGold, style = AppStyle.typography.labelSmall)
                        }
                        AppIconAction("RPE, $context: ${set.rpe ?: "neurčeno"}", cycleRpe) { AppText(set.rpe?.let { "@$it" } ?: "RPE", style = AppStyle.typography.labelSmall) }
                        completion()
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        weightField(Modifier.weight(1f))
                        repsField(Modifier.weight(1f))
                    }
                } else {
                    AppAction("${set.setNumber} · ${set.setType.displayName} · Možnosti série") { showOptions = true }
                    AppText("Předchozí: ${set.previousPerformance ?: "—"}", color = AppStyle.colors.onSurfaceVariant)
                    if(set.isPersonalRecord && set.isCompleted && set.setType != WorkoutSetType.WARMUP) AppText("🏆 PR · osobní rekord", color = AppStyle.colors.prGold)
                    weightField(Modifier)
                    repsField(Modifier)
                    AppAction("RPE, $context: ${set.rpe ?: "neurčeno"}", onClick = cycleRpe)
                    completion()
                }
            }
        }
    }
    if(showOptions) Dialog(onDismissRequest = { showOptions = false }) {
        AppCard(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            AppText("Možnosti série č. ${set.setNumber}", heading = true)
            AppText("Aktuálně: ${set.weightKg.formatCompactDecimal()} kg × ${set.reps} reps")
            AppAction("-2.5 kg") { onWeightChange((set.weightKg - 2.5).coerceAtLeast(0.0)) }
            AppAction("+2.5 kg") { onWeightChange(set.weightKg + 2.5) }
            AppAction("-1 rep") { onRepsChange((set.reps - 1).coerceAtLeast(0)) }
            AppAction("+1 rep") { onRepsChange(set.reps + 1) }
            AppAction("🏋️ Otevřít kalkulátor kotoučů pro tuto sérii") { showOptions = false; onOpenPlateCalculator() }
            AppText("TYP SÉRIE")
            WorkoutSetType.entries.forEach { type ->
                AppSelection(set.setType == type, { AppText(type.displayName) }, { onSetTypeChange(type); showOptions = false })
            }
            AppAction("Zavřít") { showOptions = false }
            AppButton({ showOptions = false; onDelete() }, variant = ActionVariant.Destructive) { AppText("Smazat sérii 🗑️") }
        }
    }
}

@Composable private fun SetPreview(dark: Boolean) { AppTheme(dark) {
    SetRowItem(WorkoutSet("preview", "session", "exercise", 1, 60.0, 8, isCompleted = true, previousPerformance = "57.5 kg × 8", isPersonalRecord = true), {}, {}, {}, {}, {}, modifier = Modifier.width(320.dp))
} }
@Preview @Composable private fun LightSetPreview() = SetPreview(false)
@Preview @Composable private fun DarkSetPreview() = SetPreview(true)
