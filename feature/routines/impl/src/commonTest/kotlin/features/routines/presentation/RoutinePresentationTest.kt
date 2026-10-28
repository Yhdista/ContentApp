package features.routines.presentation

import androidx.lifecycle.viewModelScope
import core.common.ApplicationScope
import core.result.Result
import core.storage.*
import features.routines.api.*
import features.exercises.api.*

import features.workout.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.Json
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class RoutinePresentationTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var scope: ApplicationScope
    private lateinit var store: MemoryDrafts
    private lateinit var queue: OrderedDrafts
    private lateinit var repository: Repository
    private val catalogue = Catalogue()
    private val writes = object : CommandCoordinator { override suspend fun <T> write(block: suspend () -> T) = block() }
    private val models = mutableListOf<androidx.lifecycle.ViewModel>()
    @BeforeTest fun before() { Dispatchers.setMain(dispatcher); scope = ApplicationScope(SupervisorJob() + dispatcher); store = MemoryDrafts(); queue = OrderedDrafts(store,scope); repository = Repository() }
    @AfterTest fun after() { models.forEach { it.viewModelScope.cancel() }; scope.cancel(); Dispatchers.resetMain() }
    private fun editor() = RoutineEditorViewModel("edit:existing", repository, queue, store, writes, catalogue, RoutineUsage { flowOf(2) }).also { models.add(it) }
    private fun library() = RoutineLibraryViewModel("existing", repository, catalogue, RoutineUsage { flowOf(2) }, StartRoutineWorkout { Result.Success(StartOutcome.ResumedExisting("active")) }).also { models.add(it) }

    @Test fun rapidFieldMutationsPreserveBothAcceptedChanges() = runTest(dispatcher) {
        val vm = editor(); testScheduler.runCurrent()
        vm.onIntent(RoutineEditorContract.Intent.Mutate { it.copy(name = "Fast name") })
        vm.onIntent(RoutineEditorContract.Intent.Mutate { it.copy(notes = "Fast notes") })
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
        vm.onIntent(RoutineEditorContract.Intent.Discard); testScheduler.runCurrent()
        assertNull(store.payload)
    }
    @Test fun invalidRawDraftSurvivesImmediateBackAndReopen() = runTest(dispatcher) {
        val vm = editor(); testScheduler.runCurrent()
        val draft = vm.uiState.value.draft!!
        val invalid = draft.copy(exercises = draft.exercises.map { it.copy(sets = it.sets.map { it.copy(weight = "-") }) })
        vm.onIntent(RoutineEditorContract.Intent.Edit(invalid))
        vm.onIntent(RoutineEditorContract.Intent.Back)
        vm.viewModelScope.cancel()
        val next = editor(); testScheduler.runCurrent()
        assertEquals(invalid, next.uiState.value.draft)
        next.onIntent(RoutineEditorContract.Intent.Save); testScheduler.runCurrent()
        assertNotNull(next.uiState.value.error); assertEquals(0, repository.saved.size)
    }
    @Test fun saveCopyUsesUnsavedContentAndFreshOwnedIdentities() = runTest(dispatcher) {
        val vm = editor(); testScheduler.runCurrent()
        vm.onIntent(RoutineEditorContract.Intent.Edit(vm.uiState.value.draft!!.copy(name = "My unsaved version")))
        repository.conflict = true
        vm.onIntent(RoutineEditorContract.Intent.Save); testScheduler.runCurrent()
        assertTrue(vm.uiState.value.conflict); assertNotNull(store.payload)
        repository.conflict = false
        vm.onIntent(RoutineEditorContract.Intent.SaveCopy); testScheduler.runCurrent()
        val copy = repository.saved.single()
        assertEquals("My unsaved version", copy.name); assertNotEquals("existing",copy.id)
        assertTrue(copy.exercises.first().id != "entry" && copy.exercises.first().sets.first().id != "set"); assertNull(repository.expected.single()); assertNull(store.payload)
    }
    @Test fun effectWaitsForCommitAndPublishFreezesEdits() = runTest(dispatcher) {
        val vm = editor(); testScheduler.runCurrent()
        val effects = mutableListOf<RoutineEditorContract.Effect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.effect.collect { effects.add(it) } }
        repository.gate = CompletableDeferred()
        val current = vm.uiState.value.draft!!
        vm.onIntent(RoutineEditorContract.Intent.Save); testScheduler.runCurrent()
        assertTrue(vm.uiState.value.isSaving); assertTrue(effects.isEmpty()); assertNotNull(store.payload)
        vm.onIntent(RoutineEditorContract.Intent.Edit(current.copy(name = "Too late")))
        assertEquals(current,vm.uiState.value.draft)
        repository.gate!!.complete(Unit); testScheduler.runCurrent()
        assertIs<RoutineEditorContract.Effect.Saved>(effects.single()); assertNull(store.payload)
    }
    @Test fun failedReloadAndDiscardRetainRawDraftAndAllowRecovery() = runTest(dispatcher) {
        val vm = editor(); testScheduler.runCurrent()
        val edited = vm.uiState.value.draft!!.copy(name = "Local")
        vm.onIntent(RoutineEditorContract.Intent.Edit(edited)); testScheduler.runCurrent()
        repository.failRead = true
        vm.onIntent(RoutineEditorContract.Intent.Reload); testScheduler.runCurrent()
        assertEquals(edited,vm.uiState.value.draft); assertNotNull(vm.uiState.value.error)
        store.failDelete = true
        vm.onIntent(RoutineEditorContract.Intent.Back); vm.onIntent(RoutineEditorContract.Intent.Discard); testScheduler.runCurrent()
        assertNotNull(store.payload); assertTrue(vm.uiState.value.confirmExit); assertFalse(vm.uiState.value.isSaving)
        store.failDelete = false
        vm.onIntent(RoutineEditorContract.Intent.Discard); testScheduler.runCurrent(); assertNull(store.payload)
    }
    @Test fun libraryReadFailureIsNotAnEmptySuccessAndResumeIsVisible() = runTest(dispatcher) {
        repository.failRead = true
        val failed = library(); testScheduler.runCurrent()
        assertNotNull(failed.uiState.value.error); assertFalse(failed.uiState.value.isLoading)
        repository.failRead = false
        failed.onIntent(RoutineLibraryContract.Intent.Retry); testScheduler.runCurrent()
        assertNull(failed.uiState.value.error)
        failed.onIntent(RoutineLibraryContract.Intent.Start); testScheduler.runCurrent()
        assertTrue(failed.uiState.value.resumed)
        failed.onIntent(RoutineLibraryContract.Intent.Archive)
        assertTrue(failed.uiState.value.archiveConfirmation)
    }

}
private class MemoryDrafts : DraftStore {
    var payload: String? = null; var failDelete = false
    override suspend fun get(kind: String, id: String) = payload
    override suspend fun put(kind: String, id: String, payload: String) { this.payload = payload }
    override suspend fun delete(kind: String, id: String) { check(!failDelete); payload = null }
}
private class Repository : RoutineRepository {
    var item = Routine("existing", "Original", revision = 1, exercises = listOf(RoutineExercise("entry", "exercise", 90, listOf(RoutineSet("set", 10.0, 8)))))
    var conflict = false; var failRead = false; var gate: CompletableDeferred<Unit>? = null
    val saved = mutableListOf<Routine>(); val expected = mutableListOf<Long?>()
    override fun observeAll(includeArchived: Boolean): Flow<List<Routine>> = flow { check(!failRead); emit(listOf(item)) }
    override fun observeById(id: String) = flowOf(item)
    override suspend fun getById(id: String): Routine { check(!failRead); return item }
    override suspend fun save(routine: Routine, expectedRevision: Long?): Result<Routine,RoutineError> {
        if(conflict) return Result.Error(RoutineError.REVISION_CONFLICT)
        gate?.await(); saved.add(routine); expected.add(expectedRevision)
        return Result.Success(routine)
    }
    override suspend fun duplicate(id: String): Result<Routine,RoutineError> = Result.Success(item.copy(id = "copy"))
    override suspend fun archive(id: String, expectedRevision: Long): Result<Unit,RoutineError> = Result.Success(Unit)

}

private class Catalogue : ExerciseCatalog {
    val items = listOf(Exercise("exercise", "Bench", MuscleGroup.CHEST))
    override fun observeExercises() = flowOf(items)
    override suspend fun getAllExercises() = items
    override suspend fun getExerciseById(id: String) = items.find { it.id == id }
    override suspend fun getExercisesCount() = items.size
    override suspend fun saveExercise(exercise: Exercise) = Unit
    override suspend fun saveExercises(exercises: List<Exercise>) = Unit
}
