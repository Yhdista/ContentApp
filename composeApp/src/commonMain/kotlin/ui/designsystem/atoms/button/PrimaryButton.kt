package ui.designsystem.atoms.button

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ui.designsystem.nucleus.AppColors
import ui.designsystem.nucleus.AppSpacing

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = AppColors.Primary,
    contentColor: Color = AppColors.OnPrimary
) {
    val actualBg = if (enabled) containerColor else AppColors.SurfaceVariant
    val actualContent = if (enabled) contentColor else AppColors.OnSurfaceVariant

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize() // Garance 48 dp dotykového terče
            .clip(RoundedCornerShape(12.dp))
            .background(actualBg)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .defaultMinSize(minHeight = AppSpacing.minInteractiveTouchTarget)
            .padding(horizontal = AppSpacing.xl, vertical = AppSpacing.md),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = actualContent,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
