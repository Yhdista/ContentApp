package features.trainingplans.presentation

import androidx.compose.runtime.Stable
import core.mvi.*
import features.trainingplans.api.*
import features.routines.api.*

internal object PlanLibraryContract {
    @Stable data class State(val items: List<TrainingPlan> = emptyList(), val detail: TrainingPlan? = null,
        val choices: List<Routine> = emptyList(), val progress: PlanProgress? = null, val activeId: String? = null,
        val isLoading: Boolean = true, val busy: Boolean = false, val error: String? = null,
        val warning: String? = null, val resumed: Boolean = false, val archiveConfirmation: Boolean = false) : UiState
    sealed interface Intent : UiIntent {
        data object Retry : Intent
        data object Start : Intent
        data object Resume : Intent
        data object Duplicate : Intent
        data object Archive : Intent
        data object ConfirmArchive : Intent
        data object CancelArchive : Intent
        data object SelectActive : Intent
        data object ClearActive : Intent
        data object Skip : Intent
        data class ConfirmNext(val slotId: String) : Intent
        data class StartStandalone(val routineId: String) : Intent
    }
    sealed interface Effect : UiEffect {
        data object Workout : Effect
        data class Detail(val id: String) : Effect
    }
}
