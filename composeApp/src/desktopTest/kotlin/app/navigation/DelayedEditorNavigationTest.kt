package app.navigation

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.test.*
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.room.Room
import app.database.*
import app.di.commonModule
import app.di.platformModule
import core.common.ApplicationScope
import core.result.Result
import features.exercises.api.*
import features.routines.api.*
import features.trainingplans.api.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Test
import org.junit.Before
import org.junit.After
import org.koin.core.context.*
import org.koin.dsl.module
import kotlin.test.*

/** Delayed real SQLite publication must not navigate an unrelated tab during its transition. */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class DelayedEditorNavigationTest {
    @Before fun mainDispatcher() { Dispatchers.setMain(Dispatchers.Unconfined) }
    @After fun resetDispatcher() { Dispatchers.resetMain() }
    @Test fun routinePublishWhileLeavingKeepsOtherTab() = exerciseDelayedPublish(false)
    @Test fun planPublishWhileLeavingKeepsOtherTab() = exerciseDelayedPublish(true)

    private fun exerciseDelayedPublish(plan: Boolean) = runDesktopComposeUiTest(width = 460, height = 760) {
        val db = Room.inMemoryDatabaseBuilder<AppDatabase> { AppDatabaseConstructor.initialize() }.configureAppDatabase().build()
        val app = startKoin { modules(commonModule, platformModule, module { single<AppDatabase> { db } }) }
        val owner = object : ViewModelStoreOwner, LifecycleOwner {
            override val viewModelStore = ViewModelStore()
            override val lifecycle = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        }
        val accepted = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val committed = CompletableDeferred<Unit>()
        try {
            val routines = app.koin.get<RoutineRepository>()
            val plans = app.koin.get<TrainingPlanRepository>()
            runBlocking {
                app.koin.get<ExerciseCatalog>().saveExercise(Exercise("bench", "Bench", MuscleGroup.CHEST))
                assertIs<Result.Success<Routine>>(routines.save(Routine("routine", "Original routine", exercises = listOf(
                    RoutineExercise("entry", "bench", 90, listOf(RoutineSet("set", 10.0, 8))))), null))
                assertIs<Result.Success<TrainingPlan>>(plans.save(TrainingPlan("plan", "Original plan", slots = listOf(
                    PlanSlot("slot", "Day", "routine"))), null))
            }
            app.koin.loadModules(listOf(module {
                single<RoutineRepository> { object : RoutineRepository by routines {
                    override suspend fun save(routine: Routine, expectedRevision: Long?): Result<Routine, RoutineError> {
                        accepted.complete(Unit); release.await()
                        return routines.save(routine, expectedRevision).also { committed.complete(Unit) }
                    }
                } }
                single<TrainingPlanRepository> { object : TrainingPlanRepository by plans {
                    override suspend fun save(plan: TrainingPlan, expectedRevision: Long?): Result<TrainingPlan, PlanError> {
                        accepted.complete(Unit); release.await()
                        return plans.save(plan, expectedRevision).also { committed.complete(Unit) }
                    }
                } }
            }))
            setContent { CompositionLocalProvider(LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner,
                LocalSaveableStateRegistry provides remember { SaveableStateRegistry(null) { true } }) { AppNavHost() } }
            onNodeWithText(if (plan) "Plány" else "Rutiny", useUnmergedTree = true).performClick()
            waitUntil(timeoutMillis = 5000) { onAllNodes(hasText("Otevřít")).fetchSemanticsNodes().isNotEmpty() }
            onNodeWithText("Otevřít").performClick()
            waitUntil(timeoutMillis = 5000) { onAllNodes(hasText("Upravit / pokračovat v návrhu")).fetchSemanticsNodes().isNotEmpty() }
            onNodeWithText("Upravit / pokračovat v návrhu").performClick()
            waitUntil(timeoutMillis = 5000) { onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
            onNodeWithText("Název").performTextReplacement("Committed while leaving")
            onNode(hasScrollAction()).performScrollToNode(hasText("Uložit"))
            onNodeWithText("Uložit").performClick()
            waitUntil(timeoutMillis = 5000) { accepted.isCompleted }
            // Hold the outgoing composition in the NavHost transition while the write completes.
            mainClock.autoAdvance = false
            onNodeWithText(if (plan) "Rutiny" else "Plány", useUnmergedTree = true).performClick()
            runOnIdle { release.complete(Unit) }
            waitUntil(timeoutMillis = 5000) { committed.isCompleted }
            // Allow the effect collector and the real SQL transaction to finish before transition disposal.
            runBlocking { withTimeout(5000) {
                while (db.editorDraftDao().get(if (plan) "trainingplans" else "routines", if (plan) "edit:plan" else "edit:routine") != null) delay(10)
            } }
            mainClock.autoAdvance = true
            mainClock.advanceTimeBy(1000)
            waitForIdle()
            onNodeWithText(if (plan) "Nový nebo rozepsaný návrh rutiny" else "Nový nebo rozepsaný plán").assertIsDisplayed()
            runBlocking {
                assertEquals("Committed while leaving", if (plan) plans.getById("plan")!!.name else routines.getById("routine")!!.name)
                assertNull(db.editorDraftDao().get(if (plan) "trainingplans" else "routines", if (plan) "edit:plan" else "edit:routine"))
            }
            onNodeWithText(if (plan) "Plány" else "Rutiny", useUnmergedTree = true).performClick()
            waitUntil(timeoutMillis = 5000) { onAllNodes(hasText("Committed while leaving")).fetchSemanticsNodes().isNotEmpty() }
            onNodeWithText(if (plan) "Spustit příští den" else "Spustit rutinu").assertIsDisplayed()
        } finally {
            release.complete(Unit)
            runOnIdle { owner.viewModelStore.clear() }
            app.koin.get<ApplicationScope>().cancel()
            // Restore overridden global Module definitions so Koin closes their original singleton instances too.
            app.koin.loadModules(listOf(commonModule))
            stopKoin(); db.close()
        }
    }
}
