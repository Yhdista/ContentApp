package features.universal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ui.designsystem.atoms.button.PrimaryButton
import ui.designsystem.atoms.input.CounterInput
import ui.designsystem.molecules.RecordCard
import ui.designsystem.molecules.SessionTimerBar
import ui.designsystem.nucleus.AppColors
import ui.designsystem.nucleus.AppSpacing
import ui.designsystem.templates.UniversalScreenTemplate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalScreen(
    state: UniversalContract.State,
    onIntent: (UniversalContract.Intent) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf("Workout", "Běh", "Úkol", "Kontrola")

    UniversalScreenTemplate(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "OmniSync • Universal PoC",
                            style = MaterialTheme.typography.titleLarge,
                            color = AppColors.OnSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = state.lastSyncLog,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.isSyncing) AppColors.StatusPending else AppColors.Primary,
                            fontSize = 11.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Background
                )
            )
        },
        timerBar = {
            SessionTimerBar(
                secondsElapsed = state.timerSeconds,
                isRunning = state.isTimerRunning,
                onStart = { onIntent(UniversalContract.Intent.StartTimer) },
                onPause = { onIntent(UniversalContract.Intent.PauseTimer) },
                onReset = { onIntent(UniversalContract.Intent.ResetTimer) }
            )
        },
        content = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AppSpacing.lg),
                contentPadding = PaddingValues(vertical = AppSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                item {
                    Text(
                        text = "Lokální záznamy (${state.items.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = AppColors.OnSurfaceVariant
                    )
                }

                items(state.items, key = { it.id }) { item ->
                    RecordCard(
                        item = item,
                        onClick = { /* Detail položky */ }
                    )
                }
            }
        },
        thumbActionZone = {
            // Akční zóna palce (spodní třetina pro rychlý záznam jednou rukou)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                // Výběr kategorie
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    items(categories) { category ->
                        val isSelected = category == state.selectedCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) AppColors.Primary else AppColors.SurfaceVariant)
                                .clickable { onIntent(UniversalContract.Intent.SelectCategory(category)) }
                                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm)
                        ) {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) AppColors.OnPrimary else AppColors.OnSurface
                            )
                        }
                    }
                }

                // Spodní ovládací řádek: Čítač + Uložit do Outboxu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CounterInput(
                        value = state.counterValue,
                        onValueChange = { onIntent(UniversalContract.Intent.SetCounter(it)) }
                    )

                    PrimaryButton(
                        text = "Zapsat do Outboxu",
                        onClick = { onIntent(UniversalContract.Intent.AddQuickRecord) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    )
}
