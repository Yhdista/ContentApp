package features.universal

import core.model.ContentItem
import core.mvi.UiEffect
import core.mvi.UiIntent
import core.mvi.UiState

class UniversalContract {
    data class State(
        val items: List<ContentItem> = emptyList(),
        val isLoading: Boolean = false,
        val isSyncing: Boolean = false,
        val lastSyncLog: String = "Čeká na synchronizaci...",
        val timerSeconds: Long = 0L,
        val isTimerRunning: Boolean = false,
        val counterValue: Int = 5,
        val selectedCategory: String = "Workout",
        val error: String? = null
    ) : UiState

    sealed class Intent : UiIntent {
        data object LoadInitial : Intent()
        data object StartTimer : Intent()
        data object PauseTimer : Intent()
        data object ResetTimer : Intent()
        data class SetCounter(val value: Int) : Intent()
        data class SelectCategory(val category: String) : Intent()
        data object AddQuickRecord : Intent()
        data object ScreenPaused : Intent()
    }

    sealed class Effect : UiEffect {
        data class ShowSnackbar(val message: String) : Effect()
        data class NavigateToDetail(val id: Int) : Effect()
    }
}
