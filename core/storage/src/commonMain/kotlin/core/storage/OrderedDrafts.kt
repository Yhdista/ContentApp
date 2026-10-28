package core.storage

import core.common.ApplicationScope
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

/** One application-owned FIFO. Acceptance is synchronous; awaiting never owns the write's lifetime.
 * All reads, edits and terminal commands go through this queue, outside the DB transaction worker.
 */
class DraftAlreadyOpen : IllegalStateException("Draft already has a live editor")

class OrderedDrafts(private val store: DraftStore, scope: ApplicationScope) {
    private val commands = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private val generations = mutableMapOf<Pair<String, String>, Any>()
    private val leases = mutableMapOf<Pair<String, String>, Any>()
    private val worker = scope.launch { for (command in commands) command() }
        .also { job -> job.invokeOnCompletion { commands.close(it) } }

    private fun <T> enqueue(block: suspend () -> T): Deferred<T> {
        val result = CompletableDeferred<T>()
        val completion = worker.invokeOnCompletion { result.completeExceptionally(it ?: CancellationException("Draft queue stopped")) }
        result.invokeOnCompletion { completion.dispose() }
        if (commands.trySend {
            try { result.complete(block()) }
            catch (e: Exception) { result.completeExceptionally(e); currentCoroutineContext().ensureActive() }
        }.isFailure) result.completeExceptionally(CancellationException("Draft queue stopped"))
        return result
    }

    /** An editor lease lasts until its owner completes, or a terminal command succeeds.
     * Acquiring/releasing is ordered with accepted writes; cancellation never deletes the draft.
     * Non-editor clients cannot supersede a live lease either.
     */
    fun open(kind: String, id: String, owner: Job? = null): Deferred<Session> = enqueue {
        val key = kind to id
        owner?.ensureActive()
        if (key in leases) throw DraftAlreadyOpen()
        val payload = store.get(kind, id)
        owner?.ensureActive() // Cancellation during a suspended read must not install an orphan lease.
        val token = Any()
        generations[key] = token
        if (owner != null) {
            leases[key] = token
            owner.invokeOnCompletion {
                enqueue {
                    if (generations[key] === token) generations.remove(key)
                    if (leases[key] === token) leases.remove(key)
                }
            }
        }
        Session(key, token, payload)
    }

    inner class Session internal constructor(private val key: Pair<String, String>, private val token: Any, val payload: String?) {
        private fun checkCurrent() = check(generations[key] === token) { "Draft session is closed" }
        fun put(payload: String): Deferred<Unit> = enqueue {
            checkCurrent()
            store.put(key.first, key.second, payload)
        }
        /** Caller freezes editing before acceptance. Success invalidates any late callbacks. */
        fun <T> finish(block: suspend () -> T): Deferred<T> = enqueue {
            checkCurrent()
            block().also { generations.remove(key); leases.remove(key) }
        }
        fun discard(): Deferred<Unit> = finish { store.delete(key.first, key.second) }
        fun barrier(): Deferred<Unit> = enqueue { checkCurrent() }
    }
}
