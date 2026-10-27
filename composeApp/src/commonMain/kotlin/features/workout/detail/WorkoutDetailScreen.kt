package features.workout.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.designsystem.nucleus.AppColors
import ui.designsystem.nucleus.AppSpacing
import features.workout.domain.WorkoutSetType
import ui.designsystem.templates.UniversalScreenTemplate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    state: WorkoutDetailContract.State,
    onIntent: (WorkoutDetailContract.Intent) -> Unit,
    modifier: Modifier = Modifier
) {
    val session = state.session
    var showSaveDialog by remember { mutableStateOf(false) }
    var templateNameInput by remember(session?.title) { mutableStateOf(session?.title ?: "Můj trénink") }

    UniversalScreenTemplate(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = session?.title ?: "Detail tréninku",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.OnBackground
                        )
                        Text(
                            text = "Souhrn výkonu & sérií",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.OnSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(AppSpacing.minInteractiveTouchTarget)
                            .clickable(role = Role.Button) {
                                onIntent(WorkoutDetailContract.Intent.BackClicked)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "←",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Primary
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(role = Role.Button) {
                                showSaveDialog = true
                            }
                            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📋 Šablona",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.Primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Background
                )
            )
        },
        thumbActionZone = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.md)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppColors.SurfaceVariant)
                        .border(1.dp, AppColors.Primary, RoundedCornerShape(14.dp))
                        .clickable(role = Role.Button) {
                            onIntent(WorkoutDetailContract.Intent.BackClicked)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Zpět do historie",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Primary
                    )
                }
            }
        },
        content = { contentPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentPadding = PaddingValues(
                    horizontal = AppSpacing.lg,
                    vertical = AppSpacing.md
                ),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
            ) {
                // 1. Souhrnná karta tréninku
                item {
                    val volumeFormatted = session?.let {
                        if (it.totalVolumeKg % 1.0 == 0.0) it.totalVolumeKg.toInt().toString() else it.totalVolumeKg.toString()
                    } ?: "0"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AppColors.Surface)
                            .border(1.dp, AppColors.Outline, RoundedCornerShape(16.dp))
                            .padding(AppSpacing.lg),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$volumeFormatted kg",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AppColors.Primary
                            )
                            Text(
                                text = "Celkový objem",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.OnSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .border(0.5.dp, AppColors.Outline, RoundedCornerShape(1.dp))
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.durationFormatted,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.OnSurface
                            )
                            Text(
                                text = "Délka cvičení",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.OnSurfaceVariant
                            )
                        }
                    }
                }

                // 1.5 Osobní rekordy (PR)
                val allPrSets = state.exercisesWithSets.flatMap { group ->
                    group.sets.filter { it.isPersonalRecord }.map { group.exercise to it }
                }
                if (allPrSets.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF26200A))
                                .border(1.dp, Color(0xFFFBBF24).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .padding(AppSpacing.md),
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                            ) {
                                Text(
                                    text = "🏆",
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = "Nové osobní rekordy v tomto tréninku!",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFBBF24)
                                )
                            }
                            allPrSets.forEach { (exercise, set) ->
                                val weightStr = if (set.weightKg % 1.0 == 0.0) set.weightKg.toInt().toString() else set.weightKg.toString()
                                Text(
                                    text = "• ${exercise.name}: $weightStr kg × ${set.reps} opakování",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppColors.OnSurface
                                )
                            }
                        }
                    }
                }

                // 2. Seznam cviků a sérií
                item {
                    Text(
                        text = "ROZPIS ODCVIČENÝCH CVIKŮ",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.OnSurfaceVariant
                    )
                }

                if (state.exercisesWithSets.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(AppColors.Surface)
                                .padding(AppSpacing.lg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "V tomto tréninku nebyly zaznamenány žádné série.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.OnSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(state.exercisesWithSets, key = { it.exercise.id }) { item ->
                        ExerciseDetailCard(item = item)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(AppSpacing.xxl))
                }
            }
        }
    )

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = {
                Text(
                    text = "Uložit jako šablonu",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.OnSurface
                )
            },
            text = {
                Column {
                    Text(
                        text = "Zadejte název šablony pro budoucí tréninky:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.OnSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.sm))
                    OutlinedTextField(
                        value = templateNameInput,
                        onValueChange = { templateNameInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSaveDialog = false
                        val finalName = templateNameInput.ifBlank { session?.title ?: "Šablona" }
                        onIntent(WorkoutDetailContract.Intent.SaveAsTemplate(finalName))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                ) {
                    Text("Uložit", color = AppColors.OnPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Zrušit", color = AppColors.OnSurfaceVariant)
                }
            },
            containerColor = AppColors.SurfaceVariant
        )
    }
}

@Composable
private fun ExerciseDetailCard(
    item: WorkoutDetailContract.ExerciseWithSets,
    modifier: Modifier = Modifier
) {
    val volumeFormatted = if (item.exerciseVolumeKg % 1.0 == 0.0) {
        item.exerciseVolumeKg.toInt().toString()
    } else {
        item.exerciseVolumeKg.toString()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Outline, RoundedCornerShape(16.dp))
            .padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        // Hlavička cviku
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = item.exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.OnSurface
                )
                Text(
                    text = "${item.exercise.muscleGroup.displayName} • Objem cviku: $volumeFormatted kg",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.Primary
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Seznam sérií
        item.sets.forEach { set ->
            val weightStr = if (set.weightKg % 1.0 == 0.0) set.weightKg.toInt().toString() else set.weightKg.toString()
            val rpeStr = set.rpe?.let { " • @$it" } ?: ""

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppColors.SurfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val (badgeText, badgeBg, badgeTextColor) = when (set.setType) {
                        WorkoutSetType.NORMAL -> Triple(
                            "${set.setNumber}",
                            AppColors.SurfaceVariant,
                            AppColors.OnSurface
                        )
                        WorkoutSetType.WARMUP -> Triple(
                            "W",
                            Color(0xFFF59E0B).copy(alpha = 0.2f),
                            Color(0xFFF59E0B)
                        )
                        WorkoutSetType.DROPSET -> Triple(
                            "D",
                            Color(0xFF6366F1).copy(alpha = 0.2f),
                            Color(0xFF818CF8)
                        )
                        WorkoutSetType.FAILURE -> Triple(
                            "F",
                            Color(0xFFEF4444).copy(alpha = 0.2f),
                            Color(0xFFEF4444)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor
                        )
                    }

                    Text(
                        text = "$weightStr kg × ${set.reps} reps$rpeStr",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.OnSurface
                    )
                }

                if (set.isCompleted) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (set.isPersonalRecord) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF451A03))
                                    .border(1.dp, Color(0xFFFBBF24), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "🏆 PR",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFBBF24),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AppColors.StatusSynced.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "✓ Hotovo",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.StatusSynced,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
