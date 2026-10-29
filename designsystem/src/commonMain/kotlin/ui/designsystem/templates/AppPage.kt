package ui.designsystem.templates

import ui.designsystem.atoms.*
import ui.designsystem.organisms.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.designsystem.nucleus.*
@Composable fun AppScreen(modifier: Modifier = Modifier, topBar: @Composable () -> Unit = {}, snackbarHost: @Composable () -> Unit = {}, content: @Composable (PaddingValues) -> Unit) = NucleusScaffold(modifier, topBar, snackbarHost, content)
@Composable fun AppPage(title: String, modifier: Modifier = Modifier, onBack: (() -> Unit)? = null, content: LazyListScope.() -> Unit) {
    AppScreen(modifier, topBar = { AppTopBar(title = { AppText(title) }, navigationIcon = { if(onBack != null) AppAction("Zpět", onClick = onBack) }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
