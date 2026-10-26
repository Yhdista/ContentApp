package features.workout.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
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
import features.workout.domain.Exercise
import features.workout.domain.MuscleGroup
import ui.designsystem.nucleus.AppColors
import ui.designsystem.nucleus.AppSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerSheet(
    isVisible: Boolean,
    exercises: List<Exercise>,
    onSelectExercise: (Exercise) -> Unit,
    onDismissRequest: () -> Unit,
    onCreateCustomExercise: (name: String, muscleGroup: MuscleGroup, defaultRestSeconds: Int) -> Unit = { _, _, _ -> },
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    if (!isVisible) return

    var searchQuery by remember { mutableStateOf("") }
    var selectedMuscleGroup by remember { mutableStateOf<MuscleGroup?>(null) }
    var isCreateDialogOpen by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = AppColors.Background,
        contentColor = AppColors.OnBackground,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = AppSpacing.sm)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AppColors.Outline)
            )
        }
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
                Text(
                    text = "Přidat cvik",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.OnBackground
                )

                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppColors.PrimaryContainer.copy(alpha = 0.4f))
                        .clickable(role = Role.Button) { isCreateDialogOpen = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ Vlastní cvik",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Primary
                    )
                }
            }

            // Vyhledávací pole
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.Surface)
                    .border(1.dp, AppColors.Outline, RoundedCornerShape(12.dp))
                    .padding(horizontal = AppSpacing.md),
                contentAlignment = Alignment.CenterStart
            ) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Hledat cvik...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.OnSurfaceVariant
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = AppColors.OnBackground,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush = SolidColor(AppColors.Primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Filtrovací čipy partií
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                contentPadding = PaddingValues(vertical = AppSpacing.xs)
            ) {
                item {
                    val isAllSelected = selectedMuscleGroup == null
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isAllSelected) AppColors.Primary else AppColors.SurfaceVariant)
                            .clickable { selectedMuscleGroup = null }
                            .padding(horizontal = AppSpacing.md, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Vše",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isAllSelected) AppColors.OnPrimary else AppColors.OnSurfaceVariant
                        )
                    }
                }

                items(MuscleGroup.entries.toTypedArray()) { group ->
                    val isSelected = selectedMuscleGroup == group
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) AppColors.Primary else AppColors.SurfaceVariant)
                            .clickable { selectedMuscleGroup = if (isSelected) null else group }
                            .padding(horizontal = AppSpacing.md, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = group.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) AppColors.OnPrimary else AppColors.OnSurfaceVariant
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.Surface)
                            .border(1.dp, AppColors.OutlineVariant, RoundedCornerShape(12.dp))
                            .clickable(role = Role.Button) { onSelectExercise(exercise) }
                            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = exercise.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.OnSurface
                            )
                            Text(
                                text = "${exercise.muscleGroup.displayName} • Pauza: ${exercise.defaultRestSeconds}s",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.OnSurfaceVariant
                            )
                        }

                        Text(
                            text = "+",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Primary
                        )
                    }
                }

                // Tlačítko pro vytvoření vlastního cviku
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.SurfaceVariant.copy(alpha = 0.5f))
                            .border(1.dp, AppColors.Primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .clickable(role = Role.Button) { isCreateDialogOpen = true }
                            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+ Vytvořit nový cvik${if (searchQuery.isNotBlank()) " \"$searchQuery\"" else ""}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Primary
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
