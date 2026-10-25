package features.universal

import androidx.lifecycle.viewModelScope
import core.common.AppClock
import core.common.ApplicationScope
import core.repository.ContentRepository
import core.sync.SyncManager
import core.viewmodel.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class UniversalViewModel(
    private val repository: ContentRepository,
    private val syncManager: SyncManager,
    private val applicationScope: ApplicationScope,
    private val clock: AppClock
) : BaseViewModel<UniversalContract.State, UniversalContract.Intent, UniversalContract.Effect>(
    initialState = UniversalContract.State()
) {
    private var timerJob: Job? = null

    init {
        // 1. Spuštění Outbox synchronizační smyčky na pozadí
        syncManager.startSyncLoop()

        // 2. Sledování stavu synchronizace
        viewModelScope.launch {
            syncManager.isSyncing.collect { syncing ->
                updateState { copy(isSyncing = syncing) }
            }
        }
        viewModelScope.launch {
            syncManager.lastSyncLog.collect { log ->
                updateState { copy(lastSyncLog = log) }
            }
        }

        // 3. Načtení dat z Roomu
        handleIntent(UniversalContract.Intent.LoadInitial)
    }

    override fun handleIntent(intent: UniversalContract.Intent) {
        when (intent) {
            is UniversalContract.Intent.LoadInitial -> loadContent()
            is UniversalContract.Intent.StartTimer -> startTimer()
            is UniversalContract.Intent.PauseTimer -> pauseTimer()
            is UniversalContract.Intent.ResetTimer -> resetTimer()
            is UniversalContract.Intent.SetCounter -> updateState { copy(counterValue = intent.value) }
            is UniversalContract.Intent.SelectCategory -> updateState { copy(selectedCategory = intent.category) }
            is UniversalContract.Intent.AddQuickRecord -> addQuickRecord()
            is UniversalContract.Intent.ScreenPaused -> {
                pauseTimer()
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
                        repository.refreshContent()
                    }
                }
        }
    }

    private fun startTimer() {
        if (timerJob?.isActive == true) return
        updateState { copy(isTimerRunning = true) }
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                updateState { copy(timerSeconds = timerSeconds + 1) }
            }
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        timerJob = null
        updateState { copy(isTimerRunning = false) }
    }

    private fun resetTimer() {
        pauseTimer()
        updateState { copy(timerSeconds = 0) }
    }

    /**
     * Zápis v reálném čase (Hevy styl):
     * Spouští se v `ApplicationScope`, takže ani okamžité opuštění obrazovky
     * nepřeruší zápis do Roomu a do fronty mutací.
     */
    private fun addQuickRecord() {
        val currentState = uiState.value
        val title = "${currentState.selectedCategory} Sezení #${(10..99).random()}"
        val desc = "Záznam vytvořen v čase ${currentState.timerSeconds}s • hodnota: ${currentState.counterValue}"
        val value = currentState.counterValue
        val category = currentState.selectedCategory

        applicationScope.launch {
            repository.addRecord(
                title = title,
                description = desc,
                value = value,
                category = category
            )
        }

        sendEffect(UniversalContract.Effect.ShowSnackbar("Záznam vložen lokálně do Outboxu!"))
    }
}
