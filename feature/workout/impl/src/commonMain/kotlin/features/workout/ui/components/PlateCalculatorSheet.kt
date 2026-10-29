package features.workout.ui.components

import ui.designsystem.atoms.*
import ui.designsystem.molecules.*
import ui.designsystem.nucleus.AppStyle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import features.workout.domain.BarbellPlate
import features.workout.domain.PlateCalculator
import ui.designsystem.nucleus.AppSpacing

internal data class PlateSpec(
    val weightKg: Double,
    val color: Color,
    val textColor: Color,
    val heightDp: Int
)

internal fun BarbellPlate.toSpec(): PlateSpec = when (weightKg) {
    25.0 -> PlateSpec(25.0, Color(0xFFB91C1C), Color.White, 76)
    20.0 -> PlateSpec(20.0, Color(0xFF2563EB), Color.White, 68)
    15.0 -> PlateSpec(15.0, Color(0xFFEAB308), Color.Black, 60)
    10.0 -> PlateSpec(10.0, Color(0xFF166534), Color.White, 52)
    5.0 -> PlateSpec(5.0, Color(0xFFE2E8F0), Color.Black, 44)
    2.5 -> PlateSpec(2.5, Color(0xFF334155), Color.White, 36)
    else -> PlateSpec(weightKg, Color(0xFF94A3B8), Color.Black, 28)
}

internal val STANDARD_PLATES = PlateCalculator.STANDARD_AVAILABLE_PLATES.map { it.toSpec() }

internal fun calculatePlatesPerSide(totalWeightKg: Double, barWeightKg: Double = 20.0): List<PlateSpec> {
    return PlateCalculator.calculatePlatesPerSide(totalWeightKg, barWeightKg).map { it.toSpec() }
}

@Composable
internal fun PlateCalculatorSheet(
    isVisible: Boolean,
    initialWeightKg: Double = 60.0,
    onDismissRequest: () -> Unit,
    onApplyWeight: ((Double) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    val colorScheme = AppStyle.colors

    AppSheet(
        onDismissRequest = onDismissRequest,

        modifier = modifier
    ) {
        var totalWeight by remember(initialWeightKg) {
            mutableStateOf(if (initialWeightKg <= 0.0) 60.0 else initialWeightKg)
        }
        var barWeight by remember { mutableStateOf(20.0) }

        val plates = remember(totalWeight, barWeight) {
            calculatePlatesPerSide(totalWeight, barWeight)
        }

        val weightPerSide = ((totalWeight - barWeight) / 2.0).coerceAtLeast(0.0)
        val formattedTotal = if (totalWeight % 1.0 == 0.0) totalWeight.toInt().toString() else totalWeight.toString()
        val formattedPerSide = if (weightPerSide % 1.0 == 0.0) weightPerSide.toInt().toString() else weightPerSide.toString()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            // Hlavička
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    AppText(
                        text = "Kalkulátor kotoučů",
                        style = AppStyle.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    AppText(
                        text = "Rozpad zátěže na olympijskou osu",
                        style = AppStyle.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                // Výběr osy (20kg / 15kg / 10kg)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(20.0 to "20kg", 15.0 to "15kg", 10.0 to "10kg").forEach { (weight, label) ->
                        val isSelected = barWeight == weight
                        AppSurfaceAction(onClick = { barWeight = weight }, selected = barWeight == weight,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) colorScheme.primary else colorScheme.surfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AppText(
                                text = label,
                                style = AppStyle.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Velký displej váhy
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colorScheme.surface)
                    .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(AppSpacing.md),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AppText(
                        text = "$formattedTotal kg",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colorScheme.primary
                    )
                    AppText(
                        text = "Celková váha",
                        style = AppStyle.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .border(0.5.dp, colorScheme.outlineVariant, RoundedCornerShape(1.dp))
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AppText(
                        text = "$formattedPerSide kg",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    AppText(
                        text = "Na jedné straně",
                        style = AppStyle.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }

            // Tlačítka pro rychlou úpravu váhy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
            ) {
                listOf(-10.0, -5.0, -2.5, 2.5, 5.0, 10.0).forEach { delta ->
                    val sign = if (delta > 0) "+$delta" else "$delta"
                    val label = if (delta % 1.0 == 0.0) sign.replace(".0", "") else sign
                    AppSurfaceAction(onClick = {
                                totalWeight = (totalWeight + delta).coerceAtLeast(barWeight)
                            }, enabled = true,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colorScheme.surfaceVariant)
                            ,
                        contentAlignment = Alignment.Center
                    ) {
                        AppText(
                            text = label,
                            style = AppStyle.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (delta > 0) colorScheme.primary else colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Grafická vizualizace činky s kotouči
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(horizontal = AppSpacing.md),
                contentAlignment = Alignment.CenterStart
            ) {
                // Osa (tyč)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(Color(0xFF64748B))
                )

                // Objímka osy (collar)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height(50.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF334155))
                    )

                    // Kotouče na ose
                    if (plates.isEmpty()) {
                        AppText(
                            text = "Pouze prázdná osa ($barWeight kg)",
                            style = AppStyle.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    } else {
                        plates.forEach { plate ->
                            val plateWeightStr = if (plate.weightKg % 1.0 == 0.0) {
                                plate.weightKg.toInt().toString()
                            } else {
                                plate.weightKg.toString()
                            }
                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .height(plate.heightDp.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(plate.color)
                                    .border(0.5.dp, Color.Black.copy(alpha = 0.3f), RoundedCornerShape(3.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                AppText(
                                    text = plateWeightStr,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = plate.textColor,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Textový rozpis
            if (plates.isNotEmpty()) {
                val grouped = plates.groupBy { it.weightKg }
                val summaryText = grouped.entries.joinToString(", ") { (weight, list) ->
                    val wStr = if (weight % 1.0 == 0.0) weight.toInt().toString() else weight.toString()
                    "${list.size}× ${wStr}kg"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppText(
                        text = "Kotouče na každou stranu:",
                        style = AppStyle.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant
                    )
                    AppText(
                        text = summaryText,
                        style = AppStyle.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.xs))

            // Tlačítko Použít váhu / Zavřít
            AppSurfaceAction(onClick = {
                        if (onApplyWeight != null) {
                            onApplyWeight(totalWeight)
                        }
                        onDismissRequest()
                    }, enabled = true, label = "Zavřít",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorScheme.primary)
                    ,
                contentAlignment = Alignment.Center
            ) {
                AppText(
                    text = if (onApplyWeight != null) "Použít $formattedTotal kg do série" else "Hotovo",
                    style = AppStyle.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))
        }
    }
}
