package features.trainingplans.presentation

import androidx.lifecycle.viewModelScope
import core.common.ApplicationScope
import core.result.Result
import core.storage.*
import features.trainingplans.api.*
import features.routines.api.*

import features.workout.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.Json
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class PlanPresentationTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var scope: ApplicationScope
    private lateinit var store: MemoryDrafts
    private lateinit var queue: OrderedDrafts
    private lateinit var repository: Repository
    private val routines = Routines()
    private val writes = object : CommandCoordinator { override suspend fun <T> write(block: suspend () -> T) = block() }
    private val models = mutableListOf<androidx.lifecycle.ViewModel>()
    @BeforeTest fun before() { Dispatchers.setMain(dispatcher); scope = ApplicationScope(SupervisorJob() + dispatcher); store = MemoryDrafts(); queue = OrderedDrafts(store,scope); repository = Repository() }
    @AfterTest fun after() { models.forEach { it.viewModelScope.cancel() }; scope.cancel(); Dispatchers.resetMain() }
    private fun editor() = PlanEditorViewModel("edit:existing", repository, queue, store, writes, routines).also { models.add(it) }
    private fun library() = PlanLibraryViewModel("existing", repository, routines, StartPlanWorkout { Result.Success(StartOutcome.ResumedExisting("active")) }, StartRoutineWorkout { Result.Success(StartOutcome.Created("solo")) }).also { models.add(it) }

    @Test fun rapidFieldMutationsPreserveBothAcceptedChanges() = runTest(dispatcher) {
        val vm = editor(); testScheduler.runCurrent()
        vm.onIntent(PlanEditorContract.Intent.Mutate { it.copy(name = "Fast name") })
        vm.onIntent(PlanEditorContract.Intent.Mutate { it.copy(notes = "Fast notes") })
        vm.viewModelScope.cancel()
        val restored = editor(); testScheduler.runCurrent()
        assertEquals("Fast name", restored.uiState.value.draft!!.name)
        assertEquals("Fast notes", restored.uiState.value.draft!!.notes)
    }
    @Test fun malformedDraftIsExplicitAndCanBeDiscarded() = runTest(dispatcher) {
        store.payload = "not json"
        val vm = editor(); testScheduler.runCurrent()
        assertNull(vm.uiState.value.draft); assertNotNull(vm.uiState.value.error)
        assertTrue(vm.uiState.value.canDiscard)
        vm.onIntent(PlanEditorContract.Intent.Discard); testScheduler.runCurrent()
        assertNull(store.payload)
    }
    @Test fun invalidRawDraftSurvivesImmediateBackAndReopen() = runTest(dispatcher) {
        val vm = editor(); testScheduler.runCurrent()
        val draft = vm.uiState.value.draft!!
        val invalid = draft.copy(slots = draft.slots.map { it.copy(label = "") })
        vm.onIntent(PlanEditorContract.Intent.Edit(invalid))
        vm.onIntent(PlanEditorContract.Intent.Back)
        vm.viewModelScope.cancel()
        val next = editor(); testScheduler.runCurrent()
        assertEquals(invalid, next.uiState.value.draft)
        next.onIntent(PlanEditorContract.Intent.Save); testScheduler.runCurrent()
        assertNotNull(next.uiState.value.error); assertEquals(0, repository.saved.size)
    }
    @Test fun saveCopyUsesUnsavedContentAndFreshOwnedIdentities() = runTest(dispatcher) {
        val vm = editor(); testScheduler.runCurrent()
        vm.onIntent(PlanEditorContract.Intent.Edit(vm.uiState.value.draft!!.copy(name = "My unsaved version")))
        repository.conflict = true
        vm.onIntent(PlanEditorContract.Intent.Save); testScheduler.runCurrent()
        assertTrue(vm.uiState.value.conflict); assertNotNull(store.payload)
        repository.conflict = false
        vm.onIntent(PlanEditorContract.Intent.SaveCopy); testScheduler.runCurrent()
        val copy = repository.saved.single()
        assertEquals("My unsaved version", copy.name); assertNotEquals("existing",copy.id)
        assertTrue(copy.slots.first().id != "slot"); assertNull(repository.expected.single()); assertNull(store.payload)
    }
    @Test fun effectWaitsForCommitAndPublishFreezesEdits() = runTest(dispatcher) {
        val vm = editor(); testScheduler.runCurrent()
        val effects = mutableListOf<PlanEditorContract.Effect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.effect.collect { effects.add(it) } }
        repository.gate = CompletableDeferred()
        val current = vm.uiState.value.draft!!
        vm.onIntent(PlanEditorContract.Intent.Save); testScheduler.runCurrent()
        assertTrue(vm.uiState.value.isSaving); assertTrue(effects.isEmpty()); assertNotNull(store.payload)
        vm.onIntent(PlanEditorContract.Intent.Edit(current.copy(name = "Too late")))
        assertEquals(current,vm.uiState.value.draft)
        repository.gate!!.complete(Unit); testScheduler.runCurrent()
        assertIs<PlanEditorContract.Effect.Saved>(effects.single()); assertNull(store.payload)
    }
    @Test fun failedReloadAndDiscardRetainRawDraftAndAllowRecovery() = runTest(dispatcher) {
        val vm = editor(); testScheduler.runCurrent()
        val edited = vm.uiState.value.draft!!.copy(name = "Local")
        vm.onIntent(PlanEditorContract.Intent.Edit(edited)); testScheduler.runCurrent()
        repository.failRead = true
        vm.onIntent(PlanEditorContract.Intent.Reload); testScheduler.runCurrent()
        assertEquals(edited,vm.uiState.value.draft); assertNotNull(vm.uiState.value.error)
        store.failDelete = true
        vm.onIntent(PlanEditorContract.Intent.Back); vm.onIntent(PlanEditorContract.Intent.Discard); testScheduler.runCurrent()
        assertNotNull(store.payload); assertTrue(vm.uiState.value.confirmExit); assertFalse(vm.uiState.value.isSaving)
        store.failDelete = false
        vm.onIntent(PlanEditorContract.Intent.Discard); testScheduler.runCurrent(); assertNull(store.payload)
    }
    @Test fun libraryReadFailureIsNotAnEmptySuccessAndResumeIsVisible() = runTest(dispatcher) {
        repository.failRead = true
        val failed = library(); testScheduler.runCurrent()
        assertNotNull(failed.uiState.value.error); assertFalse(failed.uiState.value.isLoading)
        repository.failRead = false
        failed.onIntent(PlanLibraryContract.Intent.Retry); testScheduler.runCurrent()
        assertNull(failed.uiState.value.error)
        failed.onIntent(PlanLibraryContract.Intent.Start); testScheduler.runCurrent()
        assertTrue(failed.uiState.value.resumed)
        failed.onIntent(PlanLibraryContract.Intent.Archive)
        assertTrue(failed.uiState.value.archiveConfirmation)
    }

    @Test fun reviewRepairAndActiveSelectionAreExplicitCommands() = runTest(dispatcher) {
        val vm = library()
        testScheduler.runCurrent()
        assertTrue(vm.uiState.value.progress!!.needsReview)
        vm.onIntent(PlanLibraryContract.Intent.ConfirmNext("slot"))
        testScheduler.runCurrent()
        assertFalse(vm.uiState.value.progress!!.needsReview)
        assertEquals(2, vm.uiState.value.progress!!.progressRevision)
        vm.onIntent(PlanLibraryContract.Intent.SelectActive)
        testScheduler.runCurrent()
        assertEquals("existing", vm.uiState.value.activeId)
        vm.viewModelScope.cancel()
    }

}
private class MemoryDrafts : DraftStore {
    var payload: String? = null; var failDelete = false
    override suspend fun get(kind: String, id: String) = payload
    override suspend fun put(kind: String, id: String, payload: String) { this.payload = payload }
    override suspend fun delete(kind: String, id: String) { check(!failDelete); payload = null }
}
private class Repository : TrainingPlanRepository {
    var item = TrainingPlan("existing", "Original", revision = 1, slots = listOf(PlanSlot("slot", "Den A", "routine")))
    var conflict = false; var failRead = false; var gate: CompletableDeferred<Unit>? = null
    val saved = mutableListOf<TrainingPlan>(); val expected = mutableListOf<Long?>()
    override fun observeAll(includeArchived: Boolean): Flow<List<TrainingPlan>> = flow { check(!failRead); emit(listOf(item)) }
    override fun observeById(id: String) = flowOf(item)
    override suspend fun getById(id: String): TrainingPlan { check(!failRead); return item }
    override suspend fun save(plan: TrainingPlan, expectedRevision: Long?): Result<TrainingPlan,PlanError> {
        if(conflict) return Result.Error(PlanError.REVISION_CONFLICT)
        gate?.await(); saved.add(plan); expected.add(expectedRevision)
        return Result.Success(plan)
    }
    override suspend fun duplicate(id: String): Result<TrainingPlan,PlanError> = Result.Success(item.copy(id = "copy"))
    override suspend fun archive(id: String, expectedRevision: Long): Result<Unit,PlanError> = Result.Success(Unit)

    val progress = MutableStateFlow(PlanProgress("existing", "slot", 1, true))
    val active = MutableStateFlow<TrainingPlan?>(null)
    override fun observeProgress(planId: String) = progress
    override suspend fun getProgress(planId: String) = progress.value
    override fun observeActive() = active
    override suspend fun selectActive(planId: String?): Result<Unit,PlanError> { active.value = if(planId == null) null else item; return Result.Success(Unit) }
    override suspend fun skipNext(planId: String, expectedProgressRevision: Long): Result<PlanProgress,PlanError> = Result.Error(PlanError.REVIEW_REQUIRED)
    override suspend fun confirmNext(planId: String, slotId: String, expectedProgressRevision: Long): Result<PlanProgress,PlanError> {
        assertEquals(progress.value.progressRevision, expectedProgressRevision)
        progress.value = progress.value.copy(nextSlotId = slotId, needsReview = false, progressRevision = expectedProgressRevision + 1)
        return Result.Success(progress.value)
    }
    override suspend fun prepareStart(planId: String): Result<PlanStartContext,PlanError> = error("unused")
    override suspend fun recordCompleted(source: PlanWorkoutSource): Result<ProgressOutcome,PlanError> = error("unused")

}

private class Routines : RoutineRepository {
    private val routine = Routine("routine", "Bench day", exercises = emptyList())
    override fun observeAll(includeArchived: Boolean) = flowOf(listOf(routine))
    override fun observeById(id: String) = flowOf(routine)
    override suspend fun getById(id: String) = routine
    override suspend fun save(routine: Routine, expectedRevision: Long?): Result<Routine,RoutineError> = error("unused")
    override suspend fun duplicate(id: String): Result<Routine,RoutineError> = error("unused")
    override suspend fun archive(id: String, expectedRevision: Long): Result<Unit,RoutineError> = error("unused")
}
