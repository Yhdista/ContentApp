package features.workout.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import features.workout.domain.WeeklyActivitySummary
import ui.designsystem.nucleus.AppSpacing
import ui.designsystem.nucleus.prGold
import ui.designsystem.nucleus.prGoldBorder
import ui.designsystem.nucleus.prGoldContainer
import ui.designsystem.nucleus.statusSynced

@Composable
fun WeeklyStreakCard(
    summary: WeeklyActivitySummary,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.surface)
            .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        // Hlavička karty
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔥", fontSize = 18.sp)
                Column {
                    Text(
                        text = "Aktivita tento týden",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "Přehled tréninkových dnů",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }

            // Streak / Týdenní odznak
            val badgeText = if (summary.currentStreakDays > 1) {
                "🔥 ${summary.currentStreakDays} dny v řadě"
            } else if (summary.currentStreakDays == 1) {
                "🔥 1 den streak"
            } else {
                "${summary.thisWeekWorkoutsCount} tréninky"
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colorScheme.prGoldContainer)
                    .border(1.dp, colorScheme.prGoldBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.prGold,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Pruh dnů (Pondělí až Neděle)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppSpacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            summary.days.forEach { day ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = day.dayOfWeekName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (day.isToday) colorScheme.primary else colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )

                    val circleModifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)

                    if (day.hasWorkout) {
                        Box(
                            modifier = circleModifier.background(colorScheme.statusSynced),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✓",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }
                    } else if (day.isToday) {
                        Box(
                            modifier = circleModifier
                                .background(colorScheme.primary.copy(alpha = 0.15f))
                                .border(1.5.dp, colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(colorScheme.primary)
                            )
                        }
                    } else {
                        Box(
                            modifier = circleModifier.background(colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "•",
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
