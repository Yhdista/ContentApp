package app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.lifecycle.ViewModelStoreOwner
import kotlin.time.Duration.Companion.seconds

// Modern Skiko idle waits otherwise inherit an infinite deadline.
internal val appUiTestTimeout = 30.seconds

/** Only fixture teardown bypasses idle; all scenario synchronization remains explicit in each test. */
@OptIn(ExperimentalTestApi::class)
internal class AppUiTestHost(private val ui: ComposeUiTest, private val owner: ViewModelStoreOwner) {
    private var mounted by mutableStateOf(true)

    @Composable fun Content(content: @Composable () -> Unit) {
        if(mounted) content()
    }

    fun run(beforeClear: () -> Unit = {}, afterClear: List<() -> Unit> = emptyList(), block: () -> Unit) {
        var primaryFailure: Throwable? = null
        try {
            block()
        } catch (failure: Throwable) {
            primaryFailure = failure
            throw failure
        } finally {
            val failures = mutableListOf<Throwable>()
            fun cleanup(action: () -> Unit) { runCatching(action).onFailure { failures += it } }
            cleanup(beforeClear)
            cleanup { ui.mainClock.autoAdvance = false }
            // Waiting for idle here advances an outgoing composition while its owners are
            // still alive. Dispatch directly, then let the test runner dispose the scene.
            cleanup { ui.runOnUiThread { mounted = false; owner.viewModelStore.clear() } }
            afterClear.forEach { cleanup(it) }
            val primary = primaryFailure
            if(primary != null) {
                failures.forEach(primary::addSuppressed)
            } else if(failures.isNotEmpty()) {
                val first = failures.first()
                failures.drop(1).forEach(first::addSuppressed)
                throw first
            }
        }
    }
}
