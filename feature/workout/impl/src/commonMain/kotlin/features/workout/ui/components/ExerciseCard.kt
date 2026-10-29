package features.workout.ui.components

import ui.designsystem.nucleus.ActionVariant

import ui.designsystem.atoms.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import features.exercises.api.Exercise
import features.workout.domain.WorkoutSet
import features.workout.domain.WorkoutSetType
import ui.designsystem.nucleus.AppSpacing

@Composable
internal fun ExerciseCard(
    exercise: Exercise,
    sets: List<WorkoutSet>,
    onAddSet: () -> Unit,
    onToggleSetComplete: (WorkoutSet) -> Unit,
    onWeightChange: (WorkoutSet, Double) -> Unit,
    onRepsChange: (WorkoutSet, Int) -> Unit,
    onRpeChange: (WorkoutSet, Double?) -> Unit,
    onDeleteSet: (WorkoutSet) -> Unit,
    onRemoveExercise: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenPlateCalculator: ((WorkoutSet) -> Unit)? = null,
    onShowStats: (() -> Unit)? = null,
    onSetTypeChange: ((WorkoutSet, WorkoutSetType) -> Unit)? = null
) {
    AppCard(modifier) {
        AppText(exercise.name, heading = true)
        AppText("${exercise.muscleGroup.displayName} · Pauza: ${exercise.defaultRestSeconds}s")
        if(onShowStats != null) AppIconAction("Statistiky cviku ${exercise.name}", onShowStats) { AppText("📈 Statistiky") }
        AppIconAction("Odebrat cvik ${exercise.name}", onRemoveExercise) { AppText("✕ Odebrat cvik") }
        sets.forEach { set ->
            androidx.compose.runtime.key(set.id) {
                SetRowItem(
                    set = set,
                    onToggleComplete = { onToggleSetComplete(set) },
                    onWeightChange = { onWeightChange(set, it) },
                    onRepsChange = { onRepsChange(set, it) },
                    onRpeChange = { onRpeChange(set, it) },
                    onDelete = { onDeleteSet(set) },
                    onOpenPlateCalculator = { onOpenPlateCalculator?.invoke(set) },
                    onSetTypeChange = { onSetTypeChange?.invoke(set, it) }
                )
            }
        }
        AppButton(onAddSet, variant = ActionVariant.Secondary) { AppText("+ Přidat sérii") }
    }
}
