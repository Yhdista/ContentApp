package features.workout.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
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
import ui.designsystem.nucleus.AppColors
import ui.designsystem.nucleus.AppSpacing

@Composable
fun RestTimerBar(
    isVisible: Boolean,
    remainingSeconds: Int,
    totalSeconds: Int,
    onAddSeconds: (Int) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                .background(AppColors.Surface)
                .border(1.5.dp, AppColors.Primary, RoundedCornerShape(16.dp))
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
                    Text(
                        text = "⏱ PAUZA MEZI SÉRIEMI",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = formattedTime,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppColors.Primary
                )
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = AppColors.Primary,
                trackColor = AppColors.SurfaceVariant
            )

            // Ovládací tlačítka (všechna min 48 dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // -15s
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppColors.SurfaceVariant)
                        .clickable(role = Role.Button) { onAddSeconds(-15) }
                        .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "-15s",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.OnSurface
                    )
                }

                Spacer(modifier = Modifier.width(AppSpacing.sm))

                // +30s
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppColors.SurfaceVariant)
                        .clickable(role = Role.Button) { onAddSeconds(30) }
                        .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+30s",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Primary
                    )
                }

                Spacer(modifier = Modifier.width(AppSpacing.sm))

                // Přeskočit
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppColors.SurfaceVariant)
                        .clickable(role = Role.Button, onClick = onSkip)
                        .padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Přeskočit",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.StatusError
                    )
                }
            }
        }
    }
}
