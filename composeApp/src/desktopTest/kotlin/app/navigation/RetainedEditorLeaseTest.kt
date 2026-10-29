package app.navigation

import ui.designsystem.atoms.AppText

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.ui.test.*
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.navigation.createGraph
import androidx.navigation.toRoute
import androidx.room.Room
import app.database.*
import app.di.commonModule
import app.di.platformModule
import core.common.ApplicationScope
import core.result.Result
import features.exercises.api.*
import features.routines.api.*
import features.routines.presentation.RoutineEditorRoot
import features.trainingplans.api.*
import features.trainingplans.presentation.PlanEditorRoot
import kotlinx.coroutines.*
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.Test
import org.junit.Before
import org.junit.After
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import ui.designsystem.nucleus.*
import kotlin.test.*

/** Real feature Roots/ViewModels, entry owners, queue and SQLite; small source destinations isolate
 * lease lifecycle from the separate full Material library rendering smoke. */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class RetainedEditorLeaseTest {
    @Before fun mainDispatcher() { Dispatchers.setMain(Dispatchers.Unconfined) }
    @After fun resetDispatcher() { Dispatchers.resetMain() }
    @Test fun routineEntryRetainsLeaseThenSavesAndReopensForDiscard() = exerciseRetainedEditor(plan = false)
    @Test fun planEntryRetainsLeaseThenSavesAndReopensForDiscard() = exerciseRetainedEditor(plan = true)

    private fun exerciseRetainedEditor(plan: Boolean) = withAppUiTestResources { resources -> runDesktopComposeUiTest(width = 460, height = 760, testTimeout = appUiTestTimeout) {
        val db = Room.inMemoryDatabaseBuilder<AppDatabase> { AppDatabaseConstructor.initialize() }.configureAppDatabase().build()
        val app = startKoin { modules(commonModule, platformModule, module { single<AppDatabase> { db } }) }
        val owner = object : ViewModelStoreOwner, LifecycleOwner {
            override val viewModelStore = ViewModelStore()
            override val lifecycle = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        }
        lateinit var nav: NavHostController
        var savedId: String? = null
        val route: Any = if (plan) PlanEditorRoute("edit:plan") else RoutineEditorRoute("edit:routine")
        val host = AppUiTestHost(this, owner, resources)
        host.run(afterClear = listOf({ app.koin.get<ApplicationScope>().cancel() }, { stopKoin() }, { db.close() })) {
            runBlocking {
                app.koin.get<features.exercises.api.ExerciseCatalogImport>().importExercises(listOf(Exercise("bench", "Bench", MuscleGroup.CHEST)))
                assertIs<Result.Success<Routine>>(app.koin.get<RoutineRepository>().save(
                    Routine("routine", "Original", exercises = listOf(RoutineExercise("entry", "bench", 90, listOf(RoutineSet("set", 10.0, 8))))), null))
                assertIs<Result.Success<TrainingPlan>>(app.koin.get<TrainingPlanRepository>().save(
                    TrainingPlan("plan", "Original", slots = listOf(PlanSlot("slot", "Day", "routine"))), null))
            }
            setContent { host.Content(app) {
                CompositionLocalProvider(LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner,
                    LocalSaveableStateRegistry provides remember { SaveableStateRegistry(null) { true } }) {
                    AppTheme(darkTheme = false) {
                        nav = rememberNavController()
                        val graph = remember(nav) { nav.createGraph(startDestination = RoutineListRoute, route = AppGraphRoute::class) {
                            composable<RoutineListRoute> { AppText("Source") }
                            composable<PlanListRoute> { AppText("Other source") }
                            composable<RoutineEditorRoute> { entry -> RoutineEditorRoot(entry.toRoute<RoutineEditorRoute>().draftId,
                                onSaved = { savedId = it; nav.popBackStack() }, onBack = { nav.popBackStack() }) }
                            composable<PlanEditorRoute> { entry -> PlanEditorRoot(entry.toRoute<PlanEditorRoute>().draftId,
                                onSaved = { savedId = it; nav.popBackStack() }, onBack = { nav.popBackStack() }) }
                        } }
                        NavHost(nav, graph)
                    }
                }
             } }
            runOnIdle { nav.navigate(route) }
            waitUntil(timeoutMillis = 5000) { onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
            val originalEntry = nav.currentBackStackEntry!!
            val originalStore = originalEntry.viewModelStore
            onNodeWithText("Název").performTextReplacement("Retained local")
            runOnIdle {
                assertTrue(nav.popBackStack(RoutineListRoute, inclusive = false, saveState = true))
                nav.navigate(PlanListRoute); nav.navigate(route)
            }
            waitUntil(timeoutMillis = 5000) { onAllNodes(hasText("Tento návrh je otevřený", substring = true)).fetchSemanticsNodes().isNotEmpty() }
            onAllNodes(hasSetTextAction()).assertCountEquals(0)
            onNodeWithText("Zkusit znovu").performClick()
            onAllNodes(hasSetTextAction()).assertCountEquals(0)
            onNodeWithText("Zpět").performClick()
            runOnIdle { nav.popBackStack(); nav.navigate(route) { restoreState = true } }
            waitForIdle(); assertEquals(originalEntry.id, nav.currentBackStackEntry!!.id)
            assertSame(originalStore, nav.currentBackStackEntry!!.viewModelStore)
            onNodeWithText("Název").assertTextContains("Retained local")
            onNodeWithText("Poznámka").performTextReplacement("Owner still accepts edits")
            onNode(hasScrollAction()).performScrollToNode(hasText("Uložit"))
            onNodeWithText("Uložit").performClick()
            waitUntil(timeoutMillis = 5000) { savedId != null }
            runBlocking {
                val name = if (plan) app.koin.get<TrainingPlanRepository>().getById("plan")!!.name
                    else app.koin.get<RoutineRepository>().getById("routine")!!.name
                assertEquals("Retained local", name)
                assertNull(db.editorDraftDao().get(if (plan) "trainingplans" else "routines", if (plan) "edit:plan" else "edit:routine"))
            }
            runOnIdle { nav.navigate(route) }
            waitUntil(timeoutMillis = 5000) { onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
            assertNotEquals(originalEntry.id, nav.currentBackStackEntry!!.id)
            assertNotSame(originalStore, nav.currentBackStackEntry!!.viewModelStore)
            onNodeWithText("Název").assertTextContains("Retained local")
            onNodeWithText("Zpět").performClick()
            onNodeWithText("Zahodit návrh").performClick()
            waitUntil(timeoutMillis = 5000) { onAllNodes(hasText("Source")).fetchSemanticsNodes().isNotEmpty() }
            runBlocking { assertNull(db.editorDraftDao().get(if (plan) "trainingplans" else "routines", if (plan) "edit:plan" else "edit:routine")) }
        }
    } }
}
