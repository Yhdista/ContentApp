package core.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

/** Entry-owned effects must not navigate while an outgoing screen is still composed.
 * Buffered channel effects wait for this entry to resume; a real pop clears its ViewModel. */
@Composable
fun <T> CollectScreenEffects(effects: Flow<T>, onEffect: suspend CoroutineScope.(T) -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentHandler by rememberUpdatedState(onEffect)
    LaunchedEffect(effects, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            effects.collect { currentHandler(it) }
        }
    }
}
