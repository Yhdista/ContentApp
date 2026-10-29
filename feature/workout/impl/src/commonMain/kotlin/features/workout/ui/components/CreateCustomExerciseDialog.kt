package features.workout.ui.components
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import features.exercises.api.MuscleGroup
import ui.designsystem.atoms.*
import ui.designsystem.molecules.*
@Composable internal fun CreateCustomExerciseDialog(initialName: String = "", onDismiss: () -> Unit, onConfirm: (name: String, muscleGroup: MuscleGroup, defaultRestSeconds: Int) -> Unit, modifier: Modifier = Modifier) {
    var name by remember { mutableStateOf(initialName) }
    var group by remember { mutableStateOf(MuscleGroup.CHEST) }
    var rest by remember { mutableStateOf(90) }
    var error by remember { mutableStateOf<String?>(null) }
    Dialog(onDismissRequest = onDismiss) {
        AppCard(modifier.verticalScroll(rememberScrollState())) {
            AppText("Nový vlastní cvik", heading = true)
            AppInput("Název cviku", name, isError = error != null, supportingText = error) { name = it; error = null }
            AppChoice("Svalová partie: ${group.displayName}", MuscleGroup.entries.map { it.name to it.displayName }) { group = MuscleGroup.valueOf(it) }
            AppChoice("Výchozí pauza: $rest s", listOf(60, 90, 120, 180).map { it.toString() to "$it s" }) { rest = it.toInt() }
            AppButton({ if(name.trim().isBlank()) error = "Zadejte název cviku" else onConfirm(name.trim(), group, rest) }) { AppText("Vytvořit a přidat") }
            AppAction("Zrušit", onClick = onDismiss)
        }
    }
}
