package features.workout.active

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

@Composable
fun ActiveWorkoutRoot(
    viewModel: ActiveWorkoutViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ActiveWorkoutContract.Effect.WorkoutFinished -> onNavigateBack()
                is ActiveWorkoutContract.Effect.WorkoutCancelled -> onNavigateBack()
                is ActiveWorkoutContract.Effect.ShowSnackbar -> {
                    // Snackbar handling if needed
                }
            }
        }
    }

    ActiveWorkoutScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}
