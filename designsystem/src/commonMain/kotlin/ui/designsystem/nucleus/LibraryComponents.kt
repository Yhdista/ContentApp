package ui.designsystem.nucleus

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Domain-free Material facades used by the modular library screens. */
@Composable fun AppText(text: String, heading: Boolean = false) {
    Text(text, style = if(heading) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium)
}
@Composable fun AppAction(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(onClick, enabled = enabled, modifier = Modifier.minimumInteractiveComponentSize()) { Text(text) }
}
@Composable fun AppInput(label: String, value: String, enabled: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, label = { Text(label) }, enabled = enabled,
        modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize())
}
@Composable fun AppCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content) }
}
@Composable fun AppChoice(label: String, options: List<Pair<String, String>>, enabled: Boolean = true, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        AppAction(label, enabled && options.isNotEmpty()) { expanded = true }
        DropdownMenu(expanded, { expanded = false }) {
            options.forEach { (id, text) -> DropdownMenuItem(text = { Text(text) }, onClick = { expanded = false; onSelect(id) },
                modifier = Modifier.minimumInteractiveComponentSize()) }
        }
    }
}
@Composable fun AppConfirm(title: String, text: String, confirm: String, onConfirm: () -> Unit, onContinue: () -> Unit, enabled: Boolean = true) {
    AlertDialog(onDismissRequest = onContinue, title = { Text(title) }, text = { Text(text) },
        confirmButton = { AppAction(confirm, enabled, onConfirm) }, dismissButton = { AppAction("Pokračovat v úpravách", enabled, onContinue) })
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun AppPage(title: String, onBack: (() -> Unit)? = null, content: LazyListScope.() -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text(title) }, navigationIcon = { if(onBack != null) AppAction("Zpět", onClick = onBack) }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
@Composable fun AppTabs(selected: Int, onSelect: (Int) -> Unit) {
    NavigationBar {
        listOf("Tréninky", "Rutiny", "Plány").forEachIndexed { index, label ->
            NavigationBarItem(selected == index, onClick = { onSelect(index) }, icon = { Text(listOf("◉", "≡", "↻")[index]) }, label = { Text(label) })
        }
    }
}
@Composable fun AppLoading() { CircularProgressIndicator() }
@Composable private fun ComponentsPreview(dark: Boolean) { AppTheme(darkTheme = dark) {
    AppPage("Knihovna") { item { AppCard { AppText("Silový trénink", true); AppInput("Název", "Den A") {}; AppAction("Uložit") {} } } }
} }
@Preview @Composable private fun LightLibraryPreview() = ComponentsPreview(false)
@Preview @Composable private fun DarkLibraryPreview() = ComponentsPreview(true)
