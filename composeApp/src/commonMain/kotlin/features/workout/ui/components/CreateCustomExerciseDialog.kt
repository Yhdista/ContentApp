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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import features.workout.domain.MuscleGroup
import ui.designsystem.nucleus.AppColors
import ui.designsystem.nucleus.AppSpacing

@Composable
fun CreateCustomExerciseDialog(
    initialName: String = "",
    onDismiss: () -> Unit,
    onConfirm: (name: String, muscleGroup: MuscleGroup, defaultRestSeconds: Int) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedMuscleGroup by remember { mutableStateOf(MuscleGroup.CHEST) }
    var selectedRestSeconds by remember { mutableStateOf(90) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val restOptions = listOf(60, 90, 120, 180)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(AppColors.Surface)
                .border(1.dp, AppColors.Outline, RoundedCornerShape(20.dp))
                .padding(AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            Text(
                text = "Nový vlastní cvik",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppColors.OnSurface
            )

            // 1. Název cviku
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                Text(
                    text = "NÁZEV CVIKU",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.OnSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppColors.SurfaceVariant)
                        .border(1.dp, if (errorMessage != null) AppColors.StatusError else AppColors.Outline, RoundedCornerShape(12.dp))
                        .padding(horizontal = AppSpacing.md),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (name.isEmpty()) {
                        Text(
                            text = "Např. Přítahy jednoručky",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.OnSurfaceVariant
                        )
                    }
                    BasicTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            errorMessage = null
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = AppColors.OnSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        cursorBrush = SolidColor(AppColors.Primary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.StatusError
                    )
                }
            }

            // 2. Svalová partie
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                Text(
                    text = "SVALOVÁ PARTIE",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.OnSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    items(MuscleGroup.entries.toTypedArray()) { group ->
                        val isSelected = selectedMuscleGroup == group
                        Box(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) AppColors.Primary else AppColors.SurfaceVariant)
                                .clickable { selectedMuscleGroup = group }
                                .padding(horizontal = AppSpacing.md, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = group.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) AppColors.OnPrimary else AppColors.OnSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 3. Výchozí pauza (Rest Timer)
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                Text(
                    text = "VÝCHOZÍ PAUZA MEZI SÉRIEMI",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.OnSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    restOptions.forEach { seconds ->
                        val isSelected = selectedRestSeconds == seconds
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) AppColors.Primary else AppColors.SurfaceVariant)
                                .clickable { selectedRestSeconds = seconds },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${seconds}s",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) AppColors.OnPrimary else AppColors.OnSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.sm))

            // 4. Akční tlačítka
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppColors.SurfaceVariant)
                        .clickable(role = Role.Button, onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Zrušit",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.OnSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppColors.Primary)
                        .clickable(
                            role = Role.Button,
                            onClick = {
                                if (name.trim().isBlank()) {
                                    errorMessage = "Zadejte název cviku"
                                } else {
                                    onConfirm(name.trim(), selectedMuscleGroup, selectedRestSeconds)
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Vytvořit a přidat",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.OnPrimary
                    )
                }
            }
        }
    }
}
