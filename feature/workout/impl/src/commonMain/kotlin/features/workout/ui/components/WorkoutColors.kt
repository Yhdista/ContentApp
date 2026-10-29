package features.workout.ui.components
import androidx.compose.ui.graphics.Color
import ui.designsystem.nucleus.AppColorTokens
/** Personal-record presentation stays in workout, outside the global design tokens. */
internal val AppColorTokens.prGold: Color get() = if(isLight) Color(0xFF854D0E) else Color(0xFFFBBF24)
internal val AppColorTokens.prGoldContainer: Color get() = if(isLight) Color(0xFFFEF3C7) else Color(0xFF451A03)
internal val AppColorTokens.prGoldBorder: Color get() = prGold
