package ui.designsystem.nucleus
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.MaterialTheme

data class AppColorTokens(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color
) {
    val isLight: Boolean get() = background.luminance() > 0.5f
    val success: ToneColors get() = if(isLight) ToneColors(AppColors.LightStatusSynced, AppColors.LightOnStatusSynced, AppColors.LightStatusSynced, Color.White) else ToneColors(AppColors.DarkStatusSynced, Color(0xFF064E3B), AppColors.DarkStatusSynced, AppColors.DarkOnStatusSynced)
    val pending: ToneColors get() = if(isLight) ToneColors(AppColors.LightStatusPending, AppColors.LightOnStatusPending, AppColors.DarkStatusPending, AppColors.DarkOnStatusPending) else ToneColors(AppColors.DarkStatusPending, AppColors.DarkOnStatusPending, AppColors.DarkStatusPending, AppColors.DarkOnStatusPending)
    val danger: ToneColors get() = ToneColors(error, errorContainer, error, onError)

}
/** Distinct text foreground, surface and filled-accent pairing. */
data class ToneColors(val content: Color, val container: Color, val accent: Color, val onAccent: Color)
data class AppTextStyles(
    val headlineLarge: TextStyle,
    val headlineMedium: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle
)
/** Theme facade contains no public Material types. */
object AppStyle {
    val colors: AppColorTokens @Composable get() = MaterialTheme.colorScheme.let { c -> AppColorTokens(c.primary, c.onPrimary, c.primaryContainer, c.onPrimaryContainer, c.background, c.onBackground, c.surface, c.onSurface, c.surfaceVariant, c.onSurfaceVariant, c.outline, c.outlineVariant, c.error, c.onError, c.errorContainer, c.onErrorContainer) }
    val typography: AppTextStyles @Composable get() = MaterialTheme.typography.let { t -> AppTextStyles(t.headlineLarge, t.headlineMedium, t.titleLarge, t.titleMedium, t.titleSmall, t.bodyLarge, t.bodyMedium, t.bodySmall, t.labelLarge, t.labelMedium, t.labelSmall) }
}
