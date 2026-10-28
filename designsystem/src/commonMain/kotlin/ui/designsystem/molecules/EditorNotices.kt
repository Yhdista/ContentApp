package ui.designsystem.molecules

import androidx.compose.runtime.Composable
import ui.designsystem.nucleus.*

@Composable fun EditorNotice(message: String, onRetry: () -> Unit) {
    AppCard { AppText(message); AppAction("Zkusit znovu", onClick = onRetry) }
}
@Composable fun EmptyLibrary(message: String) { AppCard { AppText(message) } }
