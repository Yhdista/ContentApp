package app.navigation

import androidx.navigation.NavArgument
import androidx.navigation.NavGraph

/** Navigation 2.8.0-alpha10 on non-Android drops restored entries whose argument Bundle is null.
 * A harmless default argument keeps root/object destinations present in the framework's saved stack.
 * Remove after upgrading Navigation and rerunning TypedNavigationTest's host recreation scenario.
 */
internal fun NavGraph.withRestorableEntries(): NavGraph = apply {
    fun decorate(graph: NavGraph) {
        check("_hostEntry" !in graph.arguments) { "Reserved navigation argument collision" }
        graph.addArgument("_hostEntry", NavArgument.Builder().setDefaultValue(true).build())
        graph.forEach { destination ->
            if(destination is NavGraph) decorate(destination)
            else {
                check("_hostEntry" !in destination.arguments) { "Reserved navigation argument collision" }
                destination.addArgument("_hostEntry", NavArgument.Builder().setDefaultValue(true).build())
            }
        }
    }
    decorate(this)
}
