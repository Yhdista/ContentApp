import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import core.di.initKoin
import features.universal.UniversalRoot
import features.universal.UniversalViewModel
import org.koin.core.context.GlobalContext
import ui.designsystem.nucleus.AppTheme

fun main() = application {
    // Inicializace Koin DI kontejneru pro Desktop
    initKoin()

    val koin = GlobalContext.get()
    val viewModel = koin.get<UniversalViewModel>()

    Window(
        onCloseRequest = ::exitApplication,
        title = "OmniSync • Universal PoC (Desktop)",
        state = rememberWindowState(width = 460.dp, height = 860.dp)
    ) {
        AppTheme(darkTheme = true) {
            UniversalRoot(viewModel = viewModel)
        }
    }
}
