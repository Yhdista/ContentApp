package features.workout.ui.components

import androidx.compose.ui.graphics.Color
import features.workout.domain.WorkoutSetType
import ui.designsystem.nucleus.AppColors

data class SetTypeBadgeVisuals(
    val label: String,
    val backgroundColor: Color,
    val contentColor: Color
)

/**
 * Jednotný vizuální styl pro odznak typu série (Normal 1..N, Warmup W, Dropset D, Failure F).
 */
fun WorkoutSetType.toBadgeVisuals(
    setNumber: Int,
    isCompleted: Boolean = false
): SetTypeBadgeVisuals = when (this) {
    WorkoutSetType.NORMAL -> SetTypeBadgeVisuals(
        label = "$setNumber",
        backgroundColor = if (isCompleted) AppColors.StatusSynced.copy(alpha = 0.15f) else AppColors.SurfaceVariant,
        contentColor = if (isCompleted) AppColors.StatusSynced else AppColors.OnSurface
    )
    WorkoutSetType.WARMUP -> SetTypeBadgeVisuals(
        label = "W",
        backgroundColor = Color(0xFFF59E0B).copy(alpha = 0.2f),
        contentColor = Color(0xFFF59E0B)
    )
    WorkoutSetType.DROPSET -> SetTypeBadgeVisuals(
        label = "D",
        backgroundColor = Color(0xFF6366F1).copy(alpha = 0.2f),
        contentColor = Color(0xFF818CF8)
    )
    WorkoutSetType.FAILURE -> SetTypeBadgeVisuals(
        label = "F",
        backgroundColor = Color(0xFFEF4444).copy(alpha = 0.2f),
        contentColor = Color(0xFFEF4444)
    )
}
