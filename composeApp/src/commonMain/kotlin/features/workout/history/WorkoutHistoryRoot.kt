package features.workout.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

@Composable
fun WorkoutHistoryRoot(
    viewModel: WorkoutHistoryViewModel,
    onNavigateToActiveWorkout: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is WorkoutHistoryContract.Effect.NavigateToActiveWorkout -> {
                    onNavigateToActiveWorkout()
                }
                is WorkoutHistoryContract.Effect.NavigateToDetail -> {
                    onNavigateToDetail(effect.sessionId)
                }
                is WorkoutHistoryContract.Effect.ShowSnackbar -> {
                    // Snackbar handling if needed
                }
            }
        }
    }

    WorkoutHistoryScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}
