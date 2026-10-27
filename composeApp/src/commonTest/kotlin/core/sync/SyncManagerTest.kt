package core.sync

import core.common.ApplicationScope
import core.database.MutationDao
import core.database.SyncMutationEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SyncManagerTest {

    private lateinit var fakeMutationDao: FakeSyncMutationDao
    private lateinit var testScope: TestScope
    private lateinit var appScope: ApplicationScope
    private lateinit var syncManager: SyncManager

    @BeforeTest
    fun setup() {
        val testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        fakeMutationDao = FakeSyncMutationDao()
        appScope = ApplicationScope(testDispatcher)
        syncManager = SyncManager(fakeMutationDao, appScope)
    }

    @Test
    fun testInitialSyncManagerStatus() {
        assertFalse(syncManager.isSyncing.value)
        assertNotNull(syncManager.lastSyncLog.value)
    }

    @Test
    fun testStartSyncLoopProcessesPendingMutations() = testScope.runTest {
        val mutation = SyncMutationEntity(
            id = "mut-1",
            entityName = "workout_session",
            recordId = "session-123",
            operation = "INSERT",
            payloadJson = "{}",
            createdAt = 1000L
        )
        fakeMutationDao.insertMutation(mutation)

        syncManager.startSyncLoop()

        // Advance past delay and simulated upload
        testScope.testScheduler.advanceTimeBy(3500)

        // The mutation was processed and either succeeded (deleted) or retry recorded
        val remaining = fakeMutationDao.getPendingMutations().first()
        val processed = remaining.isEmpty() || remaining.first().retryCount > 0
        assertTrue(processed)
    }

    @Test
    fun testSyncLoopStopsAfterMaxRetriesOnPersistentFailure() = testScope.runTest {
        var uploadAttempts = 0
        val failingSyncManager = SyncManager(
            mutationDao = fakeMutationDao,
            applicationScope = appScope,
            maxRetries = 3,
            networkUploader = {
                uploadAttempts++
                false
            }
        )

        val mutation = SyncMutationEntity(
            id = "mut-fail-1",
            entityName = "workout_session",
            recordId = "session-fail",
            operation = "INSERT",
            payloadJson = "{}",
            createdAt = 2000L
        )
        fakeMutationDao.insertMutation(mutation)

        failingSyncManager.startSyncLoop()

        // Advance plenty of virtual time (20 seconds)
        testScope.testScheduler.advanceTimeBy(20_000)

        // Must stop after exactly 3 attempts and not loop infinitely!
        assertEquals(3, uploadAttempts)
        assertFalse(failingSyncManager.isSyncing.value)
        val deadLettered = fakeMutationDao.getAllMutationsSnapshot().first { it.id == "mut-fail-1" }
        assertEquals(3, deadLettered.retryCount)
    }

    @Test
    fun testStartSyncLoopIsIdempotentWhenCalledMultipleTimes() = testScope.runTest {
        var uploadAttempts = 0
        val deterministicSyncManager = SyncManager(
            mutationDao = fakeMutationDao,
            applicationScope = appScope,
            maxRetries = 3,
            networkUploader = {
                uploadAttempts++
                true
            }
        )

        val mutation = SyncMutationEntity(
            id = "mut-idem-1",
            entityName = "workout_set",
            recordId = "set-1",
            operation = "INSERT",
            payloadJson = "{}",
            createdAt = 3000L
        )
        fakeMutationDao.insertMutation(mutation)

        // Call startSyncLoop 3 times concurrently
        deterministicSyncManager.startSyncLoop()
        deterministicSyncManager.startSyncLoop()
        deterministicSyncManager.startSyncLoop()

        testScope.testScheduler.advanceTimeBy(2000)

        assertEquals(1, uploadAttempts)
        assertTrue(fakeMutationDao.getAllMutationsSnapshot().isEmpty())
    }

    private class FakeSyncMutationDao : MutationDao {
        private val list = mutableListOf<SyncMutationEntity>()
        private val flow = MutableStateFlow<List<SyncMutationEntity>>(emptyList())

        override fun getPendingMutations(): Flow<List<SyncMutationEntity>> = flow.asStateFlow()

        fun getAllMutationsSnapshot(): List<SyncMutationEntity> = list.toList()

        override suspend fun insertMutation(mutation: SyncMutationEntity) {
            list.removeAll { it.id == mutation.id }
            list.add(mutation)
            flow.value = list.toList()
        }

        override suspend fun deleteMutation(id: String) {
            list.removeAll { it.id == id }
            flow.value = list.toList()
        }

        override suspend fun markError(id: String, error: String, retryCount: Int) {
            val idx = list.indexOfFirst { it.id == id }
            if (idx >= 0) {
                list[idx] = list[idx].copy(lastError = error, retryCount = retryCount)
                flow.value = list.toList()
            }
        }
    }
}
