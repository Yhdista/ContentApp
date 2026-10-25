package features.content

import core.model.ContentItem
import core.mvi.UiEffect
import core.mvi.UiIntent
import core.mvi.UiState

class ContentListContract {
    data class State(
        val items: List<ContentItem> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null
    ) : UiState

    sealed class Intent : UiIntent {
        data object LoadContent : Intent()
        data object RefreshContent : Intent()
        data class OnItemClick(val item: ContentItem) : Intent()
    }

    sealed class Effect : UiEffect {
        data class ShowToast(val message: String) : Effect()
        data class NavigateToDetail(val item: ContentItem) : Effect()
    }
}
