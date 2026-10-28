package core.viewmodel

import androidx.compose.runtime.Composable

@Composable expect fun EditorBackHandler(enabled: Boolean, onBack: () -> Unit)
