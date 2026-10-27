package core.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import org.koin.compose.getKoin
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import kotlin.reflect.KClass

/**
 * Lifecycle-safe KMP ViewModel holder pro Compose obrazovky.
 *
 * Vytvoří [ViewModelStore] svázaný se životním cyklem dané obrazovky v kompozici
 * (např. uvnitř `AnimatedContent` v navigaci). Při opuštění obrazovky (`onDispose`)
 * automaticky zavolá [ViewModelStore.clear], což zruší `viewModelScope` a zavolá
 * `ViewModel.onCleared()`. Tím zabraňuje únikům paměti a běžícím korutinám na pozadí.
 */
@Composable
inline fun <reified T : ViewModel> rememberScreenViewModel(
    qualifier: Qualifier? = null,
    key: String? = null,
    noinline parameters: ParametersDefinition? = null
): T {
    val koin = getKoin()
    val vmKey = key ?: T::class.qualifiedName ?: T::class.simpleName ?: "default_vm"
    val store = remember(vmKey) { ViewModelStore() }

    val viewModel = remember(store, vmKey) {
        val factory = object : ViewModelProvider.Factory {
            override fun <VM : ViewModel> create(modelClass: KClass<VM>, extras: CreationExtras): VM {
                @Suppress("UNCHECKED_CAST")
                return koin.get<T>(qualifier, parameters) as VM
            }
        }
        ViewModelProvider.create(store, factory)[vmKey, T::class]
    }

    DisposableEffect(store) {
        onDispose {
            store.clear()
        }
    }

    return viewModel
}
