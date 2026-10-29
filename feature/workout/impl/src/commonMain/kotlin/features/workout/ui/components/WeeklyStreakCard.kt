package features.workout.ui.components

import ui.designsystem.atoms.*
import ui.designsystem.nucleus.AppStyle

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

@Composable
internal fun WeeklyStreakCard(
    summary: WeeklyActivitySummary,
    modifier: Modifier = Modifier
) {
    val colorScheme = AppStyle.colors

    AppCard(modifier) {
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
                AppText("🔥", fontSize = 18.sp)
                Column {
                    AppText(
                        text = "Aktivita tento týden",
                        style = AppStyle.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    AppText(
                        text = "Přehled tréninkových dnů",
                        style = AppStyle.typography.labelSmall,
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
                AppText(
                    text = badgeText,
                    style = AppStyle.typography.labelSmall,
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
                    AppText(
                        text = day.dayOfWeekName,
                        style = AppStyle.typography.labelSmall,
                        fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (day.isToday) colorScheme.primary else colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )

                    val circleModifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)

                    if (day.hasWorkout) {
                        Box(
                            modifier = circleModifier.background(colorScheme.success.accent),
                            contentAlignment = Alignment.Center
                        ) {
                            AppText(
                                text = "✓",
                                color = colorScheme.success.onAccent,
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
                            AppText(
                                text = "•",
                                color = colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
