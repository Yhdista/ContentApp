package features.workout.history

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import core.viewmodel.rememberScreenViewModel
import core.viewmodel.CollectScreenEffects

@Composable
fun WorkoutHistoryRoot(
    onNavigateToActiveWorkout: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToRoutines: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true,
    onToggleTheme: () -> Unit = {}
) {
    val viewModel: WorkoutHistoryViewModel = rememberScreenViewModel()
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    CollectScreenEffects(viewModel.effect) { effect ->
        when (effect) {
            WorkoutHistoryContract.Effect.NavigateToRoutines -> onNavigateToRoutines()
            is WorkoutHistoryContract.Effect.NavigateToActiveWorkout -> {
                onNavigateToActiveWorkout()
            }
            is WorkoutHistoryContract.Effect.NavigateToDetail -> {
                onNavigateToDetail(effect.sessionId)
            }
            is WorkoutHistoryContract.Effect.ShowSnackbar -> {
                launch { snackbarHostState.showSnackbar(effect.message) }
            }
        }
    }

    WorkoutHistoryScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
        isDarkTheme = isDarkTheme,
        onToggleTheme = onToggleTheme,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    )
}
