package core.mvi

interface UiState
interface UiIntent
interface UiEffect

interface MviContract<S : UiState, I : UiIntent, E : UiEffect> {
    val uiState: S
    fun onIntent(intent: I)
}
