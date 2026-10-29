package ui.designsystem.nucleus
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import ui.designsystem.atoms.*

@Composable internal fun NucleusText(text: String, modifier: Modifier, color: Color, style: TextStyle, fontSize: TextUnit, fontWeight: FontWeight?, textAlign: TextAlign?, maxLines: Int, overflow: TextOverflow) {
    Text(text, modifier, color = color, style = style, fontSize = fontSize, fontWeight = fontWeight, textAlign = textAlign, maxLines = maxLines, overflow = overflow)
}
@Composable internal fun NucleusAction(onClick: () -> Unit, modifier: Modifier, enabled: Boolean, variant: ActionVariant, content: @Composable RowScope.() -> Unit) {
    when(variant) {
        ActionVariant.Primary -> Button(onClick, modifier, enabled, content = content)
        ActionVariant.Secondary -> OutlinedButton(onClick, modifier, enabled, content = content)
        ActionVariant.Quiet -> TextButton(onClick, modifier, enabled, content = content)
        ActionVariant.Destructive -> Button(onClick, modifier, enabled, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError), content = content)
    }
}
@Composable internal fun NucleusInput(label: String, value: String, modifier: Modifier, enabled: Boolean, keyboardOptions: KeyboardOptions, singleLine: Boolean, isError: Boolean, supportingText: String?, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, modifier, enabled = enabled, label = { Text(label) }, keyboardOptions = keyboardOptions, singleLine = singleLine, isError = isError, supportingText = supportingText?.let { { Text(it) } })
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun NucleusSheet(onDismissRequest: () -> Unit, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(onDismissRequest, modifier, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), content = content)
}
@Composable internal fun NucleusDialog(onDismissRequest: () -> Unit, modifier: Modifier, title: @Composable () -> Unit, text: @Composable () -> Unit, confirmButton: @Composable () -> Unit, dismissButton: @Composable () -> Unit) {
    AlertDialog(onDismissRequest, confirmButton, modifier, dismissButton = dismissButton, title = title, text = text)
}
@Composable internal fun NucleusScaffold(modifier: Modifier, topBar: @Composable () -> Unit, snackbarHost: @Composable () -> Unit, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(modifier, topBar = topBar, snackbarHost = snackbarHost, containerColor = MaterialTheme.colorScheme.background, content = content)
}
@Composable internal fun NucleusLoading(modifier: Modifier, color: Color) { CircularProgressIndicator(modifier, color = color) }
@Composable internal fun NucleusProgress(progress: Float, modifier: Modifier, color: Color, trackColor: Color) { LinearProgressIndicator(progress = { progress }, modifier = modifier, color = color, trackColor = trackColor) }
internal class MaterialMessages { val state = SnackbarHostState() }
internal suspend fun MaterialMessages.show(message: String) { state.showSnackbar(message) }
@Composable internal fun NucleusMessages(messages: MaterialMessages, modifier: Modifier) { SnackbarHost(messages.state, modifier) }

@Composable internal fun NucleusChoice(label: String, options: List<Pair<String, String>>, enabled: Boolean, modifier: Modifier, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        AppButton({ expanded = true }, enabled = enabled && options.isNotEmpty(), variant = ActionVariant.Secondary) { AppText(label) }
        DropdownMenu(expanded, { expanded = false }) {
            options.forEach { (id, text) -> DropdownMenuItem(text = { Text(text) }, onClick = { expanded = false; onSelect(id) }, modifier = Modifier.appInteractiveSize()) }
        }
    }
}
