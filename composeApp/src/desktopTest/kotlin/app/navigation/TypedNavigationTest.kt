package app.navigation

import ui.designsystem.atoms.AppText

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.test.*
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import androidx.navigation.createGraph
import core.viewmodel.rememberScreenViewModel
import features.routines.api.*
import features.workout.domain.ActiveWorkoutRoute
import org.junit.Rule
import org.junit.Test
import org.junit.Before
import org.junit.After
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.koin.compose.KoinApplication
import org.koin.dsl.module
import ui.designsystem.nucleus.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class TypedNavigationTest {
    @Before fun mainDispatcher() { Dispatchers.setMain(Dispatchers.Unconfined) }
    @After fun resetDispatcher() { Dispatchers.resetMain() }

    class EntryModel : ViewModel() { var clears = 0; override fun onCleared() { clears++ } }

    @OptIn(ExperimentalTestApi::class)
    @Test fun entryOwnershipRestorationAndWorkoutReturnsToSource() = runComposeUiTest {
        val ui = this
        val owner = object : ViewModelStoreOwner, LifecycleOwner {
            override val viewModelStore = ViewModelStore()
            override val lifecycle = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        }
        var registry by mutableStateOf(SaveableStateRegistry(null) { true })
        var visible by mutableStateOf(true)
        lateinit var nav: NavHostController
        val models = mutableMapOf<String,EntryModel>()
        var draftArgument = ""
        ui.setContent {
            KoinApplication(application = { modules(module { factory { EntryModel() } }) }) {
                CompositionLocalProvider(LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner, LocalSaveableStateRegistry provides registry) {
                    if(visible) {
                    nav = rememberNavController()
                    val graph = remember(nav) { nav.createGraph(startDestination = RoutineListRoute, route = AppGraphRoute::class) {
                        composable<RoutineListRoute> { AppText("Routines") }
                        composable<RoutineDetailRoute> { entry ->
                            val id = entry.toRoute<RoutineDetailRoute>().id
                            val vm: EntryModel = rememberScreenViewModel()
                            models[id] = vm
                            AppText(id)
                        }
                        composable<RoutineEditorRoute> { entry -> draftArgument = entry.toRoute<RoutineEditorRoute>().draftId; AppText(draftArgument) }
                        composable<ActiveWorkoutRoute> { AppText("Workout") }
                    }.withRestorableEntries() }
                    NavHost(nav,graph)
                    }
                }
            }
        }
        ui.runOnIdle { nav.navigate(RoutineDetailRoute("source")) }
        ui.waitForIdle()
        val source = models.getValue("source")
        ui.runOnIdle { nav.navigate(ActiveWorkoutRoute) }
        ui.waitForIdle(); assertEquals(0,source.clears)
        ui.runOnIdle { assertTrue(nav.popBackStack()) }
        ui.waitForIdle(); assertSame(source,models["source"])
        ui.runOnIdle { nav.navigate(RoutineDetailRoute("second")) }
        ui.waitForIdle(); val second = models.getValue("second"); assertNotSame(source,second)
        ui.runOnIdle { nav.popBackStack() }
        ui.waitForIdle(); assertEquals(1,second.clears); assertEquals(0,source.clears)
        ui.runOnIdle { nav.navigate(RoutineEditorRoute("edit:id / : ě")) }
        ui.waitForIdle(); assertEquals("edit:id / : ě", draftArgument)
        val saved = ui.runOnIdle { registry.performSave().also { visible = false } }
        ui.waitForIdle()
        draftArgument = ""
        ui.runOnIdle { registry = SaveableStateRegistry(saved) { true }; visible = true }
        ui.waitForIdle(); assertEquals("edit:id / : ě", draftArgument)
        ui.runOnIdle { assertTrue(nav.popBackStack()) }
        ui.waitForIdle()
        assertSame(source, models["source"])
        ui.runOnIdle { assertTrue(nav.popBackStack()) }
        ui.waitForIdle(); assertEquals(1,source.clears)
        ui.runOnIdle { assertFalse(nav.popBackStack()) }
        owner.viewModelStore.clear()
    }
}
