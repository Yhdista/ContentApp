# 📋🗓️ Routines and Training Plans Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement task-by-task. Canonical task state and rulings live in claude-mem work_state (`routines-trainingplans-implementation`); scratch reports are not a second to-do list.

**Goal:** Implement independent routines and cyclic training plans which create immutable workout prescriptions, preserving existing data and enforcing module boundaries.

**Architecture:** Feature API/impl modules share one exercise catalogue and a generic transactional command coordinator. App-owned workflows compose feature APIs inside one Room transaction. Editors persist drafts; typed Navigation Compose owns screen entries.

**Tech Stack:** Existing Kotlin 2.2.10, Compose Multiplatform 1.7.0, Room KMP 2.7.0-alpha11, Koin 4.0.2; Navigation Compose org.jetbrains.androidx.navigation 2.8.0-alpha10 (verify all targets before adopting).

**Spec:** [Approved design](../specs/2026-10-28-routines-trainingplans-design.md), approved by user “pokracuj” on 2026-10-28 12:03. Implementation proceeds without another selection/approval menu.

## 🛡️ Global Constraints

- API/impl modules for workout, exercises, routines and trainingplans; no feature impl → foreign feature impl dependency.
- foundation remains independent of Compose, Room, Koin and fitness domain; designsystem remains domain-agnostic.
- Android, desktop JVM and iOS simulator must compile; keep existing workout functionality.
- Names trim to 1–100 characters; notes ≤2,000; routines 1–50 exercise entries, each 1–20 sets; reps 1–999; finite nonnegative weight; rest 0–3,600; plans 1–31 stable slots.
- One active workout. Accepted commands, data changes and outbox are atomic; no destructive migration or automatic simulated upload.
- Source routine/plan revision + slot + progressRevision captured at start. Advance exactly once on completion; skip/cancel/start rules follow spec.
- Archived references block new starts; preserve history. Drafts are local, durable and never synchronized.
- Domain errors retain validation/not-found/archived/revision-conflict identity. Cancellation is propagated.
- All user-facing documentation follows Czech language with expressive relevant emoji. Code symbols remain plain.
- Work in the existing clean checkout on feature branch; no parallel implementation agents. Sequential task reviews; retain canonical state in work_state.

## 🔍 Review Focus

- Repeated same exercise in one routine must retain distinct exercise-entry identity in the workout and editor.
- A one-slot plan or full cursor wrap during a workout must not cause an unnoticed second advance: compare progressRevision, not just slot ID.
- Editing/archiving source data never changes an existing workout prescription or its display names/rest settings.
- Saving/autosaving a draft while navigating away must not lose the last accepted change or resurrect a deleted draft after save.
- Migration must handle dangling catalogue IDs and preserve old outbox JSON byte-for-byte, including legacy template payloads.

## 🗂️ File and Interface Map

- `core/storage`: generic CommandCoordinator, RoomCommandCoordinator(RoomDatabase, ApplicationScope), MutationDao/SyncMutationEntity, durable editor draft DAO/store.
- `core/presentation`: existing BaseViewModel and screen ViewModel helper; navigation-entry ownership replaces composition-owned stores in final task.
- `feature/exercises/api`: keep Kotlin packages `features.exercises.api`; Exercise, MuscleGroup, ExerciseCatalog contract. `impl`: catalogue entity/DAO/Room adapter.
- `feature/workout/api`: existing `features.workout.domain` models/contracts moved here; legacy templates remain only until replaced in Task 4. `impl`: existing workout UI/data, depending only on API modules, core and designsystem.
- `feature/routines/api`: `features.routines.api` models, typed errors/results, RoutineRepository and routes. `impl`: persistence, drafts, later presentation.
- `feature/trainingplans/api`: `features.trainingplans.api` models, PlanRepository and routes. `impl`: persistence, drafts, later presentation.
- `composeApp/app`: only platform hosts, Room @Database/migration, DI, workflow adapters and navigation graph.

API compatibility decisions: RoutineSet uses existing WorkoutSetType from workout API (workout API never depends on routines API). WorkoutStartPrescription is neutral and belongs to workout API. UI invocation contracts StartRoutineWorkout, StartPlanWorkout and CompleteWorkout belong to workout API and are implemented in app workflows. Storage detail methods are separate from UI action contracts.

### Task 1: 📦 Extract modules and generic infrastructure

**Files:** Create Gradle modules under `core/{storage,presentation}` and `feature/{exercises,workout}/{api,impl}`; move current source while retaining stable package names except catalogue types (`features.exercises.api`). Modify `settings.gradle.kts`, version catalog and app DI/DB imports. Move outbox source unchanged from app `core/database` to core/storage; coordinator becomes generic.

**Interfaces:** Produces `core.storage.CommandCoordinator.write<T>(block: suspend () -> T): T` and `RoomCommandCoordinator(database: RoomDatabase, applicationScope: ApplicationScope)`; ExerciseCatalog observes/lists/looks up and writes catalogue records using current IDs. Exercise gains a domain archived flag defaulting false; persistence of that flag and the archive command are introduced with schema 7 in Task 4, not as a v6 schema change. Existing WorkoutRepository and models move to workout API; WorkoutLocalDataSource, entities/DAO and current screens move to impl. Expose feature Koin module factory from impl so app DI does not require internals.

- [ ] Add a regression to existing real-Room tests verifying generic coordinator can commit two writes and rolls back both on failure; observed failure before refactor can be a compile failure for absent generic interface. Existing suite is the behavior-preserving refactor gate.
- [ ] Extract catalogue model/entity/DAO from WorkoutDao and expose its repository via API. Existing workout local source consumes catalogue API, not catalogue impl. Existing fake repository tests adapt to the catalogue interface. Keep old SQL table names, columns and DB version until migration Task 4; archived state comes with schema 7, not premature v6 changes.
- [ ] Move shared presentation and storage; every feature/module uses the same CommandCoordinator singleton. Preserve existing tests and public behavior. Mark internals internal where app composition does not need entity/DAO visibility.
- [ ] Add Navigation Compose dependency in a smoke compilation path with serializable sample routes or final route contracts; prove Android/desktop/iOS compilation. Do not switch actual navigation yet. Do not silently upgrade unrelated libraries; document any compatibility adjustment.
- [ ] Run desktop suites and compileDebugKotlinAndroid + compileKotlinIosSimulatorArm64. Commit independently testable extraction and provide exact command results.

### Task 2: 📋 Implement routines domain, persistence and drafts

**Files:** Create `feature/routines/api` and `impl`: RoutineModels.kt, RoutineRepository.kt, RoutineValidation.kt, RoutineEntities.kt, RoutineDao.kt, RoomRoutineRepository.kt and tests. Shared drafts live in `core/storage` (EditorDraftEntity/Dao and RoomDraftStore); add routine/draft tables to app database provisionally at version 7, migration supplied Task 4. Preserve legacy template tables for migration.

**Interfaces:**
- `Routine(id: String, name: String, notes: String, revision: Long, archived: Boolean, exercises: List<RoutineExercise>)`.
- `RoutineExercise(id: String, exerciseId: String, restSeconds: Int, sets: List<RoutineSet>)`; `RoutineSet(id: String, weightKg: Double, reps: Int, setType: WorkoutSetType)`; list order is persisted via orderIndex.
- `RoutineRepository`: observeAll(includeArchived=false): Flow<List<Routine>>, observeById(id): Flow<Routine?>, getById(id): Routine?, save(routine, expectedRevision: Long?): Result<Routine, RoutineError>, duplicate(id): Result<Routine,RoutineError>, archive(id,expectedRevision): Result<Unit,RoutineError>. New records begin revision 1, updates require expectedRevision and increment atomically. Error types implement foundation RootError.
- `DraftStore`: suspend get(kind,id): String?, put(kind,id,payload): Unit, delete(kind,id): Unit; persistence uses the generic coordinator and emits no outbox. API payloads use Json serialization.
- Routine feature repository consumes ExerciseCatalog and CommandCoordinator/MutationDao; never other impl.

- [ ] Test boundary validation at 0/1/50/51 entries; 0/1/20/21 sets; reps 0/1/999/1000; negative/NaN/infinity weight; rest -1/0/3600/3601; blank/100/101 name and 2000/2001 note.
- [ ] Implement atomic full-aggregate save: revision CAS, catalogue check, stable child IDs and ordering, granular typed JSON outbox. Draft save accepts incomplete form without treating it as a live routine.
- [ ] Test duplicate with fresh aggregate/entry/set IDs and preserved values, repeated exercise IDs retaining distinct entries, stale editor conflict without overwriting, archive preserving rows, DB error rollback including outbox and durable drafts surviving reopening.
- [ ] Export Koin registrations; app database composes DAO/entities. Add fixtures using real Room for aggregate rollback rather than relying only on fake DAO. Run relevant suites; commit.

### Task 3: 🗓️ Implement cyclic plans and revision-aware progress

**Files:** Create feature/trainingplans api/impl models, repository, validation, DAO/entities, RoomTrainingPlanRepository and unit/integration tests. Add DB tables and DI wiring, keeping app schema 7.

**Interfaces:**
- `TrainingPlan(id,name,notes,revision,archived,slots: List<PlanSlot>)`; `PlanSlot(id,label,routineId)` ordered by list index.
- `PlanProgress(planId,nextSlotId: String?,progressRevision: Long,needsReview: Boolean)`.
- `TrainingPlanRepository`: observeAll, observeById, getById, observeProgress/getProgress, save(plan,expectedRevision): Result<TrainingPlan,PlanError>, duplicate, archive, selectActive(planId?), observeActive, skipNext(planId,expectedProgressRevision), confirmNext(planId,slotId,expectedProgressRevision), recordCompleted(source: PlanWorkoutSource): Result<ProgressOutcome,PlanError>.
- `PlanWorkoutSource(planId,planRevision,slotId,progressRevision)` is a neutral model OWNED BY workout API (add there in this task and import into plan API), avoiding a workout→plans→routines→workout cycle. `ProgressOutcome` Advanced / ReviewRequired / ArchivedUnchanged. Implement recordCompleted as transaction participant; workout completion use case owns idempotency and calls it only for first completion.

- [ ] Test plan validation 0/1/31/32 slots, duplicate routine across slots, stable slot IDs and ordering, stale save revision, active-plan switch retaining independent progress, duplicate with fresh IDs.
- [ ] Test A→B→C→A, one-slot cycle, skip without workout, confirm selection clearing needsReview and incrementing progressRevision. All cursor changes increment revision. Start blocked for needsReview, null cursor or archived routine; only current slot can be plan-started.
- [ ] Test removal of cursor slot → null cursor + needsReview; source plan edit → ReviewRequired; one-slot skip and full-cycle wrap → ReviewRequired; archived source plan → no advance; selecting a different active plan does not redirect completion.
- [ ] Persist plan save/progress/outbox transactionally; routine references validated through RoutineRepository API. RecordCompleted must compare all three slot/plan/progress revision values and never suppress workout completion on a legitimate review-required outcome.
- [ ] Run domain + real Room integration tests and commit.

### Task 4: 🏋️ Integrate snapshots, workflow and migration 6 → 7

**Files:** Workout API models/actions and storage contracts, workout entity/DAO/repository updates; `composeApp/app/workflow/TrainingWorkflow.kt`; `app/database/Migrations.kt` and schema 7; `desktopTest` migration/workflow coverage. Remove legacy template API/UI/data after data migration is defined; source schema 6 remains committed.

**Interfaces:**
- `WorkoutStartPrescription`: title, ordered exercise entries (unique entry ID, catalogue ID, name/muscle-group snapshots, rest seconds, ordered set prescriptions), sourceRoutineId/revision?, sourcePlan context?.
- Session adds optional source context. Persist WorkoutExercise rows with names/order/rest; sets reference workoutExerciseId and optional immutable prescribed weight/reps/type while existing fields retain actual values. Old imported sets have null prescribed/source values.
- `StartOutcome` Created(sessionId) / ResumedExisting(sessionId).
- UI action contracts `StartRoutineWorkout.start(routineId)`, `StartPlanWorkout.start(planId)`, `CompleteWorkout.complete(sessionId)` return typed Result; app implements and injects them. WorkoutStore/API low-level methods createFromPrescription and completeOnce return first-completion flag; all join shared coordinator transaction.

- [ ] Test simultaneous routine/plan starts create one full session and resume existing without altering source, and failed third set or outbox insert rolls back all rows.
- [ ] Start copies routine + catalogue values into immutable workout-exercise and prescribed-set columns; later routine changes/catalogue rename/archive leave history untouched. Same catalogue exercise twice remains two independent workout entries.
- [ ] Complete once atomically stores performance, matching plan progress and outbox. Repeated completion does not advance or enqueue again. Cancellation does not advance. Plan mismatch commits completion with needsReview. Inject failure during progress/outbox write and prove entire completion rolls back.
- [ ] Implement manual migration 6→7: keep catalogue IDs, convert templates into routines with same IDs; expand targetSets to individual prescriptions; use NORMAL and catalogue rest. Dangling IDs get archived placeholders. Preserve original workout values, deterministically reconstruct entry grouping/order and leave prescribed/source columns null; keep every old mutation ID/payload exactly. Remove old template tables only after successful conversion inside migration.
- [ ] Build real v6 fixture using committed schema SQL, including repeated exercise template entries, missing catalogue records, legacy workout and an escaped legacy mutation. Open with production migration; assert exact preserved IDs/values, converted routines, original payload and exported schema validation.
- [ ] Replace ActiveWorkout loading/grouping by workoutExercise identity so repeated exercises render independently. Preserve old workout editor, stats, rest timer and export. CSV/JSON export must retain identity/order and use name snapshots for historical display; show missing prescribed values honestly.
- [ ] Run all desktop + Android tests, APK build and iOS compile; commit.

### Task 5: 📱 Deliver screens, drafts and typed navigation

**Files:** Routine/plan Contract/ViewModel/Root/Screen packages in respective impl modules; designsystem nucleus facades/atoms for new UI; app navigation graph and Koin action wiring; shared presentation helper uses nav entry ViewModelStoreOwner; source-specific workout labels/detail and “Vytvořit rutinu” integration.

**Interfaces:** Serializable routes in feature API: WorkoutHistoryRoute, ActiveWorkoutRoute, WorkoutDetailRoute(sessionId), RoutineListRoute, RoutineDetailRoute(id), RoutineEditorRoute(draftId), PlanListRoute, PlanDetailRoute(id), PlanEditorRoute(draftId). Roots accept navigation callbacks and public action contracts, never AppNavController. Shared rememberScreenViewModel obtains current nav entry owner, retains per-entry instance, clears on actual pop.

- [ ] Add ViewModel tests before implementation: valid/invalid save, stale revision with reload/save-copy, loading error kept distinct from empty, successful effect only after commit, archive affecting-start warning, resume existing message, plan needsReview repair, active plan selection.
- [ ] Editors support exercise/slot selection, add/remove/reorder, per-set fields, labels, duplicate and archive. Routine detail shows count of dependent plans before save. Plan detail provides current slot start, skip, explicit review-required next-slot selection and standalone routine start for other slots. List/detail empty, loading, error and archived states are visible.
- [ ] Persist draft on accepted edits and restore by draftId across navigation/host recreation. Atomic publish deletes the draft inside the save transaction; await accepted autosaves before publish so an older save cannot resurrect the draft. Back with dirty draft offers continue editing or discard; failures keep draft and show recoverable error. Invalid drafts remain editable. Tests must cover latest accepted change during back, reopen, conflict save-copy and no resurrection after publish.
- [ ] Add generic Material-facing wrappers in designsystem nucleus and domain-agnostic atoms/molecules for new screens, 48dp interactive targets, insets, theme and dark/light previews. Keep product models and ViewModels out of designsystem. Reuse existing styling without forcing a rewrite of old workout screens.
- [ ] Replace custom AppNavigationState/AnimatedContent/PlatformBackHandler with one typed NavHost. Main destinations: Tréninky/Rutiny/Plány. Validate pop back to source after workout, route argument restoration, draft editor reopen, ViewModel entry ownership and Android system back. Remove obsolete custom navigation test only when replacement behavior coverage exists.
- [ ] Preserve existing template affordances as links to new routines; workout detail “Vytvořit rutinu” opens editor draft using actual sets/order/types/rest, without old averaging, and creates a NEW routine only on explicit save.
- [ ] Update README/architecture/MVI documentation to actual module map, screen flows, schema7, remaining limitations and verification commands. Run full suites, Android APK + iOS compilation, and visual smoke test on desktop if available. Commit.

### Task 6: 🔍 Final acceptance and whole-branch review

**Files:** Acceptance integration tests, updated documentation and any narrowly scoped corrections from review.

- [ ] Run all twelve spec acceptance scenarios. Assert actual storage outputs, not mocked constructor calls or source-text checks. Verify module graph contains no foreign impl dependency.
- [ ] Execute desktop and Android suites for all modules, assembleDebug, compileKotlinIosSimulatorArm64, and link/document checks. Read every failure; do not discard pre-existing failures silently.
- [ ] Dispatch independent whole-branch review with baseline 402821a and final HEAD; fix important findings with covering regressions and re-review.
- [ ] Report feature branch, commits, test evidence, remaining material limitations. Do not push or merge to shared main without the user's request covering this new implementation.
