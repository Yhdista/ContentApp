package features.workout.history

import ui.designsystem.atoms.*
import ui.designsystem.organisms.*
import ui.designsystem.templates.*
import ui.designsystem.nucleus.AppStyle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import core.common.formatCompactDecimal
import ui.designsystem.atoms.feedback.StatusTone
import features.workout.ui.components.ExercisePickerSheet
import features.workout.ui.components.ExerciseStatsSheet
import features.workout.ui.components.WeeklyStreakCard
import features.workout.ui.components.WorkoutExportDialog
import features.workout.ui.components.WorkoutHistoryItemCard
import ui.designsystem.atoms.feedback.StatusBadge
import ui.designsystem.nucleus.AppSpacing
import ui.designsystem.atoms.feedback.ErrorNotice
import ui.designsystem.templates.UniversalScreenTemplate

@Composable
internal fun WorkoutHistoryScreen(
    state: WorkoutHistoryContract.State,
    onIntent: (WorkoutHistoryContract.Intent) -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true,
    onToggleTheme: () -> Unit = {},
    snackbarHost: (@Composable () -> Unit)? = null
) {
    val colorScheme = AppStyle.colors

    UniversalScreenTemplate(
        modifier = modifier,
        snackbarHost = snackbarHost,
        topBar = {
            AppTopBar(
                title = {
                    Column {
                        AppText(
                            text = "Tréninky",
                            style = AppStyle.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onBackground
                        )
                        AppText(
                            text = "Přehled & historie (Hevy styl)",
                            style = AppStyle.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                    ) {
                        // Tlačítko pro přepnutí Dark / Light mode (☀️ / 🌙)
                        AppIconAction(label = "Přepnout barevné téma",
                            onClick = onToggleTheme,
                            modifier = Modifier.size(AppSpacing.minInteractiveTouchTarget)
                        ) {
                            AppText(if (isDarkTheme) "☀️" else "🌙", fontSize = 18.sp)
                        }

                        AppIconAction(label = "Zobrazit katalog cviků",
                            onClick = { onIntent(WorkoutHistoryContract.Intent.OpenExerciseCatalog) },
                            modifier = Modifier.size(AppSpacing.minInteractiveTouchTarget)
                        ) {
                            AppText("📈", fontSize = 18.sp)
                        }

                        AppIconAction(label = "Exportovat tréninky",
                            onClick = { onIntent(WorkoutHistoryContract.Intent.OpenExportDialog) },
                            modifier = Modifier.size(AppSpacing.minInteractiveTouchTarget)
                        ) {
                            AppText("💾", fontSize = 18.sp)
                        }

                        Box(modifier = Modifier.padding(end = AppSpacing.sm)) {
                            if (state.pendingSyncCount > 0) {
                                StatusBadge("PENDING", StatusTone.Pending)
                            } else {
                                StatusBadge("LOKÁLNĚ", StatusTone.Neutral)
                            }
                        }
                    }
                },
            )
        },
        thumbActionZone = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.md)
            ) {
                if (state.activeSession != null) {
                    AppSurfaceAction(onClick = {
                                onIntent(WorkoutHistoryContract.Intent.ResumeActiveWorkout)
                            }, enabled = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colorScheme.primary)
                            ,
                        contentAlignment = Alignment.Center
                    ) {
                        AppText(
                            text = "Pokračovat v tréninku ⚡",
                            style = AppStyle.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onPrimary
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                    ) {
                        AppSurfaceAction(onClick = {
                                    onIntent(WorkoutHistoryContract.Intent.StartNewWorkout)
                                }, enabled = true,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colorScheme.primary)
                                ,
                            contentAlignment = Alignment.Center
                        ) {
                            AppText(
                                text = "+ Nový trénink",
                                style = AppStyle.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onPrimary
                            )
                        }

                        AppSurfaceAction(onClick = {
                                    onIntent(WorkoutHistoryContract.Intent.OpenRoutines)
                                }, enabled = true,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colorScheme.surface)
                                .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                                ,
                            contentAlignment = Alignment.Center
                        ) {
                            AppText(
                                text = "📋 Rutiny",
                                style = AppStyle.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                        }
                    }
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
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                state.error?.let { error ->
                    item { ErrorNotice(error.asString(), onRetry = { onIntent(WorkoutHistoryContract.Intent.LoadHistory) }) }
                }
                // 1. Týdenní aktivita & streak (pokud je k dispozici)
                state.weeklyActivity?.let { activity ->
                    item {
                        WeeklyStreakCard(summary = activity)
                    }
                }

                // 2. Statistika objemu (Hero Card)
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
                            val volumeFormatted = state.totalVolumeAllTimeKg.formatCompactDecimal()
                            AppText(
                                text = "$volumeFormatted kg",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = colorScheme.primary
                            )
                            AppText(
                                text = "Celkový objem",
                                style = AppStyle.typography.labelSmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .border(0.5.dp, colorScheme.outlineVariant, RoundedCornerShape(1.dp))
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AppText(
                                text = "${state.totalWorkoutsCount}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = colorScheme.onSurface
                            )
                            AppText(
                                text = "Tréninků",
                                style = AppStyle.typography.labelSmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 3. Banner pro aktivní trénink (pokud existuje)
                if (state.activeSession != null) {
                    item {
                        AppCard(onClick = { onIntent(WorkoutHistoryContract.Intent.ResumeActiveWorkout) }) {
                            Column {
                                AppText(
                                    text = "🔴 PROBÍHÁ TRÉNINK",
                                    style = AppStyle.typography.labelSmall,
                                    color = colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                AppText(
                                    text = state.activeSession.title,
                                    style = AppStyle.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface
                                )
                            }
                            AppText(
                                text = "Otevřít →",
                                style = AppStyle.typography.labelLarge,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 4. Hlavička sekce historie a vyhledávání
                item {
                    AppText(
                        text = "HISTORIE DOKONČENÝCH TRÉNINKŮ",
                        style = AppStyle.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = AppSpacing.sm)
                    )
                }

                // 5. Pole vyhledávání (Filtrování historie tréninků)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colorScheme.surface)
                            .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .padding(horizontal = AppSpacing.md, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                    ) {
                        AppText("🔍", fontSize = 16.sp)
                        AppInput("Hledat trénink nebo cvik...", state.searchQuery, modifier = Modifier.weight(1f)) {
                            onIntent(WorkoutHistoryContract.Intent.UpdateSearchQuery(it))
                        }

                        if (state.searchQuery.isNotEmpty()) {
                            AppSurfaceAction(onClick = { onIntent(WorkoutHistoryContract.Intent.ClearSearchQuery) }, enabled = true,
                                modifier = Modifier
                                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                    .clip(CircleShape)
                                    .background(colorScheme.surfaceVariant)
                                    ,
                                contentAlignment = Alignment.Center
                            ) {
                                AppText(
                                    text = "✕",
                                    fontSize = 12.sp,
                                    color = colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 6. Seznam dokončených tréninků (respektující vyhledávací filtr)
                val displaySessions = state.filteredCompletedSessions
                if (state.completedSessions.isEmpty()) {
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
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                            ) {
                                AppText(text = "📋", fontSize = 32.sp)
                                AppText(
                                    text = "Zatím žádné dokončené tréninky",
                                    style = AppStyle.typography.bodyMedium,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else if (displaySessions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colorScheme.surface)
                                .padding(AppSpacing.lg),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                            ) {
                                AppText(text = "🔍", fontSize = 28.sp)
                                AppText(
                                    text = "Žádný trénink neodpovídá \"${state.searchQuery}\"",
                                    style = AppStyle.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface
                                )
                                AppText(
                                    text = "Zkuste jiný název tréninku nebo cviku",
                                    style = AppStyle.typography.labelSmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(displaySessions, key = { it.id }) { session ->
                        val exerciseNames = state.sessionExerciseNames[session.id].orEmpty()
                        WorkoutHistoryItemCard(
                            session = session,
                            exerciseNames = exerciseNames,
                            onClick = { onIntent(WorkoutHistoryContract.Intent.WorkoutClicked(session.id)) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(AppSpacing.xxl))
                }
            }
        }
    )



    if (state.isExerciseCatalogOpen) {
        ExercisePickerSheet(
            isVisible = true,
            exercises = state.allExercises,
            onSelectExercise = { exercise ->
                onIntent(WorkoutHistoryContract.Intent.CloseExerciseCatalog)
                onIntent(WorkoutHistoryContract.Intent.OpenExerciseStats(exercise))
            },
            onDismissRequest = { onIntent(WorkoutHistoryContract.Intent.CloseExerciseCatalog) }
        )
    }

    if (state.isExerciseStatsOpen && state.selectedStatsExercise != null) {
        ExerciseStatsSheet(
            exercise = state.selectedStatsExercise,
            stats = state.exerciseStats,
            onDismiss = { onIntent(WorkoutHistoryContract.Intent.CloseExerciseStats) }
        )
    }

    if (state.isExportDialogOpen) {
        WorkoutExportDialog(
            csvData = state.exportCsvData,
            jsonData = state.exportJsonData,
            sessionsCount = state.exportSessionsCount,
            setsCount = state.exportSetsCount,
            onCopyToClipboard = { text, isJson ->
                onIntent(WorkoutHistoryContract.Intent.CopyExportData(text, isJson))
            },
            onDismiss = { onIntent(WorkoutHistoryContract.Intent.CloseExportDialog) }
        )
    }
}
