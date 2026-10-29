package ui.designsystem.nucleus

import androidx.compose.foundation.layout.sizeIn
import androidx.compose.ui.Modifier

/** Domain-free action intent shared by semantic controls and private platform adapters. */
enum class ActionVariant { Primary, Secondary, Quiet, Destructive }

/** Measured interaction bounds, independent of any higher-level component. */
fun Modifier.appInteractiveSize(): Modifier = sizeIn(
    minWidth = AppSpacing.minInteractiveTouchTarget,
    minHeight = AppSpacing.minInteractiveTouchTarget
)
