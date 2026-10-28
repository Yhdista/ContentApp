package core.viewmodel

import core.common.ApplicationScope
import core.result.DataError
import core.result.Result
import core.storage.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class DurableEditorControllerTest {
    private class Store : DraftStore {
        var payload: String? = null
        var failPut = false
        var failDelete = false
        override suspend fun get(kind: String, id: String) = payload
        override suspend fun put(kind: String, id: String, payload: String) {
            if (failPut) error("Autosave unavailable")
            this.payload = payload
        }
        override suspend fun delete(kind: String, id: String) {
            if (failDelete) error("Delete unavailable")
            payload = null
        }
    }
    private val writes = object : CommandCoordinator {
        override suspend fun <T> write(block: suspend () -> T): T = block()
    }
    private val codec = object : DraftCodec<String> {
        override fun encode(draft: String) = "1|$draft"
        override fun decode(payload: String): String {
            require(payload.startsWith("1|"))
            return payload.removePrefix("1|")
        }
    }
    private class Bound(val controller: DurableEditorController<String>, val scope: CoroutineScope) {
        var draft: String? = null
        var busy = false
        var failures = 0
        init {
            controller.bind(scope, { failures++ }, { busy = it })
            controller.open({ draft = it }, onAlreadyOpen = { error("Unexpected duplicate lease") })
        }
    }
    private fun TestScope.bound(queue: OrderedDrafts, initial: String = "initial") = Bound(
        DurableEditorController(queue, writes, "editor", "new", codec) { initial },
        CoroutineScope(backgroundScope.coroutineContext + Job(backgroundScope.coroutineContext.job))
    )

    @Test fun publishFailurePreservesDraftAndLease() = runTest {
        val store = Store()
        val queue = OrderedDrafts(store, ApplicationScope(backgroundScope.coroutineContext))
        val editor = bound(queue)
        runCurrent()
        editor.controller.edit("1.|-")
        runCurrent()
        store.failDelete = true
        var published = false
        editor.controller.publish({ Result.Success("aggregate") }) { published = true }
        runCurrent()
        assertFalse(published)
        assertFalse(editor.busy)
        assertEquals(1, editor.failures)
        assertEquals("1|1.|-", store.payload)
        val duplicate = queue.open("editor", "new", Job())
        runCurrent()
        assertFailsWith<DraftAlreadyOpen> { duplicate.await() }
        store.failDelete = false
        editor.controller.edit("recovered")
        runCurrent()
        assertEquals("1|recovered", store.payload)
    }

    @Test fun publishClosesLeaseWithoutDraftResurrection() = runTest {
        val store = Store()
        val queue = OrderedDrafts(store, ApplicationScope(backgroundScope.coroutineContext))
        val editor = bound(queue)
        runCurrent()
        val gate = CompletableDeferred<Unit>()
        var committed = false
        editor.controller.publish({ gate.await(); committed = true; Result.Success("saved") }) {}
        editor.controller.edit("late edit")
        editor.scope.cancel() // The accepted publication still belongs to the application queue.
        gate.complete(Unit)
        runCurrent()
        assertTrue(committed)
        assertNull(store.payload)
        val next = queue.open("editor", "new", Job())
        runCurrent()
        assertNull(next.await().payload)
        editor.controller.edit("late callback")
        runCurrent()
        assertNull(store.payload)
    }

    @Test fun rawDraftSurvivesReopenAndSaveCopyConflict() = runTest {
        val store = Store()
        val queue = OrderedDrafts(store, ApplicationScope(backgroundScope.coroutineContext))
        val first = bound(queue)
        runCurrent()
        first.controller.edit("-|1.")
        first.scope.cancel()
        runCurrent()
        val reopened = bound(queue, "must not replace raw")
        runCurrent()
        assertEquals("-|1.", reopened.draft)
        var conflict: Result<String, DataError.Local>? = null
        reopened.controller.publish({ Result.Error(DataError.Local.CORRUPTED) }) { conflict = it }
        runCurrent()
        assertIs<Result.Error<DataError.Local>>(conflict)
        assertEquals("1|-|1.", store.payload)
        assertFalse(reopened.busy)
        var copied: String? = null
        reopened.controller.publish({ Result.Success(reopened.draft!!) }) { copied = (it as Result.Success).data }
        runCurrent()
        assertEquals("-|1.", copied)
        assertNull(store.payload)
    }

    @Test fun backgroundFailureCannotUnlockTerminalCommand() = runTest {
        val store = Store()
        val queue = OrderedDrafts(store, ApplicationScope(backgroundScope.coroutineContext))
        val editor = bound(queue)
        runCurrent()
        store.failPut = true
        editor.controller.edit("accepted failing edit")
        val gate = CompletableDeferred<Unit>()
        var saves = 0
        editor.controller.publish({ saves++; gate.await(); Result.Success("saved") }) {}
        runCurrent()
        assertEquals(1, editor.failures)
        assertTrue(editor.busy)
        assertTrue(editor.controller.isBusy)
        editor.controller.publish({ saves++; Result.Success("duplicate") }) {}
        runCurrent()
        assertEquals(1, saves)
        gate.complete(Unit)
        runCurrent()
        assertNull(store.payload)
    }
}
