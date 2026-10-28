package core.viewmodel

import androidx.compose.runtime.Composable

@Composable actual fun EditorBackHandler(enabled: Boolean, onBack: () -> Unit) { androidx.activity.compose.BackHandler(enabled, onBack) }
