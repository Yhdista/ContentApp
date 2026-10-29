package verification

import ui.designsystem.nucleus.ActionVariant

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import ui.designsystem.atoms.*
import ui.designsystem.atoms.feedback.*
import ui.designsystem.molecules.*
import ui.designsystem.organisms.*
import ui.designsystem.templates.*
import ui.designsystem.nucleus.AppTheme
import ui.designsystem.nucleus.AppStyle

/** Only designsystem is declared: no Material, foundation/domain, storage or feature dependency. */
@Composable fun PublicDesignSystemConsumer(modifier: Modifier = Modifier) {
    AppTheme {
        val messages = rememberAppMessages()
        Column(modifier) {
            AppTabs(listOf(AppTabItem({ AppText("Tab") }, { AppText("◉") })), 0) {}
            AppMessageHost(messages)
            AppPage("Consumer") {
                item { AppCard {
                    StatusBadge("Ready", tone = StatusTone.Success)
                    AppInput("Label", "-", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), isError = true, supportingText = "Review input") {}
                    AppText("Styled", style = AppStyle.typography.bodyMedium, color = AppStyle.colors.success.content)
                    AppButton({}, variant = ActionVariant.Primary) { AppText("Save") }
                    AppSelection(true, { AppText("Selected") }, {})
                    AppToggle("Checked", true, {}) { AppText("✓") }
                    AppChoice("Choice", listOf("id" to "Label")) {}
                    ReorderActions(true, {}, {}, {})
                } }
            }
        }
    }
}
@Composable private fun ConsumerDialogs() {
    AppConfirm("Title", "Message", "Confirm", "Dismiss", {}, {})
    AppSheet({}) { AppText("Sheet") }
}
