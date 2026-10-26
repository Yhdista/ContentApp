package features.workout.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import features.workout.domain.Exercise
import features.workout.domain.ExerciseStats
import features.workout.domain.PersonalRecordCalculator
import ui.designsystem.nucleus.AppColors
import ui.designsystem.nucleus.AppSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseStatsSheet(
    exercise: Exercise?,
    stats: ExerciseStats?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    if (exercise == null) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppColors.Surface,
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
                            .background(AppColors.PrimaryContainer, RoundedCornerShape(AppSpacing.xs))
                            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xxs)
                    ) {
                        Text(
                            text = exercise.muscleGroup.displayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.OnPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(AppSpacing.xs))
                    Text(
                        text = exercise.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.OnSurface
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(AppSpacing.minInteractiveTouchTarget)
                ) {
                    Text(
                        text = "✕",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.OnSurfaceVariant
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
                    CircularProgressIndicator(color = AppColors.Primary)
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
                                    value = if (stats.estimated1RMKg > 0) "${((stats.estimated1RMKg * 10).toInt() / 10.0)} kg" else "-",
                                    subtext = "Epley vzorec",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                            ) {
                                StatHighlightCard(
                                    icon = "📊",
                                    label = "Celkový objem",
                                    value = "${stats.totalVolumeKg.toInt()} kg",
                                    subtext = "všechna sezení",
                                    modifier = Modifier.weight(1f)
                                )
                                StatHighlightCard(
                                    icon = "🔢",
                                    label = "Dokončeno sérií",
                                    value = "${stats.totalCompletedSets}",
                                    subtext = "${stats.sessionsHistory.size} tréninků",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Progression Chart
                    item {
                        ProgressionChart(entries = stats.sessionsHistory)
                    }

                    // History section header
                    item {
                        Text(
                            text = "🗓 Historie tréninků (${stats.sessionsHistory.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.OnSurface,
                            modifier = Modifier.padding(top = AppSpacing.sm)
                        )
                    }

                    if (stats.sessionsHistory.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(AppSpacing.sm),
                                colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceVariant)
                            ) {
                                Text(
                                    text = "Zatím jste pro tento cvik nezaznamenali žádné dokončené série.",
                                    fontSize = 13.sp,
                                    color = AppColors.OnSurfaceVariant,
                                    modifier = Modifier.padding(AppSpacing.md)
                                )
                            }
                        }
                    } else {
                        items(stats.sessionsHistory, key = { it.sessionId }) { entry ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(AppSpacing.sm),
                                colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(AppSpacing.md)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = entry.sessionTitle,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AppColors.OnSurface
                                        )
                                        Text(
                                            text = "Max: ${entry.maxWeightKg} kg",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AppColors.Primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(AppSpacing.xs))

                                    // List of sets
                                    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                                        entry.sets.forEach { s ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Série ${s.setNumber}: ${s.weightKg} kg × ${s.reps} op.",
                                                    fontSize = 12.sp,
                                                    color = AppColors.OnSurface
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (s.isPersonalRecord) {
                                                        Box(
                                                            modifier = Modifier
                                                                .background(
                                                                    Color(0xFFFFD700).copy(alpha = 0.2f),
                                                                    RoundedCornerShape(AppSpacing.xs)
                                                                )
                                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = "🏆 PR",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFFFFD700)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(AppSpacing.xs))
                                                    }
                                                    if (s.rpe != null) {
                                                        Text(
                                                            text = "@${s.rpe}",
                                                            fontSize = 11.sp,
                                                            color = AppColors.OnSurfaceVariant
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
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(AppSpacing.sm),
        colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = icon, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(AppSpacing.xs))
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = AppColors.OnSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(AppSpacing.xxs))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.OnSurface
            )
            if (subtext.isNotEmpty()) {
                Text(
                    text = subtext,
                    fontSize = 10.sp,
                    color = AppColors.OnSurfaceVariant
                )
            }
        }
    }
}
