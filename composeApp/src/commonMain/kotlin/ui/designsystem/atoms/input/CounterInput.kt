package ui.designsystem.atoms.input

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.designsystem.atoms.button.AppIconButton
import ui.designsystem.nucleus.AppColors
import ui.designsystem.nucleus.AppSpacing

/**
 * Aktivní číselný vstup pro rychlé klikání palcem jednou rukou (Hevy styl).
 * Obsahuje dvě velká 48dp tlačítka (-) a (+) a jasně čitelnou hodnotu.
 */
@Composable
fun CounterInput(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minValue: Int = 1,
    maxValue: Int = 999
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Outline, RoundedCornerShape(16.dp))
            .padding(AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        // Tlačítko MÍNUS (48 dp)
        AppIconButton(
            onClick = { if (value > minValue) onValueChange(value - 1) },
            enabled = value > minValue,
            backgroundColor = AppColors.SurfaceVariant
        ) {
            Text("-", color = AppColors.OnSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        // Zobrazení hodnoty (pevná minimální šířka, velký font)
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = AppColors.Primary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 48.dp)
        )

        // Tlačítko PLUS (48 dp)
        AppIconButton(
            onClick = { if (value < maxValue) onValueChange(value + 1) },
            enabled = value < maxValue,
            backgroundColor = AppColors.Primary
        ) {
            Text("+", color = AppColors.OnPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}
