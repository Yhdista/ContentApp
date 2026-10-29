package ui.designsystem.molecules
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import ui.designsystem.atoms.*
import ui.designsystem.atoms.feedback.*
import ui.designsystem.nucleus.AppTheme
@Composable private fun ControlsPreview(dark: Boolean) { AppTheme(dark) {
    AppCard(Modifier.width(320.dp)) {
        AppText("Upravit položku", heading = true)
        AppInput("Název", "Den A") {}
        AppInput("Poznámka", "Víceřádkový text", singleLine = false) {}
        StatusBadge("Čeká na synchronizaci", StatusTone.Pending)
        AppButton({}) { AppText("Uložit") }
        AppToggle("Ověřeno", true, {}) { AppText("✓ Ověřeno") }
    }
} }
@Preview @Composable private fun LightControlsPreview() = ControlsPreview(false)
@Preview @Composable private fun DarkControlsPreview() = ControlsPreview(true)
