package app.navigation

import androidx.compose.runtime.mutableStateListOf

sealed interface AppDestination {
    data object History : AppDestination
    data object ActiveWorkout : AppDestination
    data class Detail(val sessionId: String) : AppDestination
}

/** Small application-specific stack. Persist route arguments, reload business data from storage. */
class AppNavigationState private constructor(initial: List<AppDestination>) {
    constructor() : this(listOf(AppDestination.History))

    private val entries = mutableStateListOf<AppDestination>().apply { addAll(initial) }
    val current: AppDestination get() = entries.last()
    val canGoBack: Boolean get() = entries.size > 1

    fun navigate(destination: AppDestination) {
        if (current != destination) entries.add(destination)
    }

    fun back(): Boolean {
        if (!canGoBack) return false
        entries.removeAt(entries.lastIndex)
        return true
    }

    fun save(): List<String> = entries.map { destination ->
        when (destination) {
            AppDestination.History -> "history"
            AppDestination.ActiveWorkout -> "active"
            is AppDestination.Detail -> "detail:${destination.sessionId}"
        }
    }

    companion object {
        fun restore(keys: List<String>): AppNavigationState {
            val destinations = keys.map { key ->
                when {
                    key == "history" -> AppDestination.History
                    key == "active" -> AppDestination.ActiveWorkout
                    key.startsWith("detail:") && key.length > 7 -> AppDestination.Detail(key.removePrefix("detail:"))
                    else -> return AppNavigationState()
                }
            }
            return if (destinations.firstOrNull() == AppDestination.History) {
                AppNavigationState(destinations)
            } else AppNavigationState()
        }
    }
}
