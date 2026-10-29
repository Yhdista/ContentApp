package features.workout.detail

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import features.exercises.api.Exercise
import features.exercises.api.MuscleGroup
import features.workout.domain.*
import features.workout.ui.components.toBadgeVisuals
import org.jetbrains.skia.Image
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import ui.designsystem.atoms.AppText
import ui.designsystem.nucleus.*

@OptIn(ExperimentalTestApi::class)
class WorkoutDetailAccessibilityTest {
    @Test fun featureBadgePairsMeetContrastOnActualDetailLayers() {
        for (dark in listOf(false, true)) runDesktopComposeUiTest {
            lateinit var colors: AppColorTokens
            setContent { AppTheme(dark) { colors = AppStyle.colors; AppText("Contrast") } }
            runOnIdle {
                // DetailExerciseCard is an AppCard surface, with a half-alpha row,
                // then the feature badge's possibly translucent background.
                val row = colors.surfaceVariant.copy(alpha = .5f).compositeOver(colors.surface)
                for (type in WorkoutSetType.entries) for (checked in listOf(false, true)) {
                    val badge = type.toBadgeVisuals(12, checked, colors.isLight)
                    val background = badge.backgroundColor.compositeOver(row)
                    val foreground = badge.contentColor.compositeOver(background)
                    val ratio = (maxOf(foreground.luminance(), background.luminance()) + .05f) /
                        (minOf(foreground.luminance(), background.luminance()) + .05f)
                    assertTrue(ratio >= 4.5f, "dark=$dark type=$type checked=$checked foreground=$foreground background=$background contrast=$ratio")
                }
            }
        }
    }

    @Test fun doubleDigitBadgeFitsActualDetailAtDoubleFontScale() {
        for (dark in listOf(false, true)) runDesktopComposeUiTest(width = 320, height = 1400) {
            val set = WorkoutSet("set12", "session", "bench", 12, 20.0, 8)
            val state = WorkoutDetailContract.State(
                session = WorkoutSession("session", "Detail", 0L),
                exercisesWithSets = listOf(WorkoutDetailContract.ExerciseWithSets(
                    Exercise("bench", "Bench", MuscleGroup.CHEST), listOf(set), 160.0)))
            setContent { CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                AppTheme(dark) { WorkoutDetailScreen(state, {}, Modifier.width(320.dp)) }
            } }
            val text = onNodeWithText("12", useUnmergedTree = true).performScrollTo()
            val file = File("build/review-ui-visual/detail-${if (dark) "dark" else "light"}-320-font2.png")
            file.parentFile.mkdirs()
            file.writeBytes(Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).encodeToData()!!.bytes)
            val layouts = mutableListOf<TextLayoutResult>()
            text.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { assertTrue(it(layouts)) }
            val layout = layouts.single { it.layoutInput.text.text == "12" }
            val bounds = text.fetchSemanticsNode().boundsInRoot
            val metrics = "size=${layout.size} paragraphHeight=${layout.multiParagraph.height} constraints=${layout.layoutInput.constraints} bounds=$bounds lines=${layout.lineCount} " +
                "line0=[${layout.getLineLeft(0)},${layout.getLineTop(0)},${layout.getLineRight(0)},${layout.getLineBottom(0)}] overflowHeight=${layout.didOverflowHeight}"
            File(file.parentFile, "detail-${if (dark) "dark" else "light"}-layout.txt").writeText(metrics)
            assertEquals(1, layout.lineCount, "Two-digit badge must remain whole: $metrics")
            assertTrue(!layout.didOverflowHeight, "Badge must not clip vertically: $metrics")
            // Font leading can place lineTop at -1px for a correctly measured
            // 28px paragraph. Check rendered horizontal extents and total height,
            // rather than treating typographic leading as clipped content.
            assertTrue(layout.multiParagraph.height <= layout.size.height,
                "Badge paragraph must fit measured height: $metrics")
            repeat(layout.lineCount) { line ->
                assertTrue(layout.getLineLeft(line) >= 0 && layout.getLineRight(line) <= layout.size.width,
                    "Rendered badge line must fit measured width: $metrics")
            }
            assertTrue(bounds.left >= 0 && bounds.right <= 320 && bounds.height >= layout.multiParagraph.height,
                "Rendered badge must fit actual screen bounds: $metrics")
            // The same actual detail capture exposed squeezed summary/header
            // siblings. Natural word wraps are allowed; character columns are not.
            for (label in listOf("Celkový objem", "Délka cvičení", "Bench", "Skutečnost: 0 / 1 sérií")) {
                val node = onNodeWithText(label, useUnmergedTree = true).performScrollTo()
                val results = mutableListOf<TextLayoutResult>()
                node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { assertTrue(it(results)) }
                val result = results.single { it.layoutInput.text.text == label }
                val actualBounds = node.fetchSemanticsNode().boundsInRoot
                val detail = "$label size=${result.size} paragraphHeight=${result.multiParagraph.height} bounds=$actualBounds lines=${result.lineCount}"
                File(file.parentFile, "detail-${if (dark) "dark" else "light"}-layout.txt").appendText("\n$detail")
                assertTrue(!result.didOverflowHeight && result.multiParagraph.height <= result.size.height &&
                    actualBounds.height >= result.multiParagraph.height && actualBounds.left >= 0 && actualBounds.right <= 320,
                    "Detail heading must fit its actual bounds: $detail")
                repeat(result.lineCount) { line ->
                    assertTrue(result.getLineLeft(line) >= 0 && result.getLineRight(line) <= result.size.width,
                        "Rendered heading line must fit: $detail")
                    if (line < result.lineCount - 1) {
                        val end = result.getLineEnd(line, visibleEnd = true)
                        assertTrue(end > 0 && end < label.length &&
                            (label[end - 1].isWhitespace() || label[end].isWhitespace()),
                            "Detail heading must wrap between words, never inside a word: end=$end $detail")
                    }
                }
            }
        }
    }
}
