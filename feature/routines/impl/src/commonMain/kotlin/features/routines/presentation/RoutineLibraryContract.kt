package features.routines.presentation

import androidx.compose.runtime.Stable
import core.mvi.*
import features.routines.api.*
import features.exercises.api.*

internal object RoutineLibraryContract {
    @Stable data class State(val items: List<Routine> = emptyList(), val detail: Routine? = null,
        val choices: List<Exercise> = emptyList(), val dependentPlans: Int = 0,
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
    }
    sealed interface Effect : UiEffect {
        data object Workout : Effect
        data class Detail(val id: String) : Effect
    }
}
