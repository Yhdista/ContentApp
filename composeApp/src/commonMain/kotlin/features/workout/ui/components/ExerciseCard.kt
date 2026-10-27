package features.workout.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import features.workout.domain.Exercise
import features.workout.domain.WorkoutSet
import features.workout.domain.WorkoutSetType
import ui.designsystem.nucleus.AppColors
import ui.designsystem.nucleus.AppSpacing

@Composable
fun ExerciseCard(
    exercise: Exercise,
    sets: List<WorkoutSet>,
    onAddSet: () -> Unit,
    onToggleSetComplete: (WorkoutSet) -> Unit,
    onWeightChange: (WorkoutSet, Double) -> Unit,
    onRepsChange: (WorkoutSet, Int) -> Unit,
    onRpeChange: (WorkoutSet, Double?) -> Unit,
    onDeleteSet: (WorkoutSet) -> Unit,
    onRemoveExercise: () -> Unit,
    onOpenPlateCalculator: ((WorkoutSet) -> Unit)? = null,
    onShowStats: (() -> Unit)? = null,
    onSetTypeChange: ((WorkoutSet, WorkoutSetType) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
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
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.OnSurface
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Partie
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AppColors.PrimaryContainer.copy(alpha = 0.4f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = exercise.muscleGroup.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.Primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Výchozí pauza
                    Text(
                        text = "Pauza: ${exercise.defaultRestSeconds}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.OnSurfaceVariant
                    )
                }
            }

            // Akce v hlavičce cviku (Statistiky, Odebrat)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onShowStats != null) {
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(role = Role.Button, onClick = onShowStats)
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📈",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button, onClick = onRemoveExercise)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✕",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppColors.OnSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(AppSpacing.xs))

        // Hlavička sloupců
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SET",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.OnSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(28.dp),
                textAlign = TextAlign.Center
            )
            Text(
                text = "PŘEDCHOZÍ",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.OnSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(64.dp),
                textAlign = TextAlign.Center
            )
            Text(
                text = "KG",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.OnSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(62.dp),
                textAlign = TextAlign.Center
            )
            Text(
                text = "REPS",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.OnSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(52.dp),
                textAlign = TextAlign.Center
            )
            Text(
                text = "RPE",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.OnSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(44.dp),
                textAlign = TextAlign.Center
            )
            Text(
                text = "✓",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.OnSurfaceVariant,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(44.dp),
                textAlign = TextAlign.Center
            )
        }

        // Seznam jednotlivých sérií
        sets.forEach { set ->
            SetRowItem(
                set = set,
                onToggleComplete = { onToggleSetComplete(set) },
                onWeightChange = { newWeight -> onWeightChange(set, newWeight) },
                onRepsChange = { newReps -> onRepsChange(set, newReps) },
                onRpeChange = { newRpe -> onRpeChange(set, newRpe) },
                onDelete = { onDeleteSet(set) },
                onOpenPlateCalculator = { onOpenPlateCalculator?.invoke(set) },
                onSetTypeChange = { newType -> onSetTypeChange?.invoke(set, newType) }
            )
        }

        // Tlačítko + Přidat sérii
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(10.dp))
                .background(AppColors.SurfaceVariant.copy(alpha = 0.5f))
                .clickable(role = Role.Button, onClick = onAddSet)
                .padding(vertical = AppSpacing.sm),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+ Přidat sérii",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.Primary
            )
        }
    }
}
