package core.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import org.koin.compose.getKoin
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import kotlin.reflect.KClass

/** Uses the navigation entry owner; only a real pop clears its ViewModels. */
@Composable
inline fun <reified T : ViewModel> rememberScreenViewModel(
    qualifier: Qualifier? = null,
    key: String? = null,
    noinline parameters: ParametersDefinition? = null
): T {
    val koin = getKoin()
    val vmKey = key ?: T::class.qualifiedName ?: T::class.simpleName ?: "default_vm"
    val store = checkNotNull(LocalViewModelStoreOwner.current) { "Screen requires a navigation entry" }.viewModelStore

    val viewModel = remember(store, vmKey) {
        val factory = object : ViewModelProvider.Factory {
            override fun <VM : ViewModel> create(modelClass: KClass<VM>, extras: CreationExtras): VM {
                @Suppress("UNCHECKED_CAST")
                return koin.get<T>(qualifier, parameters) as VM
            }
        }
        ViewModelProvider.create(store, factory)[vmKey, T::class]
    }

    return viewModel
}
