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

    @Test fun initialChoiceFailureDoesNotPreventDraftOpeningAndHasIndependentRetry() = runTest(dispatcher) {
        routines.updates.value = null
        val vm = editor(); testScheduler.runCurrent()
        val original = vm.uiState.value.draft!!
        assertFalse(vm.uiState.value.isLoading); assertNotNull(vm.uiState.value.choicesError)
        routines.updates.value = listOf(Routine("recovered", "Recovered", exercises = emptyList()))
        vm.onIntent(PlanEditorContract.Intent.RetryChoices); testScheduler.runCurrent()
        assertEquals(routines.updates.value, vm.uiState.value.choices)
        assertNull(vm.uiState.value.choicesError); assertEquals(original, vm.uiState.value.draft)
    }
    @Test fun liveChoicesAndRetryPreserveRawDraftAndLease() = runTest(dispatcher) {
        val vm = editor(); testScheduler.runCurrent()
        val original = vm.uiState.value.draft!!
        val archivedReference = vm.uiState.value.choices.single().copy(name = "Renamed original", archived = true)
        val raw = original.copy(name = "Retained", slots = original.slots.map { it.copy(label = "") })
        vm.onIntent(PlanEditorContract.Intent.Edit(raw)); testScheduler.runCurrent()
        val added = Routine("new", "New routine", exercises = emptyList())
        routines.updates.value = listOf(archivedReference, added); testScheduler.runCurrent()
        assertEquals(listOf(archivedReference, added), vm.uiState.value.choices)
        assertEquals(raw, vm.uiState.value.draft)
        routines.updates.value = null; testScheduler.runCurrent()
        assertNotNull(vm.uiState.value.choicesError)
        assertEquals(raw, vm.uiState.value.draft)
        routines.updates.value = listOf(archivedReference, added.copy(name = "Renamed", archived = true))
        vm.onIntent(PlanEditorContract.Intent.Retry); testScheduler.runCurrent()
        assertEquals(routines.updates.value, vm.uiState.value.choices)
        assertEquals(raw, vm.uiState.value.draft)
        assertNull(vm.uiState.value.choicesError)
        val other = editor(); testScheduler.runCurrent()
        assertNull(other.uiState.value.draft, "Choice refresh must retain exclusive lease")
    }
    @Test fun choiceFailureCannotReleaseTerminalSave() = runTest(dispatcher) {
        val vm = editor(); testScheduler.runCurrent()
        repository.gate = CompletableDeferred()
        vm.onIntent(PlanEditorContract.Intent.Save); testScheduler.runCurrent()
        routines.updates.value = null; testScheduler.runCurrent()
        assertTrue(vm.uiState.value.isSaving)
        assertNotNull(vm.uiState.value.choicesError)
        routines.updates.value = emptyList()
        vm.onIntent(PlanEditorContract.Intent.RetryChoices); testScheduler.runCurrent()
        assertNull(vm.uiState.value.choicesError); assertTrue(vm.uiState.value.isSaving)
        repository.gate!!.complete(Unit); testScheduler.runCurrent()
        assertEquals(1, repository.saved.size); assertNull(store.payload)
    }
    @Test fun olderAutosaveFailureCannotUnlockSave() = runTest(dispatcher) {
        val vm = editor(); testScheduler.runCurrent()
        val accepted = vm.uiState.value.draft!!.copy(name = "Accepted before terminal")
        store.putGate = CompletableDeferred(); store.failNextPut = true
        vm.onIntent(PlanEditorContract.Intent.Edit(accepted)); testScheduler.runCurrent()
        repository.gate = CompletableDeferred()
        vm.onIntent(PlanEditorContract.Intent.Save); testScheduler.runCurrent()
        store.putGate!!.complete(Unit); testScheduler.runCurrent()
        assertTrue(vm.uiState.value.isSaving, "Older autosave failure must not release terminal ownership")
        vm.onIntent(PlanEditorContract.Intent.Edit(accepted.copy(name = "Must not be accepted")))
        assertEquals(accepted, vm.uiState.value.draft)
        repository.gate!!.complete(Unit); testScheduler.runCurrent()
        assertEquals(accepted.name, repository.saved.single().name); assertNull(store.payload)
    }
    @Test fun olderAutosaveFailureCannotUnlockDiscard() = runTest(dispatcher) {
        val vm = editor(); testScheduler.runCurrent()
        val accepted = vm.uiState.value.draft!!.copy(name = "Accepted before terminal")
        store.putGate = CompletableDeferred(); store.failNextPut = true
        vm.onIntent(PlanEditorContract.Intent.Edit(accepted)); testScheduler.runCurrent()
        store.deleteGate = CompletableDeferred()
        vm.onIntent(PlanEditorContract.Intent.Discard); testScheduler.runCurrent()
        store.putGate!!.complete(Unit); testScheduler.runCurrent()
        assertTrue(vm.uiState.value.isSaving, "Older autosave failure must not release terminal ownership")
        vm.onIntent(PlanEditorContract.Intent.Edit(accepted.copy(name = "Must not be accepted")))
        assertEquals(accepted, vm.uiState.value.draft)
        store.deleteGate!!.complete(Unit); testScheduler.runCurrent()
        assertNull(store.payload)
    }
    @Test fun olderAutosaveFailureCannotUnlockReload() = runTest(dispatcher) {
        val vm = editor(); testScheduler.runCurrent()
        val accepted = vm.uiState.value.draft!!.copy(name = "Accepted before terminal")
        store.putGate = CompletableDeferred(); store.failNextPut = true
        vm.onIntent(PlanEditorContract.Intent.Edit(accepted)); testScheduler.runCurrent()
        repository.readGate = CompletableDeferred()
        vm.onIntent(PlanEditorContract.Intent.Reload); testScheduler.runCurrent()
        store.putGate!!.complete(Unit); testScheduler.runCurrent()
        assertTrue(vm.uiState.value.isSaving, "Older autosave failure must not release terminal ownership")
        vm.onIntent(PlanEditorContract.Intent.Edit(accepted.copy(name = "Must not be accepted")))
        assertEquals(accepted, vm.uiState.value.draft)
        repository.readGate!!.complete(Unit); testScheduler.runCurrent()
        assertEquals(repository.item.name, vm.uiState.value.draft!!.name); assertFalse(vm.uiState.value.isSaving)
    }
    @Test fun secondRetainedEditorCannotStealDraftAndRetriesAfterOwnerCloses() = runTest(dispatcher) {
        val first = editor(); testScheduler.runCurrent()
        val accepted = first.uiState.value.draft!!.copy(name = "First accepted", notes = "Unpublished")
        first.onIntent(PlanEditorContract.Intent.Edit(accepted))
        val second = editor(); testScheduler.runCurrent()
        assertNull(second.uiState.value.draft, "A second live editor must not acquire the same draft")
        assertNotNull(second.uiState.value.error)
        second.onIntent(PlanEditorContract.Intent.Discard); testScheduler.runCurrent()
        assertNotNull(store.payload, "Blocked entry cannot delete owner's draft")
        first.onIntent(PlanEditorContract.Intent.Edit(accepted.copy(notes = "Latest accepted")))
        first.viewModelScope.cancel()
        second.onIntent(PlanEditorContract.Intent.Retry); testScheduler.runCurrent()
        assertEquals("Latest accepted", second.uiState.value.draft!!.notes)
        assertEquals(accepted.expectedRevision, second.uiState.value.draft!!.expectedRevision)
        second.onIntent(PlanEditorContract.Intent.Save); testScheduler.runCurrent()
        assertEquals("Latest accepted", repository.saved.single().notes); assertNull(store.payload)
    }
    @Test fun secondRetainedEditorCanDiscardAfterOwnerCloses() = runTest(dispatcher) {
        val first = editor(); testScheduler.runCurrent()
        val second = editor(); testScheduler.runCurrent()
        assertNull(second.uiState.value.draft)
        first.viewModelScope.cancel()
        second.onIntent(PlanEditorContract.Intent.Retry); testScheduler.runCurrent()
        val effects = mutableListOf<PlanEditorContract.Effect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { second.effect.collect { effects.add(it) } }
        second.onIntent(PlanEditorContract.Intent.Discard); testScheduler.runCurrent()
        assertNull(store.payload); assertIs<PlanEditorContract.Effect.Back>(effects.single())
    }
    @Test fun blockedEntryCanRetryAfterOwnerPublishesWithoutWaitingForClear() = runTest(dispatcher) {
        val first = editor(); testScheduler.runCurrent()
        val second = editor(); testScheduler.runCurrent()
        assertNull(second.uiState.value.draft)
        first.onIntent(PlanEditorContract.Intent.Save); testScheduler.runCurrent()
        assertEquals(1, repository.saved.size)
        second.onIntent(PlanEditorContract.Intent.Retry); testScheduler.runCurrent()
        assertNotNull(second.uiState.value.draft); assertNull(second.uiState.value.error)
        second.onIntent(PlanEditorContract.Intent.Discard); testScheduler.runCurrent()
        assertNull(store.payload)
    }
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
    var putGate: CompletableDeferred<Unit>? = null; var deleteGate: CompletableDeferred<Unit>? = null; var failNextPut = false
    override suspend fun get(kind: String, id: String) = payload
    override suspend fun put(kind: String, id: String, payload: String) {
        putGate?.await()
        if (failNextPut) { failNextPut = false; error("Controlled older put failure") }
        this.payload = payload
    }
    override suspend fun delete(kind: String, id: String) { deleteGate?.await(); check(!failDelete); payload = null }
}
private class Repository : TrainingPlanRepository {
    var item = TrainingPlan("existing", "Original", revision = 1, slots = listOf(PlanSlot("slot", "Den A", "routine")))
    var conflict = false; var failRead = false; var gate: CompletableDeferred<Unit>? = null
    var readGate: CompletableDeferred<Unit>? = null
    val saved = mutableListOf<TrainingPlan>(); val expected = mutableListOf<Long?>()
    override fun observeAll(includeArchived: Boolean): Flow<List<TrainingPlan>> = flow { check(!failRead); emit(listOf(item)) }
    override fun observeById(id: String) = flowOf(item)
    override suspend fun getById(id: String): TrainingPlan { readGate?.await(); check(!failRead); return item }
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
    val updates = MutableStateFlow<List<Routine>?>(listOf(routine))
    override fun observeAll(includeArchived: Boolean) = updates.map { checkNotNull(it) { "Controlled choices failure" } }
    override fun observeById(id: String) = flowOf(routine)
    override suspend fun getById(id: String) = routine
    override suspend fun save(routine: Routine, expectedRevision: Long?): Result<Routine,RoutineError> = error("unused")
    override suspend fun duplicate(id: String): Result<Routine,RoutineError> = error("unused")
    override suspend fun archive(id: String, expectedRevision: Long): Result<Unit,RoutineError> = error("unused")
}
