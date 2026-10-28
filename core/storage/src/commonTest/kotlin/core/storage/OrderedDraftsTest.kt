package core.storage

import core.common.ApplicationScope
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class OrderedDraftsTest {
    private class Memory : DraftStore {
        var payload: String? = null
        var readGate: CompletableDeferred<Unit>? = null
        var putGate: CompletableDeferred<Unit>? = null
        override suspend fun get(kind: String, id: String): String? { readGate?.await(); return payload }
        override suspend fun put(kind: String, id: String, payload: String) { putGate?.await(); this.payload = payload }
        override suspend fun delete(kind: String, id: String) { payload = null }
    }
    @Test fun liveLeaseRejectsAllReopensAndReleaseFollowsAcceptedWrites() = runTest {
        val scope = ApplicationScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        try {
            val store = Memory(); val queue = OrderedDrafts(store, scope); val owner = Job()
            val editor = queue.open("routine", "edit:one", owner).await()
            assertFailsWith<DraftAlreadyOpen> { queue.open("routine", "edit:one").await() }
            assertFailsWith<DraftAlreadyOpen> { queue.open("routine", "edit:one", Job()).await() }
            store.putGate = CompletableDeferred()
            val put = editor.put("latest raw -"); testScheduler.runCurrent()
            owner.cancel()
            val reopened = queue.open("routine", "edit:one", Job())
            testScheduler.runCurrent(); assertFalse(reopened.isCompleted)
            store.putGate!!.complete(Unit); put.await()
            assertEquals("latest raw -", reopened.await().payload)
            assertFailsWith<IllegalStateException> { editor.put("obsolete").await() }
        } finally { scope.cancel() }
    }
    @Test fun cancellingOwnerDuringAcquisitionReadDoesNotLeakLease() = runTest {
        val scope = ApplicationScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        try {
            val store = Memory(); val queue = OrderedDrafts(store, scope); val owner = Job()
            store.readGate = CompletableDeferred()
            val pending = queue.open("plan", "new", owner)
            testScheduler.runCurrent(); owner.cancel(); store.readGate!!.complete(Unit)
            assertFailsWith<CancellationException> { pending.await() }
            val recovered = queue.open("plan", "new", Job()).await()
            recovered.put("recovered").await(); assertEquals("recovered", store.payload)
        } finally { scope.cancel() }
    }
    @Test fun cancellingOwnerWhileAcquisitionQueuedDoesNotLeakLease() = runTest {
        val scope = ApplicationScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        try {
            val store = Memory(); val queue = OrderedDrafts(store, scope); val owner = Job()
            val blocker = queue.open("other", "id").await()
            store.putGate = CompletableDeferred(); blocker.put("queued"); testScheduler.runCurrent()
            val pending = queue.open("plan", "new", owner)
            owner.cancel(); store.putGate!!.complete(Unit)
            assertFailsWith<CancellationException> { pending.await() }
            queue.open("plan", "new", Job()).await().discard().await(); assertNull(store.payload)
        } finally { scope.cancel() }
    }
    @Test fun acceptedWritesSurviveCallerAndReopenSeesLatestRawText() = runTest {
        val scope = ApplicationScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        try {
            val store = Memory(); val queue = OrderedDrafts(store, scope)
            val first = queue.open("routine", "new").await()
            first.put("1."); first.put("-")
            val reopened = queue.open("routine", "new").await()
            assertEquals("-", reopened.payload)
            assertFailsWith<IllegalStateException> { first.put("obsolete").await() }
            assertEquals("-", store.payload)
        } finally { scope.cancel() }
    }
    @Test fun terminalWaitsForAcceptedWritesAndCannotResurrectPublishedDraft() = runTest {
        val scope = ApplicationScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        try {
            val store = Memory(); val queue = OrderedDrafts(store, scope)
            val draft = queue.open("routine", "new").await()
            draft.put("latest")
            val finish = draft.finish { assertEquals("latest", store.payload); store.delete("routine", "new"); "saved" }
            assertEquals("saved", finish.await())
            assertFailsWith<IllegalStateException> { draft.put("late").await() }
            assertNull(store.payload)
        } finally { scope.cancel() }
    }
    @Test fun failedTerminalAllowsRecoveryWithoutLosingDraft() = runTest {
        val scope = ApplicationScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        try {
            val store = Memory(); val queue = OrderedDrafts(store, scope)
            val draft = queue.open("routine", "new").await(); draft.put("raw")
            assertFailsWith<IllegalStateException> { draft.finish<Unit> { error("failure") }.await() }
            draft.put("repaired").await()
            assertEquals("repaired", store.payload)
        } finally { scope.cancel() }
    }
}
