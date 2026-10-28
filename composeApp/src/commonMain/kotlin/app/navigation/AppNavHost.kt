package app.navigation

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
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import features.workout.active.ActiveWorkoutRoot
import features.workout.detail.WorkoutDetailRoot
import features.workout.history.WorkoutHistoryRoot
import ui.designsystem.nucleus.AppTheme

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier
) {
    var isDarkThemeState by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val systemInDark = isSystemInDarkTheme()
    val isDark = isDarkThemeState ?: systemInDark

    AppTheme(darkTheme = isDark) {
        val navigation = rememberSaveable(saver = listSaver(
            save = { state: AppNavigationState -> state.save() },
            restore = { keys -> AppNavigationState.restore(keys) }
        )) { AppNavigationState() }
        PlatformBackHandler(navigation.canGoBack) { navigation.back() }

        AnimatedContent(
            targetState = navigation.current,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = modifier
        ) { destination ->
            when (destination) {
                is AppDestination.History -> {
                    WorkoutHistoryRoot(
                        onNavigateToActiveWorkout = {
                            navigation.navigate(AppDestination.ActiveWorkout)
                        },
                        onNavigateToDetail = { sessionId ->
                            navigation.navigate(AppDestination.Detail(sessionId))
                        },
                        isDarkTheme = isDark,
                        onToggleTheme = {
                            isDarkThemeState = !isDark
                        }
                    )
                }
                is AppDestination.ActiveWorkout -> {
                    ActiveWorkoutRoot(
                        onNavigateBack = {
                            navigation.back()
                        }
                    )
                }
                is AppDestination.Detail -> {
                    WorkoutDetailRoot(
                        sessionId = destination.sessionId,
                        onNavigateBack = {
                            navigation.back()
                        }
                    )
                }
            }
        }
    }
}
