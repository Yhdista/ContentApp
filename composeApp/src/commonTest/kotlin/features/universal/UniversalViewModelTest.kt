package features.universal

import core.common.AppClock
import core.common.ApplicationScope
import core.database.ContentDao
import core.database.ContentEntity
import core.database.MutationDao
import core.database.SyncMutationEntity
import core.model.ContentItem
import core.model.ItemSyncStatus
import core.repository.ContentRepository
import core.sync.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UniversalViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val appJob = SupervisorJob()

    private lateinit var fakeRepository: FakeContentRepository
    private lateinit var fakeMutationDao: FakeMutationDao
    private lateinit var fakeContentDao: FakeContentDao
    private lateinit var syncManager: SyncManager
    private lateinit var viewModel: UniversalViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeContentRepository()
        fakeMutationDao = FakeMutationDao()
        fakeContentDao = FakeContentDao()

        val appScope = ApplicationScope(testDispatcher + appJob)
        syncManager = SyncManager(fakeMutationDao, fakeContentDao, appScope)

        val fakeClock = object : AppClock {
            override fun nowMillis(): Long = 1729860000000L
        }

        viewModel = UniversalViewModel(
            repository = fakeRepository,
            syncManager = syncManager,
            applicationScope = appScope,
            clock = fakeClock
        )
    }

    @AfterTest
    fun tearDown() {
        appJob.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() = runTest(testDispatcher) {
        testScheduler.runCurrent()
        val state = viewModel.uiState.value
        assertEquals(5, state.counterValue)
        assertEquals("Workout", state.selectedCategory)
        assertEquals(0, state.timerSeconds)
        assertFalse(state.isTimerRunning)
    }

    @Test
    fun testCounterAndCategoryChange() = runTest(testDispatcher) {
        viewModel.onIntent(UniversalContract.Intent.SetCounter(12))
        viewModel.onIntent(UniversalContract.Intent.SelectCategory("Běh"))
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals(12, state.counterValue)
        assertEquals("Běh", state.selectedCategory)
    }

    @Test
    fun testTimerStartPauseReset() = runTest(testDispatcher) {
        viewModel.onIntent(UniversalContract.Intent.StartTimer)
        testScheduler.runCurrent()
        assertTrue(viewModel.uiState.value.isTimerRunning)

        testScheduler.advanceTimeBy(3500)
        assertEquals(3, viewModel.uiState.value.timerSeconds)

        viewModel.onIntent(UniversalContract.Intent.PauseTimer)
        testScheduler.runCurrent()
        assertFalse(viewModel.uiState.value.isTimerRunning)

        testScheduler.advanceTimeBy(2000)
        assertEquals(3, viewModel.uiState.value.timerSeconds)

        viewModel.onIntent(UniversalContract.Intent.ResetTimer)
        testScheduler.runCurrent()
        assertEquals(0, viewModel.uiState.value.timerSeconds)
    }

    @Test
    fun testAddQuickRecordSurvivesInApplicationScope() = runTest(testDispatcher) {
        viewModel.onIntent(UniversalContract.Intent.SetCounter(42))
        viewModel.onIntent(UniversalContract.Intent.SelectCategory("Trénink"))
        viewModel.onIntent(UniversalContract.Intent.AddQuickRecord)

        testScheduler.runCurrent()

        assertEquals(1, fakeRepository.addedRecords.size)
        val added = fakeRepository.addedRecords.first()
        assertEquals(42, added.value)
        assertEquals("Trénink", added.category)
    }

    // --- Fakes ---

    class FakeContentRepository : ContentRepository {
        private val _flow = MutableStateFlow<List<ContentItem>>(emptyList())
        val addedRecords = mutableListOf<ContentItem>()

        override fun getContentItems(): Flow<List<ContentItem>> = _flow

        override suspend fun addRecord(
            title: String,
            description: String,
            value: Int,
            category: String
        ): ContentItem {
            val item = ContentItem(
                id = addedRecords.size + 1,
                title = title,
                description = description,
                value = value,
                category = category,
                syncStatus = ItemSyncStatus.PENDING,
                updatedAt = 1729860000000L,
                clientUuid = "test-uuid"
            )
            addedRecords.add(item)
            _flow.value = addedRecords.toList()
            return item
        }

        override suspend fun refreshContent() {}
    }

    class FakeMutationDao : MutationDao {
        private val _mutations = MutableStateFlow<List<SyncMutationEntity>>(emptyList())

        override fun getPendingMutations(): Flow<List<SyncMutationEntity>> = _mutations

        override suspend fun insertMutation(mutation: SyncMutationEntity) {
            _mutations.value = _mutations.value + mutation
        }

        override suspend fun deleteMutation(id: String) {
            _mutations.value = _mutations.value.filter { it.id != id }
        }

        override suspend fun markError(id: String, error: String, retryCount: Int) {}
    }

    class FakeContentDao : ContentDao {
        private val _items = MutableStateFlow<List<ContentEntity>>(emptyList())

        override fun getAllContent(): Flow<List<ContentEntity>> = _items

        override suspend fun insertItem(item: ContentEntity): Long {
            val id = (_items.value.size + 1).toLong()
            val saved = item.copy(id = id.toInt())
            _items.value = _items.value + saved
            return id
        }

        override suspend fun insertContent(items: List<ContentEntity>) {
            _items.value = _items.value + items
        }

        override suspend fun updateSyncStatus(id: Int, status: String) {
            _items.value = _items.value.map {
                if (it.id == id) it.copy(syncStatus = status) else it
            }
        }

        override suspend fun clearAll() {
            _items.value = emptyList()
        }
    }
}
