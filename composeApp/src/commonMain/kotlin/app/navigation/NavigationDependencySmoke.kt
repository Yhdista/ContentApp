package app.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

@Serializable
internal data class NavigationSmokeRoute(val id: String)

/** Compile the typed Navigation Compose APIs on every app target before the navigation migration. */
internal fun NavGraphBuilder.navigationDependencySmoke() {
    composable<NavigationSmokeRoute> { entry ->
        entry.toRoute<NavigationSmokeRoute>()
    }
}
