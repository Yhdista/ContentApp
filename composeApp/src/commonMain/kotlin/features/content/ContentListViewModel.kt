package features.content

import androidx.lifecycle.viewModelScope
import core.repository.ContentRepository
import core.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class ContentListViewModel(
    private val repository: ContentRepository
) : BaseViewModel<ContentListContract.State, ContentListContract.Intent, ContentListContract.Effect>(
    initialState = ContentListContract.State()
) {

    init {
        onIntent(ContentListContract.Intent.LoadContent)
    }

    override fun handleIntent(intent: ContentListContract.Intent) {
        when (intent) {
            is ContentListContract.Intent.LoadContent -> loadContent()
            is ContentListContract.Intent.RefreshContent -> refreshContent()
            is ContentListContract.Intent.OnItemClick -> {
                sendEffect(ContentListContract.Effect.NavigateToDetail(intent.item))
            }
        }
    }

    private fun loadContent() {
        viewModelScope.launch {
            repository.getContentItems()
                .onStart { updateState { copy(isLoading = true) } }
                .catch { e -> updateState { copy(isLoading = false, error = e.message) } }
                .collect { items ->
                    updateState { copy(items = items, isLoading = false, error = null) }
                    if (items.isEmpty()) {
                        refreshContent()
                    }
                }
        }
    }

    private fun refreshContent() {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            try {
                repository.refreshContent()
                updateState { copy(isLoading = false) }
            } catch (e: Exception) {
                updateState { copy(isLoading = false, error = e.message) }
            }
        }
    }
}
