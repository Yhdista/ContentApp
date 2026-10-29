package features.workout.ui.components

import ui.designsystem.atoms.*
import ui.designsystem.nucleus.AppStyle

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.common.formatCompactDecimal
import features.workout.domain.WorkoutSession
import ui.designsystem.nucleus.AppSpacing

@Composable
internal fun WorkoutHistoryItemCard(
    session: WorkoutSession,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    exerciseNames: List<String> = emptyList()
) {
    val colorScheme = AppStyle.colors
    val volumeFormatted = session.totalVolumeKg.formatCompactDecimal()

    AppCard(modifier, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppText(
                text = session.title,
                style = AppStyle.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colorScheme.success.container)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                AppText(
                    text = "Dokončeno",
                    style = AppStyle.typography.labelSmall,
                    color = colorScheme.success.content,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppText(
                text = "Objem: $volumeFormatted kg",
                style = AppStyle.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.primary
            )

            val endTimeEpochMs = session.endTimeEpochMs
            val durationMinutes = if (endTimeEpochMs != null) {
                val durationSec = (endTimeEpochMs - session.startTimeEpochMs) / 1000
                "${durationSec / 60} min"
            } else {
                "—"
            }

            AppText(
                text = "Délka: $durationMinutes",
                style = AppStyle.typography.labelSmall,
                color = colorScheme.onSurfaceVariant
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
                            .background(colorScheme.surfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        AppText(
                            text = name,
                            style = AppStyle.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }
                if (exerciseNames.size > 3) {
                    AppText(
                        text = "+${exerciseNames.size - 3}",
                        style = AppStyle.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
