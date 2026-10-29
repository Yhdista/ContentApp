package ui.designsystem.nucleus

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

private val DarkColorScheme = darkColorScheme(
    primary = AppColors.DarkPrimary,
    onPrimary = AppColors.DarkOnPrimary,
    primaryContainer = AppColors.DarkPrimaryContainer,
    onPrimaryContainer = AppColors.DarkOnPrimaryContainer,
    background = AppColors.DarkBackground,
    onBackground = AppColors.DarkOnBackground,
    surface = AppColors.DarkSurface,
    onSurface = AppColors.DarkOnSurface,
    surfaceVariant = AppColors.DarkSurfaceVariant,
    onSurfaceVariant = AppColors.DarkOnSurfaceVariant,
    outline = AppColors.DarkOutline,
    outlineVariant = AppColors.DarkOutlineVariant,
    error = AppColors.DarkStatusError,
    onError = AppColors.DarkOnStatusError,
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = AppColors.DarkStatusError
)

private val LightColorScheme = lightColorScheme(
    primary = AppColors.LightPrimary,
    onPrimary = AppColors.LightOnPrimary,
    primaryContainer = AppColors.LightPrimaryContainer,
    onPrimaryContainer = AppColors.LightOnPrimaryContainer,
    background = AppColors.LightBackground,
    onBackground = AppColors.LightOnBackground,
    surface = AppColors.LightSurface,
    onSurface = AppColors.LightOnSurface,
    surfaceVariant = AppColors.LightSurfaceVariant,
    onSurfaceVariant = AppColors.LightOnSurfaceVariant,
    outline = AppColors.LightOutline,
    outlineVariant = AppColors.LightOutlineVariant,
    error = AppColors.LightStatusError,
    onError = Color.White,
    errorContainer = AppColors.LightOnStatusError,
    onErrorContainer = AppColors.LightStatusError
)

/**
 * Nucleus Theme – vstup do Material 3 pro Dark i Light mode.
 * Všechny komponenty čerpají barvy a typografii odsud.
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val typography = rememberAppTypography()

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = { CompositionLocalProvider(LocalContentColor provides colorScheme.onBackground, content = content) }
    )
}
