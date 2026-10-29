package features.workout.ui.components

import androidx.compose.ui.graphics.Color
import features.workout.domain.WorkoutSetType
import ui.designsystem.nucleus.AppColors

internal data class SetTypeBadgeVisuals(
    val label: String,
    val backgroundColor: Color,
    val contentColor: Color
)

/**
 * Jednotný vizuální styl pro odznak typu série (Normal 1..N, Warmup W, Dropset D, Failure F).
 * Podporuje Dark Mode i Light Mode.
 */
internal fun WorkoutSetType.toBadgeVisuals(
    setNumber: Int,
    isCompleted: Boolean = false,
    isLight: Boolean = false
): SetTypeBadgeVisuals = when (this) {
    WorkoutSetType.NORMAL -> SetTypeBadgeVisuals(
        label = "$setNumber",
        backgroundColor = if (isCompleted) {
            if (isLight) AppColors.LightOnStatusSynced else AppColors.DarkStatusSynced.copy(alpha = 0.15f)
        } else {
            if (isLight) AppColors.LightSurfaceVariant else AppColors.DarkSurfaceVariant
        },
        contentColor = if (isCompleted) {
            if (isLight) AppColors.LightStatusSynced else AppColors.DarkStatusSynced
        } else {
            if (isLight) AppColors.LightOnSurface else AppColors.DarkOnSurface
        }
    )
    WorkoutSetType.WARMUP -> SetTypeBadgeVisuals(
        label = "W",
        backgroundColor = if (isLight) Color(0xFFFEF3C7) else Color(0xFFF59E0B).copy(alpha = 0.2f),
        contentColor = if (isLight) Color(0xFF854D0E) else Color(0xFFF59E0B)
    )
    WorkoutSetType.DROPSET -> SetTypeBadgeVisuals(
        label = "D",
        backgroundColor = if (isLight) Color(0xFFEEF2FF) else Color(0xFF6366F1).copy(alpha = 0.2f),
        contentColor = if (isLight) Color(0xFF4F46E5) else Color(0xFFA5B4FC)
    )
    WorkoutSetType.FAILURE -> SetTypeBadgeVisuals(
        label = "F",
        backgroundColor = if (isLight) AppColors.LightOnStatusError else Color(0xFFFCA5A5).copy(alpha = 0.2f),
        contentColor = if (isLight) AppColors.LightStatusError else Color(0xFFFCA5A5)
    )
}
