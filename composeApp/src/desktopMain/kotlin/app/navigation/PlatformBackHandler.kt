package app.navigation

import androidx.compose.runtime.Composable

// These hosts have no Android-style system back dispatch. Screens provide explicit back actions.
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit
