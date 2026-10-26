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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import features.workout.domain.WorkoutSession
import ui.designsystem.nucleus.AppColors
import ui.designsystem.nucleus.AppSpacing
import ui.designsystem.templates.UniversalScreenTemplate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistoryScreen(
    state: WorkoutHistoryContract.State,
    onIntent: (WorkoutHistoryContract.Intent) -> Unit,
    modifier: Modifier = Modifier
) {
    UniversalScreenTemplate(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Tréninky",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.OnBackground
                        )
                        Text(
                            text = "Přehled & historie (Hevy styl)",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.OnSurfaceVariant
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
                if (state.activeSession != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppColors.Primary)
                            .clickable(role = Role.Button) {
                                onIntent(WorkoutHistoryContract.Intent.ResumeActiveWorkout)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Pokračovat v tréninku ⚡",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.OnPrimary
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppColors.Primary)
                            .clickable(role = Role.Button) {
                                onIntent(WorkoutHistoryContract.Intent.StartNewWorkout)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+ Začít nový trénink 🏋️",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.OnPrimary
                        )
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
                // 1. Statistika objemu (Hero Card)
                item {
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
                            val volumeFormatted = if (state.totalVolumeAllTimeKg % 1.0 == 0.0) {
                                state.totalVolumeAllTimeKg.toInt().toString()
                            } else {
                                state.totalVolumeAllTimeKg.toString()
                            }
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
                                text = "${state.totalWorkoutsCount}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AppColors.OnSurface
                            )
                            Text(
                                text = "Tréninků",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.OnSurfaceVariant
                            )
                        }
                    }
                }

                // 2. Banner pro aktivní trénink (pokud existuje)
                if (state.activeSession != null) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(14.dp))
                                .background(AppColors.SurfaceVariant)
                                .border(1.5.dp, AppColors.Primary, RoundedCornerShape(14.dp))
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
                                    color = AppColors.Primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = state.activeSession.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.OnSurface
                                )
                            }
                            Text(
                                text = "Otevřít →",
                                style = MaterialTheme.typography.labelLarge,
                                color = AppColors.Primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 3. Hlavička sekce historie
                item {
                    Text(
                        text = "HISTORIE DOKONČENÝCH TRÉNINKŮ",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.OnSurfaceVariant,
                        modifier = Modifier.padding(top = AppSpacing.sm)
                    )
                }

                // 4. Seznam dokončených tréninků
                if (state.completedSessions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(AppColors.Surface)
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
                                    color = AppColors.OnSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(state.completedSessions, key = { it.id }) { session ->
                        WorkoutHistoryItemCard(session = session)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(AppSpacing.xxl))
                }
            }
        }
    )
}

@Composable
private fun WorkoutHistoryItemCard(
    session: WorkoutSession,
    modifier: Modifier = Modifier
) {
    val volumeFormatted = if (session.totalVolumeKg % 1.0 == 0.0) {
        session.totalVolumeKg.toInt().toString()
    } else {
        session.totalVolumeKg.toString()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.OutlineVariant, RoundedCornerShape(14.dp))
            .padding(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = session.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.OnSurface
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(AppColors.StatusSynced.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Dokončeno",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.StatusSynced,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Objem: $volumeFormatted kg",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.Primary
            )

            val durationMinutes = if (session.endTimeEpochMs != null) {
                val durationSec = (session.endTimeEpochMs - session.startTimeEpochMs) / 1000
                "${durationSec / 60} min"
            } else {
                "—"
            }

            Text(
                text = "Délka: $durationMinutes",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.OnSurfaceVariant
            )
        }
    }
}
