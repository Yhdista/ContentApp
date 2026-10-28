package features.workout.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ui.designsystem.nucleus.AppSpacing
import ui.designsystem.nucleus.statusSynced

@Composable
internal fun WorkoutThumbBar(
    onAddExercise: () -> Unit,
    onFinishWorkout: () -> Unit,
    onCancelWorkout: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(colorScheme.surface)
            .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tlačítko + Přidat cvik (min 48 dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .height(AppSpacing.minInteractiveTouchTarget)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorScheme.surfaceVariant)
                    .border(1.dp, colorScheme.primary, RoundedCornerShape(12.dp))
                    .clickable(enabled = enabled, role = Role.Button, onClick = onAddExercise),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+ Přidat cvik",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary
                )
            }

            // Tlačítko Ukončit trénink (min 48 dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .height(AppSpacing.minInteractiveTouchTarget)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorScheme.statusSynced)
                    .clickable(enabled = enabled, role = Role.Button, onClick = onFinishWorkout),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Ukončit trénink ✓",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Zrušit trénink (diskrétní textové tlačítko s 48dp zónou)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .clickable(enabled = enabled, role = Role.Button, onClick = onCancelWorkout),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Zahodit trénink bez uložení",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.error
            )
        }
    }
}
