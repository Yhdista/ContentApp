package ui.designsystem.atoms.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import core.model.ItemSyncStatus
import ui.designsystem.nucleus.AppSpacing
import ui.designsystem.nucleus.statusErrorContainerCustom
import ui.designsystem.nucleus.statusErrorCustom
import ui.designsystem.nucleus.statusPending
import ui.designsystem.nucleus.statusPendingContainer
import ui.designsystem.nucleus.statusSynced
import ui.designsystem.nucleus.statusSyncedContainer

@Composable
fun StatusBadge(
    status: ItemSyncStatus,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val (bg, contentColor, dotColor, label) = when (status) {
        ItemSyncStatus.LOCAL -> Quad(
            colorScheme.surfaceVariant,
            colorScheme.onSurfaceVariant,
            colorScheme.onSurfaceVariant,
            "LOKÁLNĚ"
        )
        ItemSyncStatus.SYNCED -> Quad(
            colorScheme.statusSyncedContainer,
            colorScheme.statusSynced,
            colorScheme.statusSynced,
            "SYNCED"
        )
        ItemSyncStatus.PENDING -> Quad(
            colorScheme.statusPendingContainer,
            colorScheme.statusPending,
            colorScheme.statusPending,
            "PENDING"
        )
        ItemSyncStatus.ERROR -> Quad(
            colorScheme.statusErrorContainerCustom,
            colorScheme.statusErrorCustom,
            colorScheme.statusErrorCustom,
            "ERROR"
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = "  $label",
            color = contentColor,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
