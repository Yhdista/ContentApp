package ui.designsystem.atoms.feedback
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ui.designsystem.atoms.*
import ui.designsystem.nucleus.AppStyle
@Composable fun ErrorNotice(message: String, modifier: Modifier = Modifier, onRetry: () -> Unit) {
    Column(modifier) { AppText(message, color = AppStyle.colors.error); AppAction("Obnovit data", onClick = onRetry) }
}
