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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import core.model.ItemSyncStatus
import ui.designsystem.nucleus.AppColors
import ui.designsystem.nucleus.AppSpacing

@Composable
fun StatusBadge(
    status: ItemSyncStatus,
    modifier: Modifier = Modifier
) {
    val (bg, contentColor, dotColor, label) = when (status) {
        ItemSyncStatus.SYNCED -> Quad(
            Color(0xFF064E3B),
            AppColors.StatusSynced,
            AppColors.StatusSynced,
            "SYNCED"
        )
        ItemSyncStatus.PENDING -> Quad(
            Color(0xFF451A03),
            AppColors.StatusPending,
            AppColors.StatusPending,
            "PENDING"
        )
        ItemSyncStatus.ERROR -> Quad(
            Color(0xFF450A0A),
            AppColors.StatusError,
            AppColors.StatusError,
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
