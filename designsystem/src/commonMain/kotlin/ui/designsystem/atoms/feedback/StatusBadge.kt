package ui.designsystem.atoms.feedback
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.designsystem.atoms.AppText
import ui.designsystem.nucleus.AppStyle

enum class StatusTone { Neutral, Success, Pending, Error }
/** UI-only status. Domain-to-label/tone mapping belongs to the consumer. */
@Composable fun StatusBadge(label: String, modifier: Modifier = Modifier, tone: StatusTone = StatusTone.Neutral) {
    val colors = AppStyle.colors
    val (container, foreground) = when(tone) {
        StatusTone.Neutral -> colors.surfaceVariant to colors.onSurfaceVariant
        StatusTone.Success -> colors.success.container to colors.success.content
        StatusTone.Pending -> colors.pending.container to colors.pending.content
        StatusTone.Error -> colors.danger.container to colors.danger.content
    }
    Row(modifier.background(container, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
        AppText(label, color = foreground, style = AppStyle.typography.labelSmall)
    }
}
