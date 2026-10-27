package features.workout.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import core.common.formatCleanWeight
import features.workout.domain.WorkoutSet
import features.workout.domain.WorkoutSetType
import ui.designsystem.nucleus.AppSpacing
import ui.designsystem.nucleus.isLight
import ui.designsystem.nucleus.prGold
import ui.designsystem.nucleus.statusSynced

@Composable
fun SetRowItem(
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
    val colorScheme = MaterialTheme.colorScheme
    var weightText by remember(set.weightKg) {
        mutableStateOf(set.weightKg.formatCleanWeight())
    }
    var repsText by remember(set.reps) {
        mutableStateOf(set.reps.toString())
    }
    var showOptionsDialog by remember { mutableStateOf(false) }

    val rowBackground = if (set.isCompleted) {
        colorScheme.surface.copy(alpha = 0.6f)
    } else {
        colorScheme.surface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(rowBackground)
            .border(
                width = 1.dp,
                color = if (set.isCompleted) colorScheme.statusSynced.copy(alpha = 0.4f) else colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Číslo a typ série (kliknutím se otevřou možnosti: rychlá tlačítka, kalkulátor, typ série, smazat)
        val badgeVisuals = set.setType.toBadgeVisuals(
            setNumber = set.setNumber,
            isCompleted = set.isCompleted,
            isLight = colorScheme.isLight
        )

        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(badgeVisuals.backgroundColor)
                .clickable(role = Role.Button) { showOptionsDialog = true },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badgeVisuals.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = badgeVisuals.contentColor
            )
        }

        // 2. Předchozí výkon (např. 80 kg × 8) + PR odznak
        Column(
            modifier = Modifier.width(64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = set.previousPerformance ?: "—",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            if (set.isPersonalRecord && set.isCompleted && set.setType != WorkoutSetType.WARMUP) {
                Text(
                    text = "🏆 PR",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.prGold,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp
                )
            }
        }

        // 3. Vstup pro Váhu (kg)
        Box(
            modifier = Modifier
                .width(62.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colorScheme.surfaceVariant)
                .border(1.dp, colorScheme.outline, RoundedCornerShape(8.dp))
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = weightText,
                onValueChange = { newText ->
                    weightText = newText
                    newText.toDoubleOrNull()?.let { onWeightChange(it) }
                },
                singleLine = true,
                textStyle = TextStyle(
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                cursorBrush = SolidColor(colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 4. Vstup pro Opakování (reps)
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colorScheme.surfaceVariant)
                .border(1.dp, colorScheme.outline, RoundedCornerShape(8.dp))
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = repsText,
                onValueChange = { newText ->
                    repsText = newText
                    newText.toIntOrNull()?.let { onRepsChange(it) }
                },
                singleLine = true,
                textStyle = TextStyle(
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                cursorBrush = SolidColor(colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 5. RPE cyklovač (např. 7.0..10.0) s garantovanou 48dp dotykovou plochou
        val rpeOptions = listOf(null, 7.0, 7.5, 8.0, 8.5, 9.0, 9.5, 10.0)
        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .width(44.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (set.rpe != null) colorScheme.primaryContainer.copy(alpha = 0.7f)
                    else colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
                .clickable(role = Role.Button) {
                    val currentIndex = rpeOptions.indexOf(set.rpe)
                    val nextIndex = (currentIndex + 1) % rpeOptions.size
                    onRpeChange(rpeOptions[nextIndex])
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = set.rpe?.let { "@$it" } ?: "RPE",
                style = MaterialTheme.typography.labelSmall,
                color = if (set.rpe != null) colorScheme.primary else colorScheme.onSurfaceVariant,
                fontWeight = if (set.rpe != null) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp
            )
        }

        // 6. Velké odškrtávací tlačítko (min 48 dp dotykový terč!)
        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (set.isCompleted) colorScheme.statusSynced else colorScheme.surfaceVariant)
                .border(
                    width = 1.5.dp,
                    color = if (set.isCompleted) colorScheme.statusSynced else colorScheme.outline,
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable(
                    role = Role.Button,
                    onClick = onToggleComplete
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✓",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (set.isCompleted) Color.White else colorScheme.onSurfaceVariant
            )
        }
    }

    if (showOptionsDialog) {
        Dialog(onDismissRequest = { showOptionsDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colorScheme.surface)
                    .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(AppSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                Text(
                    text = "Možnosti série č. ${set.setNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Text(
                    text = "Aktuálně: ${set.weightKg.formatCleanWeight()} kg × ${set.reps} reps",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )

                // Rychlá tlačítka pro úpravu váhy a opakování + Kalkulátor kotoučů
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    QuickStepButton(
                        label = "-2.5 kg",
                        modifier = Modifier.weight(1f),
                        onClick = { onWeightChange((set.weightKg - 2.5).coerceAtLeast(0.0)) }
                    )
                    QuickStepButton(
                        label = "+2.5 kg",
                        modifier = Modifier.weight(1f),
                        onClick = { onWeightChange(set.weightKg + 2.5) }
                    )
                    QuickStepButton(
                        label = "-1 rep",
                        modifier = Modifier.weight(1f),
                        onClick = { onRepsChange((set.reps - 1).coerceAtLeast(0)) }
                    )
                    QuickStepButton(
                        label = "+1 rep",
                        modifier = Modifier.weight(1f),
                        onClick = { onRepsChange(set.reps + 1) }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .border(1.dp, colorScheme.primary, RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button) {
                            showOptionsDialog = false
                            onOpenPlateCalculator()
                        }
                        .padding(vertical = AppSpacing.sm),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🏋️ Otevřít kalkulátor kotoučů pro tuto sérii",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "TYP SÉRIE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurfaceVariant
                )

                // Výběr typu série: Normal, Warmup (W), Drop set (D), Failure (F)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    val types = listOf(
                        WorkoutSetType.NORMAL to "Normální",
                        WorkoutSetType.WARMUP to "Warmup",
                        WorkoutSetType.DROPSET to "Drop set",
                        WorkoutSetType.FAILURE to "Failure"
                    )

                    types.forEach { (type, title) ->
                        val isSelected = set.setType == type
                        val typeVisuals = type.toBadgeVisuals(
                            setNumber = set.setNumber,
                            isCompleted = false,
                            isLight = colorScheme.isLight
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) colorScheme.primary else colorScheme.surfaceVariant)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) colorScheme.primary else colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable(role = Role.Button) {
                                    onSetTypeChange(type)
                                    showOptionsDialog = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (type == WorkoutSetType.NORMAL) "1..N" else typeVisuals.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) colorScheme.onPrimary else typeVisuals.contentColor
                                )
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.xs))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colorScheme.surfaceVariant)
                            .clickable(role = Role.Button) { showOptionsDialog = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Zavřít",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colorScheme.error.copy(alpha = 0.15f))
                            .border(1.dp, colorScheme.error, RoundedCornerShape(10.dp))
                            .clickable(role = Role.Button) {
                                showOptionsDialog = false
                                onDelete()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Smazat sérii 🗑️",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickStepButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(8.dp))
            .background(colorScheme.surfaceVariant)
            .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface
        )
    }
}
