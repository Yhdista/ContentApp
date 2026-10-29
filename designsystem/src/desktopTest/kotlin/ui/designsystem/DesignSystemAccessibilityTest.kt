package ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Test
import org.jetbrains.skia.Image
import java.io.File
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import ui.designsystem.nucleus.*
import ui.designsystem.atoms.*
import ui.designsystem.organisms.*
import ui.designsystem.atoms.feedback.*

@OptIn(ExperimentalTestApi::class)
class DesignSystemAccessibilityTest {
    @Test fun textColorPairsMeetContrastRequirement() {
        for(dark in listOf(false, true)) runDesktopComposeUiTest {
            lateinit var colors: AppColorTokens
            setContent { AppTheme(dark) { colors = AppStyle.colors; AppText("Contrast") } }
            runOnIdle {
                val pairs = listOf(
                    colors.primary to colors.onPrimary,
                    colors.onSurface to colors.surface,
                    colors.onSurfaceVariant to colors.surfaceVariant,
                    colors.success.content to colors.success.container,
                    colors.pending.content to colors.pending.container,
                    colors.danger.content to colors.danger.container,
                    colors.success.onAccent to colors.success.content
                )
                pairs.forEach { (a,b) ->
                    val ratio = (maxOf(a.luminance(), b.luminance()) + .05f) / (minOf(a.luminance(), b.luminance()) + .05f)
                    assertTrue(ratio >= 4.5f, "dark=$dark foreground=$a background=$b contrast=$ratio")
                }
            }
        }
    }
    @Test fun publicControlsRemainOperableAtLargeFont() {
        for(dark in listOf(false,true)) runDesktopComposeUiTest(width = 320, height = 1400) {
            var clicks = 0
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                    AppTheme(dark) { Column(Modifier.fillMaxSize().width(320.dp).background(AppStyle.colors.background).padding(8.dp)) {
                        var value by remember { mutableStateOf("") }
                        var checked by remember { mutableStateOf(false) }
                        AppTopBar(title = { AppText("Obrazovka", heading = true) }, actions = { AppButton({}, Modifier.testTag("toolbar-action")) { AppText("Akce") } })
                        var selectedTab by remember { mutableStateOf(0) }
                        AppTabs(listOf(AppTabItem({ AppText("Tréninky") }, { AppText("◉", Modifier.semantics { contentDescription = "decorative-workouts" }) }), AppTabItem({ AppText("Rutiny") }, { AppText("≡", Modifier.semantics { contentDescription = "decorative-routines" }) }), AppTabItem({ AppText("Plány") }, { AppText("↻", Modifier.semantics { contentDescription = "decorative-plans" }) })), selectedTab) { selectedTab = it }
                        AppCard { AppInput("Název", value, modifier = Modifier.testTag("field"), isError = true, supportingText = "Opravte hodnotu") { value = it }
                            AppButton({ clicks++ }, Modifier.testTag("action")) { AppText("Uložit změny") }
                            AppToggle("Ověřeno", checked, { checked = !checked }, Modifier.testTag("toggle")) { AppText("✓ Ověřit") }
                            AppSelection(true, { AppText("Vybraná možnost") }, {}, Modifier.testTag("selection"))
                            StatusBadge("PENDING", tone = StatusTone.Pending)
                        }
                    } }
                }
            }
            listOf("field","action","toggle","selection","toolbar-action").forEach { tag ->
                val bounds = onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
                assertTrue(bounds.width >= 48f && bounds.height >= 48f, "$tag: $bounds")
                assertTrue(bounds.left >= 0f && bounds.right <= 320f, "$tag exceeds 320dp: $bounds")
            }
            onNodeWithTag("field").performTextReplacement("1.")
            onNodeWithTag("field").assertTextContains("1.")
            onNodeWithTag("action").performClick()
            onNodeWithTag("toggle").performClick().assertIsOn()
            onNodeWithTag("selection").assertIsSelected()
            onNodeWithText("Obrazovka").assertIsDisplayed()
            // Preserve visual and measured evidence even when a following assertion fails.
            val file = File("build/task3-visual/controls-${if(dark) "dark" else "light"}-320-font2.png")
            file.parentFile.mkdirs()
            file.writeBytes(Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).encodeToData()!!.bytes)
            // Actual text layout must retain whole labels; bounds alone allowed character columns.
            val labels = listOf("Tréninky", "Rutiny", "Plány")
            val navigationLayouts = labels.associateWith { label ->
                val layouts = mutableListOf<TextLayoutResult>()
                onNodeWithText(label, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { getLayout ->
                    assertTrue(getLayout(layouts), "Text layout is available for $label")
                }
                layouts.single { it.layoutInput.text.text == label }
            }
            val metrics = navigationLayouts.mapValues { (label, layout) ->
                "$label dark=$dark size=${layout.size} constraints=${layout.layoutInput.constraints} " +
                    "paragraph=${layout.multiParagraph.width}x${layout.multiParagraph.height} " +
                    "lines=${layout.lineCount} line0=[${layout.getLineLeft(0)},${layout.getLineTop(0)},${layout.getLineRight(0)},${layout.getLineBottom(0)}] " +
                    "overflowWidth=${layout.didOverflowWidth} overflowHeight=${layout.didOverflowHeight} " +
                    "density=${layout.layoutInput.density.density} fontScale=${layout.layoutInput.density.fontScale} " +
                    "fontSize=${layout.layoutInput.style.fontSize} lineHeight=${layout.layoutInput.style.lineHeight} " +
                    "textBounds=${onNodeWithText(label, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot} " +
                    "choiceBounds=${onNodeWithText(label).fetchSemanticsNode().boundsInRoot}"
            }
            File(file.parentFile, "navigation-text-layout-${if(dark) "dark" else "light"}-320-font2.txt")
                .writeText(metrics.values.joinToString("\n"))
            val choices = labels.map { label ->
                val layout = navigationLayouts.getValue(label)
                assertEquals(1, layout.lineCount, "$label must remain a whole single-line label at fontScale2; ${metrics[label]}")
                // Compose 1.7 can retain the paragraph's constraint width (e.g.208px)
                // while measuring this single-line Text intrinsically (112px). Its
                // didOverflowWidth then reports unused paragraph space as overflow.
                // Check the actual rendered line rectangle against the measured Text.
                assertTrue(!layout.didOverflowHeight, "$label must not overflow vertically; ${metrics[label]}")
                repeat(layout.lineCount) { line ->
                    assertTrue(
                        layout.getLineLeft(line) >= 0f && layout.getLineRight(line) <= layout.size.width &&
                            layout.getLineTop(line) >= 0f && layout.getLineBottom(line) <= layout.size.height,
                        "$label rendered line $line must fit its measured text bounds; ${metrics[label]}"
                    )
                }
                onNodeWithText(label).fetchSemanticsNode().boundsInRoot.also { bounds ->
                    assertTrue(bounds.width >= 280f && bounds.height >= 48f, "$label must have a full-width choice: $bounds")
                }
            }
            choices.zipWithNext().forEach { (above, below) ->
                assertTrue(above.bottom <= below.top, "Large-font navigation choices must stack vertically")
            }
            listOf("decorative-workouts", "decorative-routines", "decorative-plans").forEach { description ->
                onNodeWithContentDescription(description, useUnmergedTree = true).assertDoesNotExist()
            }
            onNodeWithText("Plány").performClick().assertIsSelected()
            runOnIdle { assertEquals(1, clicks) }
        }
    }
}
