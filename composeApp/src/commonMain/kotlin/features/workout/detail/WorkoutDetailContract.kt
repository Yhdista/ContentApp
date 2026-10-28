package features.workout.detail

import androidx.compose.runtime.Stable
import core.mvi.UiEffect
import core.mvi.UiIntent
import core.mvi.UiState
import core.ui.UiText
import features.workout.domain.Exercise
import features.workout.domain.WorkoutSession
import features.workout.domain.WorkoutSet

class WorkoutDetailContract {
    data class ExerciseWithSets(
        val exercise: Exercise,
        val sets: List<WorkoutSet>,
        val exerciseVolumeKg: Double
    )

    @Stable
    data class State(
        val session: WorkoutSession? = null,
        val requestedSessionId: String? = null,
        val exercisesWithSets: List<ExerciseWithSets> = emptyList(),
        val durationFormatted: String = "—",
        val isLoading: Boolean = false,
        val error: UiText? = null
    ) : UiState

    sealed class Intent : UiIntent {
        data class LoadDetail(val sessionId: String) : Intent()
        data object BackClicked : Intent()
        data class SaveAsTemplate(val templateName: String) : Intent()
    }

    sealed class Effect : UiEffect {
        data object NavigateBack : Effect()
        data class ShowSnackbar(val message: String) : Effect()
    }
}
