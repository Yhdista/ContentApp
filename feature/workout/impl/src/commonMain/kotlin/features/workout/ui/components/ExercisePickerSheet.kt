package features.workout.ui.components

import ui.designsystem.atoms.*
import ui.designsystem.molecules.*
import ui.designsystem.nucleus.AppStyle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import features.exercises.api.Exercise
import features.exercises.api.MuscleGroup
import ui.designsystem.nucleus.AppSpacing

@Composable
internal fun ExercisePickerSheet(
    isVisible: Boolean,
    exercises: List<Exercise>,
    onSelectExercise: (Exercise) -> Unit,
    onDismissRequest: () -> Unit,
    onCreateCustomExercise: (name: String, muscleGroup: MuscleGroup, defaultRestSeconds: Int) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    val colorScheme = AppStyle.colors
    var searchQuery by remember { mutableStateOf("") }
    var selectedMuscleGroup by remember { mutableStateOf<MuscleGroup?>(null) }
    var isCreateDialogOpen by remember { mutableStateOf(false) }

    AppSheet(
        onDismissRequest = onDismissRequest,

        modifier = modifier
    ) {
        val filteredExercises = remember(exercises, searchQuery, selectedMuscleGroup) {
            exercises.filter { exercise ->
                val matchesQuery = searchQuery.isBlank() || exercise.name.contains(searchQuery, ignoreCase = true)
                val matchesGroup = selectedMuscleGroup == null || exercise.muscleGroup == selectedMuscleGroup
                matchesQuery && matchesGroup
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppText(
                    text = "Přidat cvik",
                    style = AppStyle.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )

                AppSurfaceAction(onClick = { isCreateDialogOpen = true }, enabled = true,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AppText(
                        text = "+ Vlastní cvik",
                        style = AppStyle.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary
                    )
                }
            }

            AppInput("Hledat cvik...", searchQuery) { searchQuery = it }

            // Filtrovací čipy partií
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                contentPadding = PaddingValues(vertical = AppSpacing.xs)
            ) {
                item {
                    val isAllSelected = selectedMuscleGroup == null
                    AppSurfaceAction(onClick = { selectedMuscleGroup = null }, selected = isAllSelected,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isAllSelected) colorScheme.primary else colorScheme.surfaceVariant)
                            .padding(horizontal = AppSpacing.md, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AppText(
                            text = "Vše",
                            style = AppStyle.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isAllSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(MuscleGroup.entries.toTypedArray()) { group ->
                    val isSelected = selectedMuscleGroup == group
                    AppSurfaceAction(onClick = { selectedMuscleGroup = if (isSelected) null else group }, selected = isSelected,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) colorScheme.primary else colorScheme.surfaceVariant)
                            .padding(horizontal = AppSpacing.md, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AppText(
                            text = group.displayName,
                            style = AppStyle.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Seznam cviků
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                contentPadding = PaddingValues(bottom = AppSpacing.xl)
            ) {
                items(filteredExercises, key = { it.id }) { exercise ->
                    AppCard(onClick = { onSelectExercise(exercise) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            AppText(
                                text = exercise.name,
                                style = AppStyle.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.onSurface
                            )
                            AppText(
                                text = "${exercise.muscleGroup.displayName} • Pauza: ${exercise.defaultRestSeconds}s",
                                style = AppStyle.typography.labelSmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }

                        AppText(
                            text = "+",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                    }
                }

                // Tlačítko pro vytvoření vlastního cviku
                item {
                    AppSurfaceAction(onClick = { isCreateDialogOpen = true }, enabled = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(1.dp, colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
                        contentAlignment = Alignment.Center
                    ) {
                        AppText(
                            text = "+ Vytvořit nový cvik${if (searchQuery.isNotBlank()) " \"$searchQuery\"" else ""}",
                            style = AppStyle.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    if (isCreateDialogOpen) {
        CreateCustomExerciseDialog(
            initialName = searchQuery,
            onDismiss = { isCreateDialogOpen = false },
            onConfirm = { name, muscleGroup, restSeconds ->
                isCreateDialogOpen = false
                onCreateCustomExercise(name, muscleGroup, restSeconds)
            }
        )
    }
}
