package core.viewmodel

import core.result.DataError
import core.result.Result
import core.result.RootError
import core.result.onFailure
import core.result.safeLocalCall
import core.storage.CommandCoordinator
import core.storage.OrderedDrafts
import kotlinx.coroutines.*

/** Feature codecs retain their raw payload/version contract; this controller owns its lifecycle. */
interface DraftCodec<D> {
    fun encode(draft: D): String
    fun decode(payload: String): D
}

/** One live editor, composed by feature presentation. All accepted writes belong to the app queue.
 * Only the terminal operation that acquired the lock may release it. Background failures never do.
 */
class DurableEditorController<D>(
    private val drafts: OrderedDrafts,
    private val writes: CommandCoordinator,
    private val kind: String,
    private val id: String,
    private val codec: DraftCodec<D>,
    private val initial: suspend (String) -> D,
) {
    private var session: OrderedDrafts.Session? = null
    private lateinit var scope: CoroutineScope
    private lateinit var failed: (DataError.Local) -> Unit
    private lateinit var busy: (Boolean) -> Unit
    private var terminal = false
    private var closed = false
    private var opening: Job? = null
    val isBusy: Boolean get() = terminal || closed

    fun bind(scope: CoroutineScope, onFailure: (DataError.Local) -> Unit, onBusy: (Boolean) -> Unit) {
        check(!this::scope.isInitialized) { "Controller already bound" }
        this.scope = scope
        failed = onFailure
        busy = onBusy
    }

    fun open(onOpened: (D) -> Unit, onLeaseAcquired: () -> Unit = {}, onAlreadyOpen: () -> Unit) {
        if (isBusy || opening?.isActive == true) return
        opening = operation {
            val opened = session ?: try {
                drafts.open(kind, id, scope.coroutineContext.job).await().also { session = it }
            } catch (_: core.storage.DraftAlreadyOpen) {
                onAlreadyOpen()
                return@operation
            }
            onLeaseAcquired()
            val draft = opened.payload?.let(codec::decode) ?: initial(id)
            opened.put(codec.encode(draft)).await()
            onOpened(draft)
        }
    }

    /** Enqueue before launching a waiter, so owner cancellation cannot lose an accepted edit. */
    fun edit(draft: D, onSaved: () -> Unit = {}) {
        if (isBusy) return
        val ack = session?.put(codec.encode(draft)) ?: return
        operation { ack.await(); onSaved() }
    }

    fun barrier() {
        if (isBusy) return
        val ack = session?.barrier() ?: return
        operation { ack.await() }
    }

    fun reload(load: suspend () -> D, onLoaded: (D) -> Unit) {
        val opened = session ?: return
        if (!lock()) return
        terminalOperation {
            val draft = load()
            opened.put(codec.encode(draft)).await()
            onLoaded(draft)
        }
    }

    fun discard(onDiscarded: () -> Unit) {
        val opened = session ?: return
        if (!lock()) return
        val ack = opened.discard()
        terminalOperation {
            ack.await()
            closed = true
            onDiscarded()
        }
    }

    private class Rejected(val result: Result.Error<*>) : RuntimeException()

    fun <T, E : RootError> publish(save: suspend () -> Result<T, E>, onResult: (Result<T, E>) -> Unit) {
        val opened = session ?: return
        if (!lock()) return
        val ack = opened.publish(writes) {
            when (val result = save()) {
                is Result.Success -> result
                is Result.Error -> throw Rejected(result)
            }
        }
        terminalOperation {
            val result = try {
                ack.await().also { closed = true }
            } catch (rejected: Rejected) {
                @Suppress("UNCHECKED_CAST")
                (rejected.result as Result.Error<E>)
            }
            onResult(result)
        }
    }

    private fun lock(): Boolean {
        if (isBusy) return false
        terminal = true
        busy(true)
        return true
    }
    private fun terminalOperation(block: suspend () -> Unit) = operation {
        try { block() } finally {
            terminal = false
            if (!closed) busy(false)
        }
    }
    private fun operation(block: suspend () -> Unit) = scope.launch {
        safeLocalCall { block() }.onFailure(failed)
    }
}
