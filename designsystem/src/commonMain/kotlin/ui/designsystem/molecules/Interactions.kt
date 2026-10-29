package ui.designsystem.molecules

import ui.designsystem.atoms.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import ui.designsystem.nucleus.*

@Composable fun AppChoice(label: String, options: List<Pair<String, String>>, modifier: Modifier = Modifier, enabled: Boolean = true, onSelect: (String) -> Unit) {
    NucleusChoice(label, options, enabled, modifier, onSelect)
}

@Composable fun AppDialog(onDismissRequest: () -> Unit, modifier: Modifier = Modifier, title: @Composable () -> Unit, text: @Composable () -> Unit, confirmButton: @Composable () -> Unit, dismissButton: @Composable () -> Unit = {}) = NucleusDialog(onDismissRequest, modifier, title, text, confirmButton, dismissButton)
@Composable fun AppConfirm(title: String, text: String, confirm: String, dismiss: String, onConfirm: () -> Unit, onContinue: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, confirmVariant: ActionVariant = ActionVariant.Primary) {
    AppDialog(onContinue, modifier, title = { AppText(title) }, text = { AppText(text) }, confirmButton = { AppAction(confirm, enabled = enabled, variant = confirmVariant, onClick = onConfirm) }, dismissButton = { AppAction(dismiss, enabled = enabled, onClick = onContinue) })
}
@Composable fun AppSheet(onDismissRequest: () -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) = NucleusSheet(onDismissRequest, modifier, content)
@Composable fun ReorderActions(enabled: Boolean, up: () -> Unit, down: () -> Unit, remove: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) { AppAction("Posunout výše", enabled = enabled, onClick = up); AppAction("Posunout níže", enabled = enabled, onClick = down); AppAction("Odebrat", enabled = enabled, onClick = remove) }
}
/** Stable generic list operation shared by feature editors. */
fun <T> List<T>.moved(item: T, delta: Int): List<T> {
    val index = indexOf(item); val target = index + delta
    if(index < 0 || target !in indices) return this
    return toMutableList().apply { add(target, removeAt(index)) }
}
@Stable class AppMessages internal constructor(internal val material: MaterialMessages) { suspend fun showSnackbar(message: String) { material.show(message) } }
@Composable fun rememberAppMessages(): AppMessages = remember { AppMessages(MaterialMessages()) }
@Composable fun AppMessageHost(messages: AppMessages, modifier: Modifier = Modifier) = NucleusMessages(messages.material, modifier)
