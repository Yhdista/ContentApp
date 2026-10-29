package features.workout.ui.components

import ui.designsystem.nucleus.ActionVariant

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.designsystem.atoms.*

@Composable internal fun WorkoutThumbBar(
    onAddExercise: () -> Unit,
    onFinishWorkout: () -> Unit,
    onCancelWorkout: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    AppCard(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppButton(onAddExercise, Modifier.weight(1f), enabled, ActionVariant.Secondary) { AppText("+ Přidat cvik") }
            AppButton(onFinishWorkout, Modifier.weight(1f), enabled) { AppText("Ukončit trénink ✓") }
        }
        AppButton(onCancelWorkout, Modifier.fillMaxWidth(), enabled, ActionVariant.Destructive) { AppText("Zahodit trénink bez uložení") }
    }
}
