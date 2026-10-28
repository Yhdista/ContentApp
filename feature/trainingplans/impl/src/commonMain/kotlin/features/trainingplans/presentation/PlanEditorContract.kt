package features.trainingplans.presentation

import androidx.compose.runtime.Stable
import core.mvi.*
import features.routines.api.*

internal object PlanEditorContract {
    @Stable data class State(val draft: PlanDraft? = null, val choices: List<Routine> = emptyList(),
        val isLoading: Boolean = true, val isSaving: Boolean = false, val error: String? = null, val choicesError: String? = null,
        val canDiscard: Boolean = false, val conflict: Boolean = false, val confirmExit: Boolean = false, val dependentPlans: Int = 0) : UiState
    sealed interface Intent : UiIntent {
        data class Mutate(val transform: (PlanDraft) -> PlanDraft) : Intent
        data class Edit(val draft: PlanDraft) : Intent
        data object Save : Intent
        data object SaveCopy : Intent
        data object Reload : Intent
        data object RetryChoices : Intent
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
