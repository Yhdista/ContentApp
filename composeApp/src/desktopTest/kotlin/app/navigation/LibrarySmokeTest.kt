package app.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.room.Room
import app.database.*
import app.di.commonModule
import app.di.platformModule
import core.common.ApplicationScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.Before
import org.junit.After
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.jetbrains.skia.Image
import java.io.File

/** Real Root/ViewModel/Room/NavHost interaction, rendered by Desktop Skia in an isolated database. */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class LibrarySmokeTest {
    @Before fun mainDispatcher() { Dispatchers.setMain(Dispatchers.Unconfined) }
    @After fun resetDispatcher() { Dispatchers.resetMain() }
    @Test fun createRoutineRawDraftBackAndPlanThroughRealScreens() = runDesktopComposeUiTest(width = 460, height = 760) {
        val db = Room.inMemoryDatabaseBuilder<AppDatabase> { AppDatabaseConstructor.initialize() }.configureAppDatabase().build()
        val app = startKoin { modules(commonModule, platformModule, module { single<AppDatabase> { db } }) }
        val owner = object : ViewModelStoreOwner, LifecycleOwner {
            override val viewModelStore = ViewModelStore()
            override val lifecycle = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        }
        try {
            setContent { CompositionLocalProvider(LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner,
                LocalSaveableStateRegistry provides remember { SaveableStateRegistry(null) { true } }) {
                Box(Modifier.requiredSize(460.dp,760.dp)) { AppNavHost() }
            } }
            onNodeWithText("Rutiny", useUnmergedTree = true).performClick()
            onNodeWithText("Nový nebo rozepsaný návrh rutiny").performClick()
            waitUntil(timeoutMillis = 5000) { onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
            onNodeWithText("Název").performTextInput("Ranní síla")
            onNodeWithText("Přidat cvik").performClick()
            onNodeWithText("Bench press").performClick()
            onNodeWithText("Zpět").performClick()
            onNodeWithText("Návrh není publikován").assertIsDisplayed()
            onNodeWithText("Pokračovat v úpravách").performClick()
            onNode(hasScrollAction()).performScrollToNode(hasText("Váha (kg)"))
            onNodeWithText("Váha (kg)").performTextReplacement("-")
            onNodeWithText("Plány", useUnmergedTree = true).performClick()
            onNodeWithText("Rutiny", useUnmergedTree = true).performClick()
            onNodeWithText("Váha (kg)").assertTextContains("-")
            onNodeWithText("Váha (kg)").performTextReplacement("20.5")
            screenshot("routine-editor")
            onNode(hasScrollAction()).performScrollToNode(hasText("Uložit"))
            onNodeWithText("Uložit").performClick()
            waitUntil(timeoutMillis = 5000) { onAllNodes(hasText("Spustit rutinu")).fetchSemanticsNodes().isNotEmpty() }
            screenshot("routine-detail")
            onNodeWithText("Plány", useUnmergedTree = true).performClick()
            onNodeWithText("Nový nebo rozepsaný plán").performClick()
            waitUntil(timeoutMillis = 5000) { onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
            onNodeWithText("Název").performTextInput("Síla A / B")
            onNodeWithText("Přidat slot s rutinou").performClick()
            onNodeWithText("Ranní síla").performClick()
            screenshot("plan-editor")
            onNode(hasScrollAction()).performScrollToNode(hasText("Uložit"))
            onNodeWithText("Uložit").performClick()
            waitUntil(timeoutMillis = 5000) { onAllNodes(hasText("Spustit příští den")).fetchSemanticsNodes().isNotEmpty() }
            screenshot("plan-detail")
            onNodeWithText("Rutiny", useUnmergedTree = true).performClick()
            waitUntil(timeoutMillis = 5000) { onAllNodes(hasText("Dotčené plány: 1.", substring = true)).fetchSemanticsNodes().isNotEmpty() }
            onNodeWithText("Upravit / pokračovat v návrhu").performClick()
            waitUntil(timeoutMillis = 5000) { onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
            onNodeWithText("Název").performTextReplacement("Retained owner content")
            onNodeWithText("Plány", useUnmergedTree = true).performClick()
            onNode(hasScrollAction()).performScrollToNode(hasText("Otevřít rutinu"))
            onNodeWithText("Otevřít rutinu").performClick()
            onNodeWithText("Upravit / pokračovat v návrhu").performClick()
            waitUntil(timeoutMillis = 5000) { onAllNodes(hasText("Tento návrh je otevřený", substring = true)).fetchSemanticsNodes().isNotEmpty() }
            onAllNodes(hasSetTextAction()).assertCountEquals(0)
            onNodeWithText("Zkusit znovu").performClick()
            onAllNodes(hasSetTextAction()).assertCountEquals(0)
            onNodeWithText("Zpět").performClick()
            onNodeWithText("Zpět").performClick()
            onNodeWithText("Rutiny", useUnmergedTree = true).performClick()
            onNodeWithText("Název").assertTextContains("Retained owner content")
            onNode(hasScrollAction()).performScrollToNode(hasText("Uložit"))
            onNodeWithText("Uložit").performClick()
            waitUntil(timeoutMillis = 5000) { onAllNodes(hasText("Spustit rutinu")).fetchSemanticsNodes().isNotEmpty() }
            waitUntil(timeoutMillis = 5000) { onAllNodes(hasText("Retained owner content")).fetchSemanticsNodes().isNotEmpty() }
            screenshot("renamed-routine-detail")
            onNodeWithText("Plány", useUnmergedTree = true).performClick()
            onNodeWithText("Spustit příští den").performClick()
            waitUntil(timeoutMillis = 5000) { onAllNodes(hasText("Ukončit trénink ✓")).fetchSemanticsNodes().isNotEmpty() }
            onNodeWithText("Ukončit trénink ✓").performClick()
            waitUntil(timeoutMillis = 5000) { onAllNodes(hasText("Spustit příští den")).fetchSemanticsNodes().isNotEmpty() }
            onNodeWithText("Tréninky", useUnmergedTree = true).performClick()
            onNodeWithText("🌙").performClick()
            onNodeWithText("Plány", useUnmergedTree = true).performClick()
            screenshot("plan-detail-dark")
        } finally {
            runOnIdle { owner.viewModelStore.clear() }
            app.koin.get<ApplicationScope>().cancel()
            stopKoin()
        }
    }
    @Test fun retainedPlanReceivesRoutineCreatedThroughAnotherTab() = runDesktopComposeUiTest(width = 460, height = 760) {
        val db = Room.inMemoryDatabaseBuilder<AppDatabase> { AppDatabaseConstructor.initialize() }.configureAppDatabase().build()
        val app = startKoin { modules(commonModule, platformModule, module { single<AppDatabase> { db } }) }
        val owner = object : ViewModelStoreOwner, LifecycleOwner {
            override val viewModelStore = ViewModelStore()
            override val lifecycle = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        }
        try {
            setContent { CompositionLocalProvider(LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner,
                LocalSaveableStateRegistry provides remember { SaveableStateRegistry(null) { true } }) {
                Box(Modifier.requiredSize(460.dp,760.dp)) { AppNavHost() }
            } }
            onNodeWithText("Plány", useUnmergedTree = true).performClick()
            onNodeWithText("Nový nebo rozepsaný plán").performClick()
            waitUntil(timeoutMillis = 5000) { onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
            onNodeWithText("Název").performTextInput("Retained plan")
            onNodeWithText("Přidat slot s rutinou").assertIsNotEnabled()
            onNodeWithText("Rutiny", useUnmergedTree = true).performClick()
            onNodeWithText("Nový nebo rozepsaný návrh rutiny").performClick()
            waitUntil(timeoutMillis = 5000) { onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
            onNodeWithText("Název").performTextInput("Created while plan open")
            onNodeWithText("Přidat cvik").performClick()
            onNodeWithText("Bench press").performClick()
            onNode(hasScrollAction()).performScrollToNode(hasText("Uložit"))
            onNodeWithText("Uložit").performClick()
            waitUntil(timeoutMillis = 5000) { onAllNodes(hasText("Spustit rutinu")).fetchSemanticsNodes().isNotEmpty() }
            onNodeWithText("Plány", useUnmergedTree = true).performClick()
            onNodeWithText("Název").assertTextContains("Retained plan")
            onNodeWithText("Přidat slot s rutinou").assertIsEnabled().performClick()
            onNodeWithText("Created while plan open").performClick()
            screenshot("retained-plan-live-choice")
            onNode(hasScrollAction()).performScrollToNode(hasText("Uložit"))
            onNodeWithText("Uložit").performClick()
            waitUntil(timeoutMillis = 5000) { onAllNodes(hasText("Spustit příští den")).fetchSemanticsNodes().isNotEmpty() }
            onNodeWithText("Retained plan").assertIsDisplayed()
        } finally {
            runOnIdle { owner.viewModelStore.clear() }
            app.koin.get<ApplicationScope>().cancel()
            stopKoin()
        }
    }
    @Test fun historyRendersOriginalTargetsWhenAllActualOccurrencesWereRemoved() = runDesktopComposeUiTest(width = 460, height = 760) {
        val db = Room.inMemoryDatabaseBuilder<AppDatabase> { AppDatabaseConstructor.initialize() }.configureAppDatabase().build()
        val app = startKoin { modules(commonModule, platformModule, module { single<AppDatabase> { db } }) }
        val owner = object : ViewModelStoreOwner, LifecycleOwner {
            override val viewModelStore = ViewModelStore()
            override val lifecycle = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        }
        try {
            val id = kotlinx.coroutines.runBlocking {
                val store = app.koin.get<features.workout.domain.WorkoutStore>()
                val repo = app.koin.get<features.workout.domain.WorkoutRepository>()
                val result = store.createFromPrescription(features.workout.domain.WorkoutStartPrescription("Omitted history",
                    List(2) { e -> features.workout.domain.WorkoutExercisePrescription("source$e", "same-exercise", "Original exercise",
                        features.exercises.api.MuscleGroup.CHEST, 60 + e,
                        List(2) { n -> features.workout.domain.WorkoutSetPrescription(20.0 + e * 2 + n, 8 + n, features.workout.domain.WorkoutSetType.DROPSET) }) }))
                val sessionId = (result as core.result.Result.Success).data.sessionId
                repo.getWorkoutExercises(sessionId).first().forEach { repo.removeWorkoutExercise(sessionId, it.id) }
                store.completeOnce(sessionId)
                sessionId
            }
            setContent { CompositionLocalProvider(LocalLifecycleOwner provides owner, LocalViewModelStoreOwner provides owner) {
                ui.designsystem.nucleus.AppTheme {
                    features.workout.detail.WorkoutDetailRoot(id, {}, {}, Modifier.requiredSize(460.dp, 760.dp))
                }
            } }
            waitUntil(timeoutMillis = 5000) { onAllNodes(hasText("Původní předpis", substring = true)).fetchSemanticsNodes().isNotEmpty() }
            for (weight in 20..23) {
                val originalNumber = if (weight % 2 == 0) 1 else 2
                val reps = if (weight % 2 == 0) 8 else 9
                val label = "$originalNumber. $weight kg × $reps · Dropset · Vynecháno — bez skutečného výkonu"
                onNode(hasScrollAction()).performScrollToNode(hasText(label))
                onNodeWithText(label).assertIsDisplayed()
            }
            screenshot("history-all-targets-omitted")
            onAllNodes(hasText("Skutečnost", substring = false)).assertCountEquals(0)
            onAllNodes(hasText("✓ Hotovo")).assertCountEquals(0)
        } finally {
            runOnIdle { owner.viewModelStore.clear() }
            app.koin.get<ApplicationScope>().cancel()
            stopKoin()
        }
    }
    private fun ComposeUiTest.screenshot(name: String) {
        mainClock.advanceTimeBy(1000)
        waitForIdle()
        val image = onRoot().captureToImage()
        val file = File("build/task5-visual/$name.png"); file.parentFile.mkdirs()
        file.writeBytes(Image.makeFromBitmap(image.asSkiaBitmap()).encodeToData()!!.bytes)
    }
}
