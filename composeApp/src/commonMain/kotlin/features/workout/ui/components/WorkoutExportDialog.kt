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
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import features.workout.domain.ExportFormat
import ui.designsystem.nucleus.AppSpacing
import ui.designsystem.nucleus.statusSynced

@Composable
fun WorkoutExportDialog(
    csvData: String,
    jsonData: String,
    sessionsCount: Int,
    setsCount: Int,
    onCopyToClipboard: (text: String, isJson: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var selectedFormat by remember { mutableStateOf(ExportFormat.CSV) }
    val currentContent = when (selectedFormat) {
        ExportFormat.CSV -> csvData
        ExportFormat.JSON -> jsonData
    }
    var copiedFeedback by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colorScheme.surface,
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
                    color = colorScheme.onSurface
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Vyexportujte kompletní tréninkovou historii do formátu CSV (kompatibilní s Hevy/Excel) nebo JSON zálohy.",
                    fontSize = 13.sp,
                    color = colorScheme.onSurfaceVariant
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
                            selectedContainerColor = colorScheme.primaryContainer,
                            selectedLabelColor = colorScheme.onPrimaryContainer,
                            containerColor = colorScheme.surfaceVariant,
                            labelColor = colorScheme.onSurfaceVariant
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
                            selectedContainerColor = colorScheme.primaryContainer,
                            selectedLabelColor = colorScheme.onPrimaryContainer,
                            containerColor = colorScheme.surfaceVariant,
                            labelColor = colorScheme.onSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(AppSpacing.sm))

                // Stats summary banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorScheme.surfaceVariant, RoundedCornerShape(AppSpacing.xs))
                        .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Tréninků: $sessionsCount",
                        fontSize = 12.sp,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "Sérií: $setsCount",
                        fontSize = 12.sp,
                        color = colorScheme.onSurface
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
                    colors = CardDefaults.cardColors(containerColor = colorScheme.background)
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
                            color = colorScheme.onSurface
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
                    containerColor = if (copiedFeedback) colorScheme.statusSynced else colorScheme.primary,
                    contentColor = if (copiedFeedback) Color.White else colorScheme.onPrimary
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
                Text("Zavřít", color = colorScheme.onSurfaceVariant)
            }
        }
    )
}
