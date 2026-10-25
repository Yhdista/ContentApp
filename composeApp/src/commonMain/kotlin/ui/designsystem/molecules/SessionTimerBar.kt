package ui.designsystem.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.designsystem.atoms.button.AppIconButton
import ui.designsystem.nucleus.AppColors
import ui.designsystem.nucleus.AppSpacing

@Composable
fun SessionTimerBar(
    secondsElapsed: Long,
    isRunning: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hours = secondsElapsed / 3600
    val minutes = (secondsElapsed % 3600) / 60
    val secs = secondsElapsed % 60
    val formattedTime = if (hours > 0) {
        "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
    } else {
        "${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.Surface)
            .border(1.dp, if (isRunning) AppColors.Primary else AppColors.Outline, RoundedCornerShape(16.dp))
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = if (isRunning) "AKTIVNÍ SEZENÍ BĚŽÍ" else "SEZENÍ POZASTAVENO",
                style = MaterialTheme.typography.labelSmall,
                color = if (isRunning) AppColors.Primary else AppColors.OnSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.headlineMedium,
                color = AppColors.OnSurface,
                fontWeight = FontWeight.Bold
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            if (isRunning) {
                AppIconButton(
                    onClick = onPause,
                    backgroundColor = AppColors.SurfaceVariant
                ) {
                    Text("⏸", fontSize = 16.sp)
                }
            } else {
                AppIconButton(
                    onClick = onStart,
                    backgroundColor = AppColors.Primary
                ) {
                    Text("▶", color = AppColors.OnPrimary, fontSize = 16.sp)
                }
            }

            AppIconButton(
                onClick = onReset,
                backgroundColor = AppColors.SurfaceVariant
            ) {
                Text("↺", color = AppColors.OnSurface, fontSize = 18.sp)
            }
        }
    }
}
