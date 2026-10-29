package features.workout.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import features.workout.domain.WorkoutSet
import org.junit.Test
import org.jetbrains.skia.Image
import java.io.File
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import ui.designsystem.nucleus.*

@OptIn(ExperimentalTestApi::class)
class SetRowAccessibilityTest {
    private val initial = WorkoutSet("set1", "session", "exercise", 1, 20.0, 8)
    @Test fun completionHasCheckedStateAndLabel() = runDesktopComposeUiTest(width = 320, height = 1500) {
        var toggles = 0
        setContent { AppTheme { var set by remember { mutableStateOf(initial) }
            SetRowItem(set, { toggles++; set = set.copy(isCompleted = !set.isCompleted) }, {}, {}, {}, {})
        } }
        val toggle = onNodeWithContentDescription("Dokončeno, série 1")
        toggle.assertIsOff().performClick().assertIsOn()
        val bounds = toggle.fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.width >= 48f && bounds.height >= 48f, "Actual toggle bounds: $bounds")
        runOnIdle { assertEquals(1, toggles) }
        // Normal text uses a compact two-row form, retaining field labels and operable actions.
        val fields = onAllNodes(hasSetTextAction()).fetchSemanticsNodes()
        assertEquals(2, fields.size)
        assertEquals(fields[0].boundsInRoot.top, fields[1].boundsInRoot.top, "Normal-font fields share a row")
        assertTrue(onNodeWithTag("set-set1").fetchSemanticsNode().boundsInRoot.height < 200f)
        val file = File("build/task3-visual/set-row-compact-320-font1.png")
        file.parentFile.mkdirs()
        file.writeBytes(Image.makeFromBitmap(onNodeWithTag("set-set1").captureToImage().asSkiaBitmap()).encodeToData()!!.bytes)
    }
    @Test fun inputsFitAt320DpWithDoubleFontScale() {
        for(dark in listOf(false,true)) runDesktopComposeUiTest(width = 320, height = 1500) {
            var weight: Double? = null
            var reps: Int? = null
            setContent { CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                AppTheme(dark) { Box(Modifier.fillMaxSize().width(320.dp).background(AppStyle.colors.background)) {
                    var current by remember { mutableStateOf(initial) }
                    SetRowItem(current, {}, { weight = it; current = current.copy(weightKg = it) }, { reps = it; current = current.copy(reps = it) }, {}, {})
                } }
            } }
            val fields = onAllNodes(hasSetTextAction()).fetchSemanticsNodes()
            assertEquals(2, fields.size)
            fields.forEach { node ->
                val b = node.boundsInRoot
                assertTrue(b.width >= 48f && b.height >= 48f && b.left >= 0f && b.right <= 320f, "Field bounds at 320dp/font2: $b")
            }
            onNodeWithText("Váha (kg), série 1").performTextReplacement("-")
            onNodeWithText("Váha (kg), série 1").assertTextContains("-")
            onNodeWithText("Váha (kg), série 1").performTextReplacement("1.")
            waitForIdle()
            onNodeWithText("Váha (kg), série 1").assertTextContains("1.")
            onNodeWithText("Opakování, série 1").performTextReplacement("12")
            runOnIdle { assertEquals(1.0, weight); assertEquals(12, reps) }
            onNodeWithContentDescription("Dokončeno, série 1").assertIsDisplayed()
            val file = File("build/task3-visual/set-row-${if(dark) "dark" else "light"}-320-font2.png")
            file.parentFile.mkdirs()
            file.writeBytes(Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).encodeToData()!!.bytes)
        }
    }
}
