package features.workout.ui.components

import ui.designsystem.atoms.*
import ui.designsystem.molecules.*
import ui.designsystem.nucleus.AppStyle

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

@Composable
internal fun WorkoutExportDialog(
    csvData: String,
    jsonData: String,
    sessionsCount: Int,
    setsCount: Int,
    onCopyToClipboard: (text: String, isJson: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = AppStyle.colors
    var selectedFormat by remember { mutableStateOf(ExportFormat.CSV) }
    val currentContent = when (selectedFormat) {
        ExportFormat.CSV -> csvData
        ExportFormat.JSON -> jsonData
    }
    var copiedFeedback by remember { mutableStateOf(false) }

    AppDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppText(
                    text = "📤 Export tréninků",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                AppText(
                    text = "Vyexportujte tréninkovou historii do CSV nebo JSON. Export neobsahuje rutiny, plány ani rozpracované návrhy.",
                    fontSize = 13.sp,
                    color = colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(AppSpacing.md))

                // Format Toggle Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    AppSelection(
                        selected = selectedFormat == ExportFormat.CSV,
                        onClick = {
                            selectedFormat = ExportFormat.CSV
                            copiedFeedback = false
                        },
                        label = { AppText("CSV (Tabulka / Hevy)") }
                    )
                    AppSelection(
                        selected = selectedFormat == ExportFormat.JSON,
                        onClick = {
                            selectedFormat = ExportFormat.JSON
                            copiedFeedback = false
                        },
                        label = { AppText("JSON (Tréninky)") }
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
                    AppText(
                        text = "Tréninků: $sessionsCount",
                        fontSize = 12.sp,
                        color = colorScheme.onSurface
                    )
                    AppText(
                        text = "Sérií: $setsCount",
                        fontSize = 12.sp,
                        color = colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(AppSpacing.sm))

                // Code Preview Box
                val vScrollState = rememberScrollState()
                val hScrollState = rememberScrollState()
                AppFrame(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(AppSpacing.xs),
                    containerColor = colorScheme.background
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppSpacing.sm)
                            .verticalScroll(vScrollState)
                            .horizontalScroll(hScrollState)
                    ) {
                        AppText(
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
            AppButton(
                onClick = {
                    onCopyToClipboard(currentContent, selectedFormat == ExportFormat.JSON)
                    copiedFeedback = true
                },
                modifier = Modifier.height(AppSpacing.minInteractiveTouchTarget)
            ) {
                AppText(
                    text = if (copiedFeedback) "✓ Zkopírováno!" else "📋 Zkopírovat",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            AppButton(
                onClick = onDismiss,
                modifier = Modifier.height(AppSpacing.minInteractiveTouchTarget)
            ) {
                AppText("Zavřít", color = colorScheme.onSurfaceVariant)
            }
        }
    )
}
