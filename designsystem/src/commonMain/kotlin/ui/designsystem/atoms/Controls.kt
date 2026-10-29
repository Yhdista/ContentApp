package ui.designsystem.atoms

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import ui.designsystem.nucleus.*

enum class ActionVariant { Primary, Secondary, Quiet, Destructive }
fun Modifier.appInteractiveSize(): Modifier = sizeIn(minWidth = 48.dp, minHeight = 48.dp)
@Composable fun AppText(text: String, heading: Boolean = false, modifier: Modifier = Modifier, color: Color = Color.Unspecified, style: TextStyle = if(heading) AppStyle.typography.titleLarge else AppStyle.typography.bodyMedium, fontSize: TextUnit = TextUnit.Unspecified, fontWeight: FontWeight? = null, fontFamily: FontFamily? = null, textAlign: TextAlign? = null, maxLines: Int = Int.MAX_VALUE, overflow: TextOverflow = TextOverflow.Clip) {
    NucleusText(text, if(heading) modifier.semantics { this.heading() } else modifier, color, if(fontFamily != null) style.copy(fontFamily = fontFamily) else style, fontSize, fontWeight, textAlign, maxLines, overflow)
}
@Composable fun AppAction(text: String, enabled: Boolean = true, modifier: Modifier = Modifier, variant: ActionVariant = ActionVariant.Secondary, onClick: () -> Unit) = AppButton(onClick, modifier, enabled = enabled, variant = variant) { AppText(text) }
@Composable fun AppButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, variant: ActionVariant = ActionVariant.Primary, content: @Composable RowScope.() -> Unit) {
    NucleusAction(onClick, modifier.appInteractiveSize(), enabled, variant, content)
}
@Composable fun AppIconAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    AppButton(onClick, modifier.semantics { contentDescription = label }, variant = ActionVariant.Quiet, content = content)
}
@Composable fun AppInput(label: String, value: String, enabled: Boolean = true, modifier: Modifier = Modifier, keyboardOptions: KeyboardOptions = KeyboardOptions.Default, singleLine: Boolean = true, isError: Boolean = false, supportingText: String? = null, onChange: (String) -> Unit) {
    NucleusInput(label, value, modifier.fillMaxWidth().appInteractiveSize(), enabled, keyboardOptions, singleLine, isError, supportingText, onChange)
}
@Composable fun AppFrame(modifier: Modifier = Modifier, containerColor: Color = AppStyle.colors.surface, shape: Shape = RoundedCornerShape(16.dp), content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.background(containerColor, shape).border(1.dp, AppStyle.colors.outlineVariant, shape), content = content)
}
@Composable fun AppCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val frameModifier = if(onClick != null) modifier.appInteractiveSize().clip(RoundedCornerShape(16.dp)).clickable(role = Role.Button, onClick = onClick) else modifier
    AppFrame(frameModifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content) }
}
@Composable fun AppSelection(selected: Boolean, label: @Composable () -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AppButton(onClick, modifier.semantics { this.selected = selected }, variant = if(selected) ActionVariant.Primary else ActionVariant.Secondary) { label() }
}
@Composable fun AppToggle(label: String, checked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier.appInteractiveSize().toggleable(checked, role = Role.Checkbox, onValueChange = { onToggle() }).semantics { contentDescription = label }.padding(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, content = content)
}
@Composable fun AppLoading(modifier: Modifier = Modifier, color: Color = AppStyle.colors.primary) = NucleusLoading(modifier, color)
@Composable fun AppProgress(progress: Float, modifier: Modifier = Modifier, color: Color = AppStyle.colors.primary, trackColor: Color = AppStyle.colors.surfaceVariant) = NucleusProgress(progress, modifier, color, trackColor)

/** A surface action with feature-owned visual content; shares button semantics and measured bounds. */
@Composable fun AppSurfaceAction(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, label: String? = null, selected: Boolean? = null, contentAlignment: androidx.compose.ui.Alignment = androidx.compose.ui.Alignment.Center, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.appInteractiveSize().then(if(selected != null) Modifier.semantics { this.selected = selected } else Modifier).then(if(label != null) Modifier.semantics { contentDescription = label } else Modifier).clickable(enabled = enabled, role = Role.Button, onClick = onClick), contentAlignment = contentAlignment, content = content)
}
