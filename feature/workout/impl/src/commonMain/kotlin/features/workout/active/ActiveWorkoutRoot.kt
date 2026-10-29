package features.workout.active

import ui.designsystem.molecules.*

import androidx.compose.runtime.remember
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = rememberAppMessages()

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
        snackbarHost = { AppMessageHost(snackbarHostState) }
    )
}
