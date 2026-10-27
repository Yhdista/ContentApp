package features.workout.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.common.formatCleanWeight
import features.workout.domain.WorkoutSession
import ui.designsystem.nucleus.AppColors
import ui.designsystem.nucleus.AppSpacing

@Composable
fun WorkoutHistoryItemCard(
    session: WorkoutSession,
    exerciseNames: List<String> = emptyList(),
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val volumeFormatted = session.totalVolumeKg.formatCleanWeight()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.OutlineVariant, RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = session.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.OnSurface
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(AppColors.StatusSynced.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Dokončeno",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.StatusSynced,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Objem: $volumeFormatted kg",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.Primary
            )

            val durationMinutes = if (session.endTimeEpochMs != null) {
                val durationSec = (session.endTimeEpochMs - session.startTimeEpochMs) / 1000
                "${durationSec / 60} min"
            } else {
                "—"
            }

            Text(
                text = "Délka: $durationMinutes",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.OnSurfaceVariant
            )
        }

        // Seznam obsažených cviků (tagy)
        if (exerciseNames.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                exerciseNames.take(3).forEach { name ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AppColors.SurfaceVariant.copy(alpha = 0.6f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.OnSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }
                if (exerciseNames.size > 3) {
                    Text(
                        text = "+${exerciseNames.size - 3}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.OnSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
