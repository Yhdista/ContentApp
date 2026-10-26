import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import core.di.initKoin
import features.workout.navigation.WorkoutAppNavHost
import ui.designsystem.nucleus.AppTheme

fun main() = application {
    // Inicializace Koin DI kontejneru pro Desktop
    initKoin()

    Window(
        onCloseRequest = ::exitApplication,
        title = "OmniSync • Workout Tracker (Desktop)",
        state = rememberWindowState(width = 460.dp, height = 860.dp)
    ) {
        AppTheme(darkTheme = true) {
            WorkoutAppNavHost()
        }
    }
}
