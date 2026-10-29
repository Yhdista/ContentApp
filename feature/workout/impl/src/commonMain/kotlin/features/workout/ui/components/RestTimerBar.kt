package features.workout.ui.components

import ui.designsystem.atoms.*
import ui.designsystem.nucleus.AppStyle

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.designsystem.nucleus.AppSpacing

@Composable
internal fun RestTimerBar(
    isVisible: Boolean,
    remainingSeconds: Int,
    totalSeconds: Int,
    onAddSeconds: (Int) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = AppStyle.colors

    AnimatedVisibility(
        visible = isVisible && remainingSeconds > 0,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        val formattedTime = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
        val progress = if (totalSeconds > 0) (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f) else 0f

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colorScheme.surface)
                .border(1.5.dp, colorScheme.primary, RoundedCornerShape(16.dp))
                .padding(AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    AppText(
                        text = "⏱ PAUZA MEZI SÉRIEMI",
                        style = AppStyle.typography.labelSmall,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                AppText(
                    text = formattedTime,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colorScheme.primary
                )
            }

            // Progress bar
            AppProgress(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = colorScheme.primary,
                trackColor = colorScheme.surfaceVariant
            )

            // Ovládací tlačítka (všechna min 48 dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // -15s
                AppSurfaceAction(onClick = { onAddSeconds(-15) }, enabled = true,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorScheme.surfaceVariant)
                        .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
                    contentAlignment = Alignment.Center
                ) {
                    AppText(
                        text = "-15s",
                        style = AppStyle.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(AppSpacing.sm))

                // +30s
                AppSurfaceAction(onClick = { onAddSeconds(30) }, enabled = true,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorScheme.surfaceVariant)
                        .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
                    contentAlignment = Alignment.Center
                ) {
                    AppText(
                        text = "+30s",
                        style = AppStyle.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(AppSpacing.sm))

                // Přeskočit
                AppSurfaceAction(onClick = onSkip, enabled = true,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorScheme.surfaceVariant)
                        .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
                    contentAlignment = Alignment.Center
                ) {
                    AppText(
                        text = "Přeskočit",
                        style = AppStyle.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.error
                    )
                }
            }
        }
    }
}
