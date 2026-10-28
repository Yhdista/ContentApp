package core.storage

import core.common.ApplicationScope
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class OrderedDraftsTest {
    private class Memory : DraftStore {
        var payload: String? = null
        override suspend fun get(kind: String, id: String) = payload
        override suspend fun put(kind: String, id: String, payload: String) { this.payload = payload }
        override suspend fun delete(kind: String, id: String) { payload = null }
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
