package features.workout.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import core.sync.SyncManager
import features.workout.active.ActiveWorkoutRoot
import features.workout.detail.WorkoutDetailRoot
import features.workout.history.WorkoutHistoryRoot
import org.koin.compose.koinInject
import ui.designsystem.nucleus.AppTheme

sealed interface WorkoutScreenDestination {
    data object History : WorkoutScreenDestination
    data object ActiveWorkout : WorkoutScreenDestination
    data class Detail(val sessionId: String) : WorkoutScreenDestination
}

@Composable
fun WorkoutAppNavHost(
    modifier: Modifier = Modifier
) {
    var isDarkThemeState by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val systemInDark = isSystemInDarkTheme()
    val isDark = isDarkThemeState ?: systemInDark

    val syncManager: SyncManager = koinInject()

    LaunchedEffect(Unit) {
        syncManager.startSyncLoop()
    }

    AppTheme(darkTheme = isDark) {
        var currentScreen by remember { mutableStateOf<WorkoutScreenDestination>(WorkoutScreenDestination.History) }

        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = modifier
        ) { destination ->
            when (destination) {
                is WorkoutScreenDestination.History -> {
                    WorkoutHistoryRoot(
                        onNavigateToActiveWorkout = {
                            currentScreen = WorkoutScreenDestination.ActiveWorkout
                        },
                        onNavigateToDetail = { sessionId ->
                            currentScreen = WorkoutScreenDestination.Detail(sessionId)
                        },
                        isDarkTheme = isDark,
                        onToggleTheme = {
                            isDarkThemeState = !isDark
                        }
                    )
                }
                is WorkoutScreenDestination.ActiveWorkout -> {
                    ActiveWorkoutRoot(
                        onNavigateBack = {
                            currentScreen = WorkoutScreenDestination.History
                        }
                    )
                }
                is WorkoutScreenDestination.Detail -> {
                    WorkoutDetailRoot(
                        sessionId = destination.sessionId,
                        onNavigateBack = {
                            currentScreen = WorkoutScreenDestination.History
                        }
                    )
                }
            }
        }
    }
}
