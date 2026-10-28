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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.common.formatCleanWeight
import features.workout.domain.WorkoutSetType
import features.workout.ui.components.toBadgeVisuals
import ui.designsystem.nucleus.AppSpacing
import ui.designsystem.nucleus.isLight
import ui.designsystem.nucleus.prGold
import ui.designsystem.nucleus.prGoldBorder
import ui.designsystem.nucleus.prGoldContainer
import ui.designsystem.nucleus.statusSynced
import ui.designsystem.nucleus.statusSyncedContainer
import ui.designsystem.atoms.feedback.ErrorNotice
import ui.designsystem.templates.UniversalScreenTemplate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkoutDetailScreen(
    state: WorkoutDetailContract.State,
    onIntent: (WorkoutDetailContract.Intent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHost: @Composable () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val session = state.session

    UniversalScreenTemplate(
        snackbarHost = snackbarHost,
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = session?.title ?: "Detail tréninku",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onBackground
                        )
                        Text(
                            text = session?.let { if(it.sourcePlan != null) "🗓️ Z plánu · revize ${it.sourcePlan?.planRevision}" else if(it.sourceRoutineId != null) "📋 Z rutiny · revize ${it.sourceRoutineRevision}" else "🏋️ Samostatný trénink" } ?: "Souhrn výkonu & sérií",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant
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
                            color = colorScheme.primary
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .padding(end = AppSpacing.sm)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colorScheme.surfaceVariant)
                            .clickable(enabled = !state.isCreatingRoutine, role = Role.Button) { onIntent(WorkoutDetailContract.Intent.CreateRoutine) }
                            .padding(horizontal = AppSpacing.sm, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📋 Vytvořit rutinu",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.background
                )
            )
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
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                state.error?.let { error ->
                    item { ErrorNotice(error.asString(), onRetry = { onIntent(WorkoutDetailContract.Intent.LoadDetail(state.requestedSessionId ?: return@ErrorNotice)) }) }
                }
                if (state.exercisesWithSets.any { group -> group.sets.any { it.orderReconstructed } }) {
                    item { Text("Pořadí cviků bylo obnoveno ze staršího záznamu.", style = MaterialTheme.typography.bodySmall) }
                }
                // 1. Karta souhrnu (Objem, Čas)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colorScheme.surface)
                            .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                            .padding(AppSpacing.lg),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val volumeFormatted = (session?.totalVolumeKg ?: 0.0).formatCleanWeight()
                            Text(
                                text = "$volumeFormatted kg",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = colorScheme.primary
                            )
                            Text(
                                text = "Celkový objem",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(44.dp)
                                .border(0.5.dp, colorScheme.outlineVariant, RoundedCornerShape(1.dp))
                        )

                        val endTimeEpochMs = session?.endTimeEpochMs
                        val durationMinutes = if (session != null && endTimeEpochMs != null) {
                            val durationSec = (endTimeEpochMs - session.startTimeEpochMs) / 1000
                            "${durationSec / 60} min"
                        } else {
                            "—"
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = durationMinutes,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "Délka cvičení",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurfaceVariant
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
                                .background(colorScheme.prGoldContainer)
                                .border(1.dp, colorScheme.prGoldBorder, RoundedCornerShape(16.dp))
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
                                    color = colorScheme.prGold
                                )
                            }
                            allPrSets.forEach { (exercise, set) ->
                                val weightStr = set.weightKg.formatCleanWeight()
                                Text(
                                    text = "• ${exercise.name}: $weightStr kg × ${set.reps} opakování",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // 2. Seznam cviků a sérií
                item {
                    Text(
                        text = "PŘEDPIS A SKUTEČNOST",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                if (state.exercisesWithSets.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colorScheme.surface)
                                .padding(AppSpacing.lg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Žádné zaznamenané cviky",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(state.exercisesWithSets, key = { it.entryId }) { group ->
                        DetailExerciseCard(group = group)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(AppSpacing.xl))
                }
            }
        }
    )

}

@Composable
private fun DetailExerciseCard(
    group: WorkoutDetailContract.ExerciseWithSets,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.surface)
            .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        // Hlavička cviku
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = group.exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = group.exercise.muscleGroup.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            val completedSetsCount = group.sets.count { it.isCompleted }
            Text(
                text = "Skutečnost: $completedSetsCount / ${group.sets.size} sérií",
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(AppSpacing.xs))

        if (group.prescriptions.isNotEmpty()) {
            Text("Původní předpis · pauza ${group.prescriptions.first().restSeconds} s", style = MaterialTheme.typography.titleSmall)
            group.prescriptions.forEach { target ->
                val actual = group.sets.firstOrNull { it.id == target.id }
                Text("${target.setNumber}. ${target.weightKg.formatCleanWeight()} kg × ${target.reps} · ${target.setType.displayName} · " +
                    if (actual == null) "Vynecháno — bez skutečného výkonu" else "Skutečná série ${actual.setNumber}",
                    style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
            }
            if (group.sets.isNotEmpty()) Text("Skutečnost", style = MaterialTheme.typography.titleSmall)
        }
        // Seznam skutečných sérií; původní cíle nevstupují do počtů, objemu ani PR.
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            group.sets.forEach { set ->
                if (group.prescriptions.isEmpty()) Text(
                    text = if (set.prescribedWeightKg != null && set.prescribedReps != null)
                        "Předpis: ${set.prescribedWeightKg} kg × ${set.prescribedReps} · ${set.prescribedSetType?.displayName ?: "—"}"
                        else "Předpis nebyl zaznamenán",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant
                )
                val weightStr = set.weightKg.formatCleanWeight()
                val rpeStr = set.rpe?.let { " @$it" } ?: ""

                val badgeVisuals = set.setType.toBadgeVisuals(
                    setNumber = set.setNumber,
                    isCompleted = set.isCompleted,
                    isLight = colorScheme.isLight
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = AppSpacing.sm, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(badgeVisuals.backgroundColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = badgeVisuals.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = badgeVisuals.contentColor
                            )
                        }

                        Text(
                            text = "$weightStr kg × ${set.reps} reps$rpeStr",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface
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
                                        .background(colorScheme.prGoldContainer)
                                        .border(1.dp, colorScheme.prGoldBorder, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "🏆 PR",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colorScheme.prGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colorScheme.statusSyncedContainer)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "✓ Hotovo",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.statusSynced,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
