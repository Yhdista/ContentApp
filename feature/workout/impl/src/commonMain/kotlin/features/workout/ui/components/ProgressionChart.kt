package features.workout.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import features.workout.domain.ExerciseHistorySessionEntry
import ui.designsystem.nucleus.AppSpacing
import ui.designsystem.nucleus.statusPending
import ui.designsystem.nucleus.statusSynced

@Composable
internal fun ProgressionChart(
    entries: List<ExerciseHistorySessionEntry>,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val sortedEntries = entries.sortedBy { it.dateEpochMs }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppSpacing.md),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📈 Vývoj odhadovaného 1RM",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .background(colorScheme.primaryContainer, RoundedCornerShape(AppSpacing.xs))
                        .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xxs)
                ) {
                    Text(
                        text = "Epley vzorec",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            if (sortedEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Zatím žádná data pro graf",
                        fontSize = 13.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            } else if (sortedEntries.size == 1) {
                val single = sortedEntries.first()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(colorScheme.surface, RoundedCornerShape(AppSpacing.sm)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${((single.max1RMKg * 10).toInt() / 10.0)} kg",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                        Text(
                            text = "První zaznamenaný trénink (${single.sessionTitle})",
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Multi-point interactive canvas
                val values = sortedEntries.map { it.max1RMKg }
                val minVal = (values.minOrNull() ?: 0.0) * 0.95
                val maxVal = (values.maxOrNull() ?: 100.0) * 1.05
                val range = (maxVal - minVal).coerceAtLeast(1.0)

                val primaryColor = colorScheme.primary
                val outlineColor = colorScheme.outlineVariant
                val surfaceBg = colorScheme.surface

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val paddingHorizontal = 24.dp.toPx()
                    val paddingTop = 20.dp.toPx()
                    val paddingBottom = 20.dp.toPx()
                    val plotW = w - paddingHorizontal * 2
                    val plotH = h - paddingTop - paddingBottom

                    // Draw 3 subtle horizontal grid lines
                    val gridColor = outlineColor.copy(alpha = 0.5f)
                    for (i in 0..2) {
                        val y = paddingTop + plotH * (i / 2f)
                        drawLine(
                            color = gridColor,
                            start = Offset(paddingHorizontal, y),
                            end = Offset(w - paddingHorizontal, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Compute points
                    val points = sortedEntries.mapIndexed { index, entry ->
                        val x = paddingHorizontal + plotW * (index.toFloat() / (sortedEntries.size - 1))
                        val normY = (entry.max1RMKg - minVal) / range
                        val y = paddingTop + plotH * (1f - normY.toFloat())
                        Offset(x, y)
                    }

                    // Build fill path
                    val fillPath = Path().apply {
                        moveTo(points.first().x, h - paddingBottom)
                        points.forEach { lineTo(it.x, it.y) }
                        lineTo(points.last().x, h - paddingBottom)
                        close()
                    }

                    val gradientBrush = Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.35f),
                            primaryColor.copy(alpha = 0.02f)
                        ),
                        startY = paddingTop,
                        endY = h - paddingBottom
                    )
                    drawPath(path = fillPath, brush = gradientBrush)

                    // Build stroke line
                    val linePath = Path().apply {
                        points.forEachIndexed { i, pt ->
                            if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                        }
                    }
                    drawPath(
                        path = linePath,
                        color = primaryColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw point circles
                    points.forEach { pt ->
                        drawCircle(
                            color = surfaceBg,
                            radius = 6.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = primaryColor,
                            radius = 4.dp.toPx(),
                            center = pt
                        )
                    }
                }

                // Bottom labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.sm),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val first = sortedEntries.first()
                    val last = sortedEntries.last()
                    Text(
                        text = "Start: ${((first.max1RMKg * 10).toInt() / 10.0)} kg",
                        fontSize = 11.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                    val diff = last.max1RMKg - first.max1RMKg
                    val diffText = if (diff >= 0) "+${((diff * 10).toInt() / 10.0)} kg" else "${((diff * 10).toInt() / 10.0)} kg"
                    val diffColor = if (diff >= 0) colorScheme.statusSynced else colorScheme.statusPending
                    Text(
                        text = "Aktuálně: ${((last.max1RMKg * 10).toInt() / 10.0)} kg ($diffText)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = diffColor
                    )
                }
            }
        }
    }
}
