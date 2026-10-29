package ui.designsystem.molecules
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ui.designsystem.atoms.*
@Composable fun EditorNotice(message: String, modifier: Modifier = Modifier, onRetry: () -> Unit) {
    AppCard(modifier) { AppText(message); AppAction("Zkusit znovu", onClick = onRetry) }
}
@Composable fun EmptyLibrary(message: String, modifier: Modifier = Modifier) { AppCard(modifier) { AppText(message) } }
