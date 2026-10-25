package ui.designsystem.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ui.designsystem.nucleus.AppColors
import ui.designsystem.nucleus.AppSpacing

/**
 * Univerzální šablona obrazovky pro mobil i desktop:
 * - Ošetřuje systémové Insets a klávesnici (IME)
 * - Odděluje pozorovací zónu nahoře a akční zónu palce dole
 */
@Composable
fun UniversalScreenTemplate(
    topBar: @Composable () -> Unit,
    timerBar: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
    thumbActionZone: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = topBar,
        containerColor = AppColors.Background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding) // Ochrana před dvojím započtením
                .imePadding()                     // Posun nad klávesnici
        ) {
            // Pevná lišta sezení/stopek (pokud je aktivní)
            if (timerBar != null) {
                Box(modifier = Modifier.padding(horizontal = AppSpacing.lg, vertical = AppSpacing.xs)) {
                    timerBar()
                }
            }

            // Hlavní obsahová zóna (Feed / Seznam)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                content(PaddingValues(0.dp))
            }

            // Akční zóna palce (Thumb Zone - spodní třetina pro rychlý záznam jednou rukou)
            if (thumbActionZone != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.Surface)
                        .padding(AppSpacing.lg),
                    contentAlignment = Alignment.Center
                ) {
                    thumbActionZone()
                }
            }
        }
    }
}
