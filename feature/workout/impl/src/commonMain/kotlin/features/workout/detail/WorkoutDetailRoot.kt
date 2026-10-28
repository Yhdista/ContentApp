package features.workout.detail

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import core.viewmodel.rememberScreenViewModel

@Composable
fun WorkoutDetailRoot(
    sessionId: String,
    onNavigateBack: () -> Unit,
    onRoutineDraft: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: WorkoutDetailViewModel = rememberScreenViewModel(key = "detail_$sessionId")
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(sessionId) {
        viewModel.load(sessionId)
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is WorkoutDetailContract.Effect.RoutineDraft -> onRoutineDraft(effect.draftId)
                is WorkoutDetailContract.Effect.NavigateBack -> {
                    onNavigateBack()
                }
                is WorkoutDetailContract.Effect.ShowSnackbar -> {
                    launch { snackbarHostState.showSnackbar(effect.message) }
                }
            }
        }
    }

    WorkoutDetailScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    )
}
