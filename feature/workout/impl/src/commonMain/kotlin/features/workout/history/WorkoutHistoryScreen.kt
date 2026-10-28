package features.workout.history

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
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
import core.model.ItemSyncStatus
import features.workout.ui.components.ExercisePickerSheet
import features.workout.ui.components.ExerciseStatsSheet
import features.workout.ui.components.WeeklyStreakCard
import features.workout.ui.components.WorkoutExportDialog
import features.workout.ui.components.WorkoutHistoryItemCard
import ui.designsystem.atoms.feedback.StatusBadge
import ui.designsystem.nucleus.AppSpacing
import ui.designsystem.atoms.feedback.ErrorNotice
import ui.designsystem.templates.UniversalScreenTemplate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkoutHistoryScreen(
    state: WorkoutHistoryContract.State,
    onIntent: (WorkoutHistoryContract.Intent) -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true,
    onToggleTheme: () -> Unit = {},
    snackbarHost: (@Composable () -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme

    UniversalScreenTemplate(
        modifier = modifier,
        snackbarHost = snackbarHost,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Tréninky",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onBackground
                        )
                        Text(
                            text = "Přehled & historie (Hevy styl)",
                            style = MaterialTheme.typography.bodySmall,
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
                        IconButton(
                            onClick = onToggleTheme,
                            modifier = Modifier.size(AppSpacing.minInteractiveTouchTarget)
                        ) {
                            Text(if (isDarkTheme) "☀️" else "🌙", fontSize = 18.sp)
                        }

                        IconButton(
                            onClick = { onIntent(WorkoutHistoryContract.Intent.OpenExerciseCatalog) },
                            modifier = Modifier.size(AppSpacing.minInteractiveTouchTarget)
                        ) {
                            Text("📈", fontSize = 18.sp)
                        }

                        IconButton(
                            onClick = { onIntent(WorkoutHistoryContract.Intent.OpenExportDialog) },
                            modifier = Modifier.size(AppSpacing.minInteractiveTouchTarget)
                        ) {
                            Text("💾", fontSize = 18.sp)
                        }

                        Box(modifier = Modifier.padding(end = AppSpacing.sm)) {
                            if (state.pendingSyncCount > 0) {
                                StatusBadge(status = ItemSyncStatus.PENDING)
                            } else {
                                StatusBadge(status = ItemSyncStatus.LOCAL)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.background
                )
            )
        },
        thumbActionZone = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.md)
            ) {
                if (state.activeSession != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colorScheme.primary)
                            .clickable(role = Role.Button) {
                                onIntent(WorkoutHistoryContract.Intent.ResumeActiveWorkout)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Pokračovat v tréninku ⚡",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onPrimary
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1.3f)
                                .minimumInteractiveComponentSize()
                                .height(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colorScheme.primary)
                                .clickable(role = Role.Button) {
                                    onIntent(WorkoutHistoryContract.Intent.StartNewWorkout)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+ Nový trénink",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .height(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colorScheme.surface)
                                .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                                .clickable(role = Role.Button) {
                                    onIntent(WorkoutHistoryContract.Intent.OpenRoutines)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "📋 Rutiny",
                                style = MaterialTheme.typography.titleSmall,
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
                            Text(
                                text = "$volumeFormatted kg",
                                fontSize = 24.sp,
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
                                .height(40.dp)
                                .border(0.5.dp, colorScheme.outlineVariant, RoundedCornerShape(1.dp))
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${state.totalWorkoutsCount}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "Tréninků",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 3. Banner pro aktivní trénink (pokud existuje)
                if (state.activeSession != null) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(14.dp))
                                .background(colorScheme.surfaceVariant)
                                .border(1.5.dp, colorScheme.primary, RoundedCornerShape(14.dp))
                                .clickable(role = Role.Button) {
                                    onIntent(WorkoutHistoryContract.Intent.ResumeActiveWorkout)
                                }
                                .padding(AppSpacing.md),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "🔴 PROBÍHÁ TRÉNINK",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = state.activeSession.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Otevřít →",
                                style = MaterialTheme.typography.labelLarge,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 4. Hlavička sekce historie a vyhledávání
                item {
                    Text(
                        text = "HISTORIE DOKONČENÝCH TRÉNINKŮ",
                        style = MaterialTheme.typography.labelMedium,
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
                        Text("🔍", fontSize = 16.sp)
                        BasicTextField(
                            value = state.searchQuery,
                            onValueChange = { onIntent(WorkoutHistoryContract.Intent.UpdateSearchQuery(it)) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .padding(vertical = 12.dp),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = colorScheme.onSurface),
                            cursorBrush = SolidColor(colorScheme.primary),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                if (state.searchQuery.isEmpty()) {
                                    Text(
                                        text = "Hledat trénink nebo cvik...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                innerTextField()
                            }
                        )
                        if (state.searchQuery.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(colorScheme.surfaceVariant)
                                    .clickable(role = Role.Button) { onIntent(WorkoutHistoryContract.Intent.ClearSearchQuery) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
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
                                Text(text = "📋", fontSize = 32.sp)
                                Text(
                                    text = "Zatím žádné dokončené tréninky",
                                    style = MaterialTheme.typography.bodyMedium,
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
                                Text(text = "🔍", fontSize = 28.sp)
                                Text(
                                    text = "Žádný trénink neodpovídá \"${state.searchQuery}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "Zkuste jiný název tréninku nebo cviku",
                                    style = MaterialTheme.typography.labelSmall,
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
