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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import features.workout.domain.WorkoutSet
import ui.designsystem.nucleus.AppColors
import ui.designsystem.nucleus.AppSpacing

@Composable
fun SetRowItem(
    set: WorkoutSet,
    onToggleComplete: () -> Unit,
    onWeightChange: (Double) -> Unit,
    onRepsChange: (Int) -> Unit,
    onRpeChange: (Double?) -> Unit,
    onDelete: () -> Unit,
    onOpenPlateCalculator: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var weightText by remember(set.weightKg) {
        val str = if (set.weightKg % 1.0 == 0.0) set.weightKg.toInt().toString() else set.weightKg.toString()
        mutableStateOf(str)
    }
    var repsText by remember(set.reps) {
        mutableStateOf(set.reps.toString())
    }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val rowBackground = if (set.isCompleted) {
        AppColors.Surface.copy(alpha = 0.6f)
    } else {
        AppColors.Surface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(rowBackground)
            .border(
                width = 1.dp,
                color = if (set.isCompleted) AppColors.StatusSynced.copy(alpha = 0.4f) else AppColors.OutlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Číslo série (kliknutím lze smazat sérii)
        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (set.isCompleted) AppColors.StatusSynced.copy(alpha = 0.15f) else AppColors.SurfaceVariant)
                .clickable(role = Role.Button) { showDeleteDialog = true },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${set.setNumber}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (set.isCompleted) AppColors.StatusSynced else AppColors.OnSurface
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
                color = AppColors.OnSurfaceVariant,
                maxLines = 1
            )
            if (set.isPersonalRecord && set.isCompleted) {
                Text(
                    text = "🏆 PR",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFBBF24),
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
                .background(AppColors.SurfaceVariant)
                .border(1.dp, AppColors.Outline, RoundedCornerShape(8.dp))
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
                    color = AppColors.OnSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                cursorBrush = SolidColor(AppColors.Primary),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 4. Vstup pro Opakování (reps)
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AppColors.SurfaceVariant)
                .border(1.dp, AppColors.Outline, RoundedCornerShape(8.dp))
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
                    color = AppColors.OnSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                cursorBrush = SolidColor(AppColors.Primary),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 5. RPE cyklovač (např. 7.5, 8.0, 8.5, 9.0, 9.5, 10.0 nebo —)
        val rpeOptions = listOf(null, 7.0, 7.5, 8.0, 8.5, 9.0, 9.5, 10.0)
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (set.rpe != null) AppColors.PrimaryContainer.copy(alpha = 0.5f) else AppColors.SurfaceVariant.copy(alpha = 0.5f))
                .clickable {
                    val currentIndex = rpeOptions.indexOf(set.rpe)
                    val nextIndex = (currentIndex + 1) % rpeOptions.size
                    onRpeChange(rpeOptions[nextIndex])
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = set.rpe?.let { "@$it" } ?: "RPE",
                style = MaterialTheme.typography.labelSmall,
                color = if (set.rpe != null) AppColors.Primary else AppColors.OnSurfaceVariant,
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
                .background(if (set.isCompleted) AppColors.StatusSynced else AppColors.SurfaceVariant)
                .border(
                    width = 1.5.dp,
                    color = if (set.isCompleted) AppColors.StatusSynced else AppColors.Outline,
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
                color = if (set.isCompleted) AppColors.OnStatusSynced else AppColors.OnSurfaceVariant
            )
        }
    }

    if (showDeleteDialog) {
        Dialog(onDismissRequest = { showDeleteDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppColors.Surface)
                    .border(1.dp, AppColors.Outline, RoundedCornerShape(16.dp))
                    .padding(AppSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                Text(
                    text = "Smazat sérii č. ${set.setNumber}?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.OnSurface
                )
                Text(
                    text = "Tato akce odebere sérii (${set.weightKg} kg × ${set.reps} reps) z aktuálního tréninku.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.OnSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.SurfaceVariant)
                            .clickable(role = Role.Button) { showDeleteDialog = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Zrušit",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.OnSurface
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppColors.StatusError)
                            .clickable(role = Role.Button) {
                                showDeleteDialog = false
                                onDelete()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Smazat",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
