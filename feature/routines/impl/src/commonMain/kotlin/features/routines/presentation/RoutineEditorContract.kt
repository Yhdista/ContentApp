package features.routines.presentation

import androidx.compose.runtime.Stable
import core.mvi.*
import features.exercises.api.*

internal object RoutineEditorContract {
    @Stable data class State(val draft: RoutineDraft? = null, val choices: List<Exercise> = emptyList(),
        val isLoading: Boolean = true, val isSaving: Boolean = false, val error: String? = null,
        val canDiscard: Boolean = false, val conflict: Boolean = false, val confirmExit: Boolean = false, val dependentPlans: Int = 0) : UiState
    sealed interface Intent : UiIntent {
        data class Mutate(val transform: (RoutineDraft) -> RoutineDraft) : Intent
        data class Edit(val draft: RoutineDraft) : Intent
        data object Save : Intent
        data object SaveCopy : Intent
        data object Reload : Intent
        data object Retry : Intent
        data object Back : Intent
        data object Continue : Intent
        data object Discard : Intent
    }
    sealed interface Effect : UiEffect {
        data class Saved(val id: String) : Effect
        data object Back : Effect
    }
}
