package features.workout.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import features.workout.active.ActiveWorkoutRoot
import features.workout.active.ActiveWorkoutViewModel
import features.workout.history.WorkoutHistoryRoot
import features.workout.history.WorkoutHistoryViewModel
import org.koin.compose.koinInject

sealed interface WorkoutScreenDestination {
    data object History : WorkoutScreenDestination
    data object ActiveWorkout : WorkoutScreenDestination
}

@Composable
fun WorkoutAppNavHost(
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf<WorkoutScreenDestination>(WorkoutScreenDestination.History) }

    val historyViewModel: WorkoutHistoryViewModel = koinInject()
    val activeViewModel: ActiveWorkoutViewModel = koinInject()

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        modifier = modifier
    ) { destination ->
        when (destination) {
            is WorkoutScreenDestination.History -> {
                WorkoutHistoryRoot(
                    viewModel = historyViewModel,
                    onNavigateToActiveWorkout = {
                        currentScreen = WorkoutScreenDestination.ActiveWorkout
                    }
                )
            }
            is WorkoutScreenDestination.ActiveWorkout -> {
                ActiveWorkoutRoot(
                    viewModel = activeViewModel,
                    onNavigateBack = {
                        currentScreen = WorkoutScreenDestination.History
                    }
                )
            }
        }
    }
}
