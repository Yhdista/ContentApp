package features.workout.detail

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
    modifier: Modifier = Modifier,
    viewModel: WorkoutDetailViewModel = rememberScreenViewModel(key = "detail_$sessionId")
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(sessionId) {
        viewModel.load(sessionId)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is WorkoutDetailContract.Effect.NavigateBack -> {
                    onNavigateBack()
                }
                is WorkoutDetailContract.Effect.ShowSnackbar -> {
                    // Feedback message
                }
            }
        }
    }

    WorkoutDetailScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}
