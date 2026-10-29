package app.navigation

import ui.designsystem.atoms.*
import ui.designsystem.organisms.*

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import androidx.navigation.createGraph
import features.workout.domain.*
import features.workout.active.ActiveWorkoutRoot
import features.workout.detail.WorkoutDetailRoot
import features.workout.history.WorkoutHistoryRoot
import features.routines.api.*
import features.routines.presentation.*
import features.trainingplans.api.*
import features.trainingplans.presentation.*
import ui.designsystem.nucleus.*

@kotlinx.serialization.Serializable
internal data object AppGraphRoute

@Composable fun AppNavHost(modifier: Modifier = Modifier) {
    var darkOverride by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val dark = darkOverride ?: isSystemInDarkTheme()
    AppTheme(darkTheme = dark) {
        val nav = rememberNavController()
        val entry by nav.currentBackStackEntryAsState()
        val destination = entry?.destination
        val editor = destination?.hasRoute<RoutineEditorRoute>() == true || destination?.hasRoute<PlanEditorRoute>() == true
        val selected = when {
            destination?.hasRoute<RoutineListRoute>() == true || destination?.hasRoute<RoutineDetailRoute>() == true || destination?.hasRoute<RoutineEditorRoute>() == true -> 1
            destination?.hasRoute<PlanListRoute>() == true || destination?.hasRoute<PlanDetailRoute>() == true || destination?.hasRoute<PlanEditorRoute>() == true -> 2
            else -> 0
        }
        val currentDark by rememberUpdatedState(dark)
        val graph = remember(nav) {
            nav.createGraph(startDestination = WorkoutHistoryRoute, route = AppGraphRoute::class) {
                composable<WorkoutHistoryRoute> { WorkoutHistoryRoot(
                    onNavigateToActiveWorkout = { nav.navigate(ActiveWorkoutRoute) { launchSingleTop = true } },
                    onNavigateToDetail = { nav.navigate(WorkoutDetailRoute(it)) },
                    onNavigateToRoutines = { nav.navigate(RoutineListRoute) { launchSingleTop = true } },
                    isDarkTheme = currentDark, onToggleTheme = { darkOverride = !currentDark }) }
                composable<ActiveWorkoutRoute> { ActiveWorkoutRoot(onNavigateBack = { nav.popBackStack() }) }
                composable<WorkoutDetailRoute> { backStack -> WorkoutDetailRoot(backStack.toRoute<WorkoutDetailRoute>().sessionId,
                    onNavigateBack = { nav.popBackStack() }, onRoutineDraft = { nav.navigate(RoutineEditorRoute(it)) }) }
                composable<RoutineListRoute> { RoutineListRoot({ nav.navigate(RoutineDetailRoute(it)) }, { nav.navigate(RoutineEditorRoute(it)) }) }
                composable<RoutineDetailRoute> { backStack -> RoutineDetailRoot(backStack.toRoute<RoutineDetailRoute>().id,
                    onDetail = { nav.navigate(RoutineDetailRoute(it)) }, onEditor = { nav.navigate(RoutineEditorRoute(it)) },
                    onWorkout = { nav.navigate(ActiveWorkoutRoute) { launchSingleTop = true } }, onBack = { nav.popBackStack() }) }
                composable<RoutineEditorRoute> { backStack -> RoutineEditorRoot(backStack.toRoute<RoutineEditorRoute>().draftId,
                    onSaved = { nav.popBackStack(); nav.navigate(RoutineDetailRoute(it)) { launchSingleTop = true } }, onBack = { nav.popBackStack() }) }
                composable<PlanListRoute> { PlanListRoot({ nav.navigate(PlanDetailRoute(it)) }, { nav.navigate(PlanEditorRoute(it)) }) }
                composable<PlanDetailRoute> { backStack -> PlanDetailRoot(backStack.toRoute<PlanDetailRoute>().id,
                    onDetail = { nav.navigate(PlanDetailRoute(it)) }, onEditor = { nav.navigate(PlanEditorRoute(it)) },
                    onWorkout = { nav.navigate(ActiveWorkoutRoute) { launchSingleTop = true } }, onBack = { nav.popBackStack() },
                    onRoutine = { nav.navigate(RoutineDetailRoute(it)) }) }
                composable<PlanEditorRoute> { backStack -> PlanEditorRoot(backStack.toRoute<PlanEditorRoute>().draftId,
                    onSaved = { nav.popBackStack(); nav.navigate(PlanDetailRoute(it)) { launchSingleTop = true } }, onBack = { nav.popBackStack() }) }
            }
        }
        Column(modifier.fillMaxSize()) {
            NavHost(nav, graph = graph, modifier = Modifier.weight(1f))
            // Editors keep the same durable draft when switching tabs. System/toolbar Back offers discard.
            AppTabs(listOf(AppTabItem({ AppText("Tréninky") }, { AppText("◉") }), AppTabItem({ AppText("Rutiny") }, { AppText("≡") }), AppTabItem({ AppText("Plány") }, { AppText("↻") })), selected) { index ->
                val route: Any = when(index) { 1 -> RoutineListRoute; 2 -> PlanListRoute; else -> WorkoutHistoryRoute }
                if(index != selected || editor) nav.navigate(route) {
                    popUpTo<WorkoutHistoryRoute> { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }
}
