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
import java.io.File
import kotlin.time.Duration.Companion.seconds

/** Delayed real SQLite publication must not navigate an unrelated tab during its transition. */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class DelayedEditorNavigationTest {
    @Before fun mainDispatcher() { Dispatchers.setMain(Dispatchers.Unconfined) }
    @After fun resetDispatcher() { Dispatchers.resetMain() }
    @Test fun routinePublishWhileLeavingKeepsOtherTab() = exerciseDelayedPublish(false)
    @Test fun planPublishWhileLeavingKeepsOtherTab() = exerciseDelayedPublish(true)

    // Modern Skiko idle waits inherit this deadline; their default is infinite.
    private fun exerciseDelayedPublish(plan: Boolean) = runDesktopComposeUiTest(width = 460, height = 760, testTimeout = 30.seconds) {
        val db = Room.inMemoryDatabaseBuilder<AppDatabase> { AppDatabaseConstructor.initialize() }.configureAppDatabase().build()
        val app = startKoin { modules(commonModule, platformModule, module { single<AppDatabase> { db } }) }
        val owner = object : ViewModelStoreOwner, LifecycleOwner {
            override val viewModelStore = ViewModelStore()
            override val lifecycle = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        }
        val accepted = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val committed = CompletableDeferred<Unit>()
        var hostVisible by mutableStateOf(true)
        val evidence = File("build/g-ui-fix/${if(plan) "plan" else "routine"}-delayed-publish").apply { mkdirs() }
        val trace = File(evidence, "phases.txt").apply { writeText("") }
        var phase = "fixtures"
        var failedAt: String? = null
        var primaryFailure: Throwable? = null
        fun mark(next: String) {
            phase = next
            trace.appendText("$next clock=${mainClock.currentTime} autoAdvance=${mainClock.autoAdvance} accepted=${accepted.isCompleted} committed=${committed.isCompleted}\n")
        }
        fun recordFailure(failure: Throwable, at: String) {
            runCatching {
                File(evidence, "failure.txt").writeText("phase=$at\n" + failure.stackTraceToString())
            }.onFailure { failure.addSuppressed(it) }
        }
        try {
            mark("fixtures")
            val routines = app.koin.get<RoutineRepository>()
            val plans = app.koin.get<TrainingPlanRepository>()
            runBlocking {
                app.koin.get<features.exercises.api.ExerciseCatalogImport>().importExercises(listOf(Exercise("bench", "Bench", MuscleGroup.CHEST)))
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
            setContent { if(hostVisible) CompositionLocalProvider(LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner,
                LocalSaveableStateRegistry provides remember { SaveableStateRegistry(null) { true } }) { AppNavHost() } }
            mark("open library")
            onNodeWithText(if (plan) "Plány" else "Rutiny", useUnmergedTree = true).performClick()
            waitUntil(timeoutMillis = 5000) { onAllNodes(hasText("Otevřít")).fetchSemanticsNodes().isNotEmpty() }
            mark("open detail")
            onNodeWithText("Otevřít").performClick()
            waitUntil(timeoutMillis = 5000) { onAllNodes(hasText("Upravit / pokračovat v návrhu")).fetchSemanticsNodes().isNotEmpty() }
            mark("open editor")
            onNodeWithText("Upravit / pokračovat v návrhu").performClick()
            waitUntil(timeoutMillis = 5000) { onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
            mark("edit and publish")
            onNodeWithText("Název").performTextReplacement("Committed while leaving")
            onNode(hasScrollAction()).performScrollToNode(hasText("Uložit"))
            onNodeWithText("Uložit").performClick()
            mark("await accepted write")
            waitUntil(timeoutMillis = 5000) { accepted.isCompleted }
            // Hold the outgoing composition in the NavHost transition while the write completes.
            mark("leave with paused transition")
            mainClock.autoAdvance = false
            onNodeWithText(if (plan) "Rutiny" else "Plány", useUnmergedTree = true).performClick()
            mark("release accepted write")
            runOnIdle { release.complete(Unit) }
            mark("await committed write")
            waitUntil(timeoutMillis = 5000) { committed.isCompleted }
            // Allow the effect collector and the real SQL transaction to finish before transition disposal.
            mark("await transactional draft deletion")
            runBlocking { withTimeout(5000) {
                while (db.editorDraftDao().get(if (plan) "trainingplans" else "routines", if (plan) "edit:plan" else "edit:routine") != null) delay(10)
            } }
            mark("settle outgoing transition")
            mainClock.autoAdvance = true
            mainClock.advanceTimeBy(1000)
            waitForIdle()
            mark("assert unrelated tab remains selected")
            onNodeWithText(if (plan) "Nový nebo rozepsaný návrh rutiny" else "Nový nebo rozepsaný plán").assertIsDisplayed()
            runBlocking {
                assertEquals("Committed while leaving", if (plan) plans.getById("plan")!!.name else routines.getById("routine")!!.name)
                assertNull(db.editorDraftDao().get(if (plan) "trainingplans" else "routines", if (plan) "edit:plan" else "edit:routine"))
            }
            mark("restore retained source entry")
            onNodeWithText(if (plan) "Plány" else "Rutiny", useUnmergedTree = true).performClick()
            waitUntil(timeoutMillis = 5000) { onAllNodes(hasText("Committed while leaving")).fetchSemanticsNodes().isNotEmpty() }
            onNodeWithText(if (plan) "Spustit příští den" else "Spustit rutinu").assertIsDisplayed()
            mark("assertions complete")
        } catch (failure: Throwable) {
            primaryFailure = failure
            failedAt = phase
            // Persist before cleanup: a renderer/idle failure must remain observable.
            recordFailure(failure, phase)
            throw failure
        } finally {
            val cleanupFailures = mutableListOf<Throwable>()
            fun cleanup(action: () -> Unit) {
                runCatching(action).onFailure { cleanupFailures += it }
            }
            cleanup { release.complete(Unit) }
            cleanup { mainClock.autoAdvance = false }
            // runOnUiThread dispatches directly; runOnIdle would advance/render forever
            // while unwinding an already failed test. Unmount only during cleanup.
            cleanup { runOnUiThread { hostVisible = false; owner.viewModelStore.clear() } }
            cleanup { app.koin.get<ApplicationScope>().cancel() }
            // Restore overridden global Module definitions so Koin closes their original singleton instances too.
            cleanup { app.koin.loadModules(listOf(commonModule)) }
            cleanup { stopKoin() }
            cleanup { db.close() }
            val failure = primaryFailure
            if(failure != null) {
                cleanupFailures.forEach(failure::addSuppressed)
                recordFailure(failure, failedAt ?: phase)
            } else if(cleanupFailures.isNotEmpty()) {
                val first = cleanupFailures.first()
                cleanupFailures.drop(1).forEach(first::addSuppressed)
                recordFailure(first, "cleanup")
                throw first
            }
        }
    }
}
