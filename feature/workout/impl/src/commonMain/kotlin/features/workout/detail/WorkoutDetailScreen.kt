package features.workout.detail

import features.workout.ui.components.prGold
import features.workout.ui.components.prGoldContainer
import features.workout.ui.components.prGoldBorder
import ui.designsystem.atoms.*
import ui.designsystem.organisms.*
import ui.designsystem.templates.*
import ui.designsystem.nucleus.AppStyle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.common.formatCompactDecimal
import features.workout.domain.WorkoutSetType
import features.workout.ui.components.toBadgeVisuals
import ui.designsystem.nucleus.AppSpacing
import ui.designsystem.atoms.feedback.ErrorNotice
import ui.designsystem.templates.UniversalScreenTemplate

@Composable
internal fun WorkoutDetailScreen(
    state: WorkoutDetailContract.State,
    onIntent: (WorkoutDetailContract.Intent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHost: @Composable () -> Unit = {}
) {
    val colorScheme = AppStyle.colors
    val session = state.session

    UniversalScreenTemplate(
        snackbarHost = snackbarHost,
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = {
                    Column {
                        AppText(
                            text = session?.title ?: "Detail tréninku",
                            style = AppStyle.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onBackground
                        )
                        AppText(
                            text = session?.let { if(it.sourcePlan != null) "🗓️ Z plánu · revize ${it.sourcePlan?.planRevision}" else if(it.sourceRoutineId != null) "📋 Z rutiny · revize ${it.sourceRoutineRevision}" else "🏋️ Samostatný trénink" } ?: "Souhrn výkonu & sérií",
                            style = AppStyle.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    AppSurfaceAction(onClick = {
                                onIntent(WorkoutDetailContract.Intent.BackClicked)
                            }, enabled = true, label = "Zpět",
                        modifier = Modifier
                            .size(AppSpacing.minInteractiveTouchTarget)
                            ,
                        contentAlignment = Alignment.Center
                    ) {
                        AppText(
                            text = "←",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                    }
                },
                actions = {
                    AppSurfaceAction(onClick = { onIntent(WorkoutDetailContract.Intent.CreateRoutine) }, enabled = !state.isCreatingRoutine,
                        modifier = Modifier
                            .padding(end = AppSpacing.sm)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colorScheme.surfaceVariant)
                            .padding(horizontal = AppSpacing.sm, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AppText(
                            text = "📋 Vytvořit rutinu",
                            style = AppStyle.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                    }
                },
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
                    item { AppText("Pořadí cviků bylo obnoveno ze staršího záznamu.", style = AppStyle.typography.bodySmall) }
                }
                // 1. Karta souhrnu (Objem, Čas)
                item {
                    BoxWithConstraints(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colorScheme.surface)
                            .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                            .padding(AppSpacing.lg)
                    ) {
                        val volume: @Composable () -> Unit = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val volumeFormatted = (session?.totalVolumeKg ?: 0.0).formatCompactDecimal()
                                AppText("$volumeFormatted kg", fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold, color = colorScheme.primary)
                                AppText("Celkový objem", style = AppStyle.typography.labelSmall,
                                    color = colorScheme.onSurfaceVariant)
                            }
                        }
                        val duration: @Composable () -> Unit = {
                            val endTimeEpochMs = session?.endTimeEpochMs
                            val durationMinutes = if (session != null && endTimeEpochMs != null) {
                                "${(endTimeEpochMs - session.startTimeEpochMs) / 1000 / 60} min"
                            } else "—"
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                AppText(durationMinutes, fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                                AppText("Délka cvičení", style = AppStyle.typography.labelSmall,
                                    color = colorScheme.onSurfaceVariant)
                            }
                        }
                        if (LocalDensity.current.fontScale > 1.3f || maxWidth < 240.dp) {
                            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                volume()
                                duration()
                            }
                        } else {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically) {
                                volume()
                                Box(Modifier.height(44.dp).border(0.5.dp, colorScheme.outlineVariant, RoundedCornerShape(1.dp)))
                                duration()
                            }
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
                                AppText(
                                    text = "🏆",
                                    fontSize = 18.sp
                                )
                                AppText(
                                    text = "Nové osobní rekordy v tomto tréninku!",
                                    style = AppStyle.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.prGold
                                )
                            }
                            allPrSets.forEach { (exercise, set) ->
                                val weightStr = set.weightKg.formatCompactDecimal()
                                AppText(
                                    text = "• ${exercise.name}: $weightStr kg × ${set.reps} opakování",
                                    style = AppStyle.typography.bodySmall,
                                    color = colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // 2. Seznam cviků a sérií
                item {
                    AppText(
                        text = "PŘEDPIS A SKUTEČNOST",
                        style = AppStyle.typography.labelMedium,
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
                            AppText(
                                text = "Žádné zaznamenané cviky",
                                style = AppStyle.typography.bodyMedium,
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
    val colorScheme = AppStyle.colors

    AppCard(modifier) {
        // Both labels retain the card width when the effective text space is narrow.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val exercise: @Composable () -> Unit = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    AppText(group.exercise.name, style = AppStyle.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                    Box(Modifier.clip(RoundedCornerShape(4.dp))
                        .background(colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)) {
                        AppText(group.exercise.muscleGroup.displayName, style = AppStyle.typography.labelSmall,
                            color = colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            val completion: @Composable () -> Unit = {
                val completedSetsCount = group.sets.count { it.isCompleted }
                AppText("Skutečnost: $completedSetsCount / ${group.sets.size} sérií",
                    style = AppStyle.typography.labelMedium, color = colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold)
            }
            if (LocalDensity.current.fontScale > 1.3f || maxWidth < 240.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    exercise()
                    completion()
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    exercise()
                    completion()
                }
            }
        }

        Spacer(modifier = Modifier.height(AppSpacing.xs))

        if (group.prescriptions.isNotEmpty()) {
            AppText("Původní předpis · pauza ${group.prescriptions.first().restSeconds} s", style = AppStyle.typography.titleSmall)
            group.prescriptions.forEach { target ->
                val actual = group.sets.firstOrNull { it.id == target.id }
                AppText("${target.setNumber}. ${target.weightKg.formatCompactDecimal()} kg × ${target.reps} · ${target.setType.displayName} · " +
                    if (actual == null) "Vynecháno — bez skutečného výkonu" else "Skutečná série ${actual.setNumber}",
                    style = AppStyle.typography.bodySmall, color = colorScheme.onSurfaceVariant)
            }
            if (group.sets.isNotEmpty()) AppText("Skutečnost", style = AppStyle.typography.titleSmall)
        }
        // Seznam skutečných sérií; původní cíle nevstupují do počtů, objemu ani PR.
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            group.sets.forEach { set ->
                if (group.prescriptions.isEmpty()) AppText(
                    text = if (set.prescribedWeightKg != null && set.prescribedReps != null)
                        "Předpis: ${set.prescribedWeightKg} kg × ${set.prescribedReps} · ${set.prescribedSetType?.displayName ?: "—"}"
                        else "Předpis nebyl zaznamenán",
                    style = AppStyle.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant
                )
                val weightStr = set.weightKg.formatCompactDecimal()
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
                                .sizeIn(minWidth = 24.dp, minHeight = 24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(badgeVisuals.backgroundColor),
                            contentAlignment = Alignment.Center
                        ) {
                            AppText(
                                text = badgeVisuals.label,
                                style = AppStyle.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = badgeVisuals.contentColor
                            )
                        }

                        AppText(
                            text = "$weightStr kg × ${set.reps} reps$rpeStr",
                            style = AppStyle.typography.bodyMedium,
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
                                    AppText(
                                        text = "🏆 PR",
                                        style = AppStyle.typography.labelSmall,
                                        color = colorScheme.prGold,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colorScheme.success.container)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                AppText(
                                    text = "✓ Hotovo",
                                    style = AppStyle.typography.labelSmall,
                                    color = colorScheme.success.content,
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
