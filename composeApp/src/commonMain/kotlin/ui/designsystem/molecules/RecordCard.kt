package ui.designsystem.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import core.model.ContentItem
import ui.designsystem.atoms.feedback.StatusBadge
import ui.designsystem.nucleus.AppColors
import ui.designsystem.nucleus.AppSpacing

@Composable
fun RecordCard(
    item: ContentItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Outline, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.category.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.Primary,
                fontWeight = FontWeight.Bold
            )
            StatusBadge(status = item.syncStatus)
        }

        Text(
            text = item.title,
            style = MaterialTheme.typography.titleLarge,
            color = AppColors.OnSurface,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = item.description,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.OnSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hodnota: ${item.value}",
                style = MaterialTheme.typography.labelLarge,
                color = AppColors.OnSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "ID: ${item.id} • ${if (item.clientUuid.isNotBlank()) item.clientUuid.take(12) + "..." else "init"}",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.OnSurfaceVariant
            )
        }
    }
}
