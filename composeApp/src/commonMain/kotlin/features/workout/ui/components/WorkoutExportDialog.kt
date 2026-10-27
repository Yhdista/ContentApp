package features.workout.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import features.workout.domain.ExportFormat
import ui.designsystem.nucleus.AppColors
import ui.designsystem.nucleus.AppSpacing

@Composable
fun WorkoutExportDialog(
    csvData: String,
    jsonData: String,
    sessionsCount: Int,
    setsCount: Int,
    onCopyToClipboard: (text: String, isJson: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.CSV) }
    val currentContent = when (selectedFormat) {
        ExportFormat.CSV -> csvData
        ExportFormat.JSON -> jsonData
    }
    var copiedFeedback by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.Surface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💾 Export a záloha dat",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.OnSurface
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Vyexportujte kompletní tréninkovou historii do formátu CSV (kompatibilní s Hevy/Excel) nebo JSON zálohy.",
                    fontSize = 13.sp,
                    color = AppColors.OnSurfaceVariant
                )

                Spacer(modifier = Modifier.height(AppSpacing.md))

                // Format Toggle Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    FilterChip(
                        selected = selectedFormat == ExportFormat.CSV,
                        onClick = {
                            selectedFormat = ExportFormat.CSV
                            copiedFeedback = false
                        },
                        label = { Text("CSV (Tabulka / Hevy)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppColors.PrimaryContainer,
                            selectedLabelColor = AppColors.OnPrimaryContainer,
                            containerColor = AppColors.SurfaceVariant,
                            labelColor = AppColors.OnSurfaceVariant
                        )
                    )
                    FilterChip(
                        selected = selectedFormat == ExportFormat.JSON,
                        onClick = {
                            selectedFormat = ExportFormat.JSON
                            copiedFeedback = false
                        },
                        label = { Text("JSON (Záloha)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppColors.PrimaryContainer,
                            selectedLabelColor = AppColors.OnPrimaryContainer,
                            containerColor = AppColors.SurfaceVariant,
                            labelColor = AppColors.OnSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(AppSpacing.sm))

                // Stats summary banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.SurfaceVariant, RoundedCornerShape(AppSpacing.xs))
                        .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Tréninků: $sessionsCount",
                        fontSize = 12.sp,
                        color = AppColors.OnSurface
                    )
                    Text(
                        text = "Sérií: $setsCount",
                        fontSize = 12.sp,
                        color = AppColors.OnSurface
                    )
                }

                Spacer(modifier = Modifier.height(AppSpacing.sm))

                // Code Preview Box
                val vScrollState = rememberScrollState()
                val hScrollState = rememberScrollState()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(AppSpacing.xs),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Background)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppSpacing.sm)
                            .verticalScroll(vScrollState)
                            .horizontalScroll(hScrollState)
                    ) {
                        Text(
                            text = currentContent.ifEmpty { "Žádná data k exportu" },
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = AppColors.OnSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCopyToClipboard(currentContent, selectedFormat == ExportFormat.JSON)
                    copiedFeedback = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (copiedFeedback) AppColors.StatusSynced else AppColors.Primary,
                    contentColor = if (copiedFeedback) AppColors.OnStatusSynced else AppColors.OnPrimary
                ),
                shape = RoundedCornerShape(AppSpacing.sm),
                modifier = Modifier.height(AppSpacing.minInteractiveTouchTarget)
            ) {
                Text(
                    text = if (copiedFeedback) "✓ Zkopírováno!" else "📋 Zkopírovat",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(AppSpacing.sm),
                modifier = Modifier.height(AppSpacing.minInteractiveTouchTarget)
            ) {
                Text("Zavřít", color = AppColors.OnSurfaceVariant)
            }
        }
    )
}
