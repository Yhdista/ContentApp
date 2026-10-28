package features.workout.active

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import core.viewmodel.rememberScreenViewModel
import core.viewmodel.CollectScreenEffects

@Composable
fun ActiveWorkoutRoot(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ActiveWorkoutViewModel = rememberScreenViewModel()
    val state by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    CollectScreenEffects(viewModel.effect) { effect ->
        when (effect) {
            is ActiveWorkoutContract.Effect.WorkoutFinished -> onNavigateBack()
            is ActiveWorkoutContract.Effect.WorkoutCancelled -> onNavigateBack()
            is ActiveWorkoutContract.Effect.ShowSnackbar -> {
                launch { snackbarHostState.showSnackbar(effect.message) }
            }
        }
    }

    ActiveWorkoutScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    )
}
