package core.common

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Aplikační scope pro asynchronní operace a zápisy, které MUSÍ přežít
 * opuštění obrazovky nebo zánik ViewModelu (např. uložení sezení, odbavení outboxu).
 */
class ApplicationScope(
    context: CoroutineContext = SupervisorJob() + Dispatchers.Default
) : CoroutineScope by CoroutineScope(context)
