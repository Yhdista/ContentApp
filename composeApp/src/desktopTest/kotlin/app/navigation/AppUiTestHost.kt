package app.navigation

import androidx.compose.runtime.Composable
import org.koin.compose.KoinIsolatedContext
import org.koin.compose.getKoin
import org.koin.core.KoinApplication
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.lifecycle.ViewModelStoreOwner
import kotlin.time.Duration.Companion.seconds

// Modern Skiko idle waits otherwise inherit an infinite deadline.
internal val appUiTestTimeout = 30.seconds

/** External owners outlive the entire UI runner, including its scene disposal and final frames. */
internal fun withAppUiTestResources(block: (AppUiTestResources) -> Unit) {
    val resources = AppUiTestResources()
    preservingFailure(cleanup = resources::close) { block(resources) }
}

internal class AppUiTestResources {
    private val actions = mutableListOf<() -> Unit>()
    fun afterSceneClosed(action: () -> Unit) { actions += action }
    fun close() = runAllCleanups(actions)
}

/** Scenario synchronization is unchanged; only teardown bypasses idle. */
@OptIn(ExperimentalTestApi::class)
internal class AppUiTestHost(
    private val ui: ComposeUiTest,
    private val owner: ViewModelStoreOwner,
    private val resources: AppUiTestResources
) {
    /** Bind each start/stop fixture explicitly; Koin's default Compose local can retain an older app. */
    @Composable fun Content(application: KoinApplication, content: @Composable () -> Unit) {
        KoinIsolatedContext(application) {
            check(getKoin() === application.koin) { "UI composition must use this fixture's Koin application" }
            content()
        }
    }

    fun run(beforeClear: () -> Unit = {}, afterClear: List<() -> Unit> = emptyList(), block: () -> Unit) {
        // Clearing VM owners and closing Koin while the runner still owns a scene can
        // recreate VMs against a closed scope. Register these for the OUTER finally.
        resources.afterSceneClosed { ui.runOnUiThread { owner.viewModelStore.clear() } }
        afterClear.forEach(resources::afterSceneClosed)
        preservingFailure(cleanup = {
            runAllCleanups(listOf(beforeClear, { ui.mainClock.autoAdvance = false }))
        }, block = block)
    }
}

private fun preservingFailure(cleanup: () -> Unit, block: () -> Unit) {
    var primary: Throwable? = null
    try {
        block()
    } catch (failure: Throwable) {
        primary = failure
        throw failure
    } finally {
        try {
            cleanup()
        } catch (failure: Throwable) {
            val original = primary
            if(original == null) throw failure
            if(failure !== original) original.addSuppressed(failure)
        }
    }
}

private fun runAllCleanups(actions: List<() -> Unit>) {
    var first: Throwable? = null
    actions.forEach { action ->
        try { action() } catch (failure: Throwable) {
            val original = first
            if(original == null) first = failure else if(failure !== original) original.addSuppressed(failure)
        }
    }
    first?.let { throw it }
}
