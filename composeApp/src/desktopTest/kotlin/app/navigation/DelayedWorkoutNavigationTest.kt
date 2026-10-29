package app.navigation

import ui.designsystem.atoms.AppText

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.test.*
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.*
import androidx.navigation.createGraph
import androidx.room.Room
import app.database.*
import app.di.commonModule
import app.di.platformModule
import core.common.ApplicationScope
import features.routines.api.RoutineListRoute
import features.workout.active.ActiveWorkoutRoot
import features.workout.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Test
import org.junit.Before
import org.junit.After
import org.koin.core.context.*
import org.koin.dsl.module
import ui.designsystem.nucleus.*
import kotlin.test.*

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class DelayedWorkoutNavigationTest {
    @Before fun mainDispatcher() { Dispatchers.setMain(Dispatchers.Unconfined) }
    @After fun resetDispatcher() { Dispatchers.resetMain() }
    @Test fun acceptedCompletionWaitsForItsRetainedEntryAndNavigatesExactlyOnce() = withAppUiTestResources { resources -> runDesktopComposeUiTest(width = 460, height = 760, testTimeout = appUiTestTimeout) {
        val db = Room.inMemoryDatabaseBuilder<AppDatabase> { AppDatabaseConstructor.initialize() }.configureAppDatabase().build()
        val app = startKoin { modules(commonModule, platformModule, module { single<AppDatabase> { db } }) }
        val owner = object : ViewModelStoreOwner, LifecycleOwner {
            override val viewModelStore = ViewModelStore()
            override val lifecycle = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        }
        val accepted = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>(); val committed = CompletableDeferred<Unit>()
        lateinit var nav: NavHostController
        var returns = 0
        val host = AppUiTestHost(this, owner, resources)
        host.run(beforeClear = { release.complete(Unit) }, afterClear = listOf(
            { app.koin.get<ApplicationScope>().cancel() }, { app.koin.loadModules(listOf(commonModule)) }, { stopKoin() }, { db.close() }
        )) {
            val repository = app.koin.get<WorkoutRepository>()
            val session = runBlocking { repository.startWorkout("Delayed completion") }
            val complete = app.koin.get<CompleteWorkout>()
            app.koin.loadModules(listOf(module { single<CompleteWorkout> { CompleteWorkout { id ->
                accepted.complete(Unit); release.await()
                complete.complete(id).also { committed.complete(Unit) }
            } } }))
            setContent { CompositionLocalProvider(LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner,
                LocalSaveableStateRegistry provides remember { SaveableStateRegistry(null) { true } }) {
                AppTheme(darkTheme = false) {
                    nav = rememberNavController()
                    val graph = remember(nav) { nav.createGraph(startDestination = WorkoutHistoryRoute, route = AppGraphRoute::class) {
                        composable<WorkoutHistoryRoute> { AppText("History source") }
                        composable<RoutineListRoute> { AppText("Other tab") }
                        composable<ActiveWorkoutRoute> { ActiveWorkoutRoot(onNavigateBack = { returns++; nav.popBackStack() }) }
                    } }
                    NavHost(nav, graph)
                }
            }  }
            runOnIdle { nav.navigate(ActiveWorkoutRoute) }
            waitUntil(timeoutMillis = 5000) { onAllNodes(hasText("Ukončit trénink ✓")).fetchSemanticsNodes().isNotEmpty() }
            onNodeWithText("Ukončit trénink ✓").performClick()
            waitUntil(timeoutMillis = 5000) { accepted.isCompleted }
            mainClock.autoAdvance = false
            runOnIdle {
                nav.popBackStack(WorkoutHistoryRoute, inclusive = false, saveState = true)
                nav.navigate(RoutineListRoute)
                release.complete(Unit)
            }
            waitUntil(timeoutMillis = 5000) { committed.isCompleted }
            runBlocking { assertTrue(repository.getSessionById(session.id)!!.isCompleted) }
            mainClock.autoAdvance = true; mainClock.advanceTimeBy(1000); waitForIdle()
            assertEquals(0, returns)
            assertTrue(nav.currentDestination!!.hasRoute<RoutineListRoute>())
            onNodeWithText("Other tab").assertIsDisplayed()
            runOnIdle { nav.popBackStack(); nav.navigate(ActiveWorkoutRoute) { restoreState = true } }
            waitUntil(timeoutMillis = 5000) { returns == 1 }
            mainClock.advanceTimeBy(1000); waitForIdle()
            assertTrue(nav.currentDestination!!.hasRoute<WorkoutHistoryRoute>())
            assertEquals(1, returns)
        }
    } }
}
