package features.universal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

@Composable
fun UniversalRoot(
    viewModel: UniversalViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    UniversalScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}
