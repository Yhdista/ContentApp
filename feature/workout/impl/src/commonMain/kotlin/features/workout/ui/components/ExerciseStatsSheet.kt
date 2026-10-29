package features.workout.ui.components

import ui.designsystem.atoms.*
import ui.designsystem.molecules.*
import ui.designsystem.nucleus.AppStyle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import features.exercises.api.Exercise
import features.workout.domain.ExerciseStats
import ui.designsystem.nucleus.AppSpacing

@Composable
internal fun ExerciseStatsSheet(
    exercise: Exercise,
    stats: ExerciseStats?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = AppStyle.colors

    AppSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.lg)
                .padding(bottom = AppSpacing.xl)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .background(colorScheme.primaryContainer, RoundedCornerShape(AppSpacing.xs))
                            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xxs)
                    ) {
                        AppText(
                            text = exercise.muscleGroup.displayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                    AppText(
                        text = exercise.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                }

                AppIconAction(
                    label = "Zavřít statistiky",
                    onClick = onDismiss,
                    modifier = Modifier.size(AppSpacing.minInteractiveTouchTarget)
                ) {
                    AppText(
                        text = "✕",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.md))

            if (stats == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AppLoading(color = colorScheme.primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                ) {
                    // Quick stats 2x2 grid
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                            ) {
                                StatHighlightCard(
                                    icon = "🏆",
                                    label = "Max váha",
                                    value = if (stats.personalBestWeightKg > 0) "${stats.personalBestWeightKg} kg" else "-",
                                    subtext = if (stats.personalBestReps > 0) "${stats.personalBestReps} reps" else "",
                                    modifier = Modifier.weight(1f)
                                )
                                StatHighlightCard(
                                    icon = "🎯",
                                    label = "Odhad 1RM",
                                    value = if (stats.estimated1RMKg > 0) "${stats.estimated1RMKg} kg" else "-",
                                    subtext = "Epley vzorec",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                            ) {
                                StatHighlightCard(
                                    icon = "🏋️",
                                    label = "Celkový objem",
                                    value = if (stats.totalVolumeKg >= 1000) "${(stats.totalVolumeKg / 1000 * 10).toInt() / 10.0} t" else "${stats.totalVolumeKg.toInt()} kg",
                                    subtext = "Všechny tréninky",
                                    modifier = Modifier.weight(1f)
                                )
                                StatHighlightCard(
                                    icon = "📊",
                                    label = "Série celkem",
                                    value = "${stats.totalCompletedSets}",
                                    subtext = "${stats.sessionsHistory.size} tréninků",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Progress Chart
                    if (stats.sessionsHistory.isNotEmpty()) {
                        item {
                            ProgressionChart(entries = stats.sessionsHistory)
                        }
                    }

                    // History breakdown header
                    item {
                        AppText(
                            text = "HISTORIE VÝKONŮ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = AppSpacing.xs)
                        )
                    }

                    // History items
                    if (stats.sessionsHistory.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = AppSpacing.lg),
                                contentAlignment = Alignment.Center
                            ) {
                                AppText(
                                    text = "Zatím žádná historie pro tento cvik",
                                    fontSize = 13.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(stats.sessionsHistory) { entry ->
                            AppFrame(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(AppSpacing.sm),
                                containerColor = colorScheme.surfaceVariant
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
                                        AppText(
                                            text = entry.sessionTitle,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colorScheme.onSurface
                                        )
                                        AppText(
                                            text = "Max 1RM: ${((entry.max1RMKg * 10).toInt() / 10.0)} kg",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        AppText(
                                            text = "${entry.sets.size} sérií • ${entry.totalVolumeKg.toInt()} kg objem",
                                            fontSize = 11.sp,
                                            color = colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (entry.sets.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(AppSpacing.xs))
                                        entry.sets.forEach { s ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                AppText(
                                                    text = "Série ${s.setNumber}: ${s.weightKg} kg × ${s.reps} op.",
                                                    fontSize = 12.sp,
                                                    color = colorScheme.onSurface
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (s.isPersonalRecord) {
                                                        Box(
                                                            modifier = Modifier
                                                                .background(
                                                                    colorScheme.prGoldContainer,
                                                                    RoundedCornerShape(AppSpacing.xs)
                                                                )
                                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                        ) {
                                                            AppText(
                                                                text = "🏆 PR",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = colorScheme.prGold
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(AppSpacing.xs))
                                                    }
                                                    if (s.rpe != null) {
                                                        AppText(
                                                            text = "@${s.rpe}",
                                                            fontSize = 11.sp,
                                                            color = colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatHighlightCard(
    icon: String,
    label: String,
    value: String,
    subtext: String,
    modifier: Modifier = Modifier
) {
    val colorScheme = AppStyle.colors
    AppFrame(
        modifier = modifier,
        shape = RoundedCornerShape(AppSpacing.sm),
        containerColor = colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppText(text = icon, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(AppSpacing.xs))
                AppText(
                    text = label,
                    fontSize = 11.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(AppSpacing.xxs))
            AppText(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
            if (subtext.isNotEmpty()) {
                AppText(
                    text = subtext,
                    fontSize = 10.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
