# 🔍 Nezávislé cross-vendor review

Skutečný model `claude-opus-5-5`, effort high; revize produkce45e2133/docs58a1d5b. Runtime517349ms,91turns,úspěšný výsledek. Reviewer nespouštěl testy/buildy. Omezení shellu je výslovně popsané níže; zdroje četl přes Read/Grep. Toto je původní verdikt před následnými opravami.

🧭 Časový kontext integrátora: samostatná [skutečná Astra revize](2026-10-29-astra-hardening-review.md) následně proběhla a [kontrola její opravy](2026-10-29-astra-scoped-rereview.md) nález uzavřela. Historické tvrzení níže o čekající Astře popisuje okamžik původního Opus review. Dvě shell zamítnutí se týkala kombinovaných discovery/git příkazů mimo read-only allowlist; nejde o důkaz nefunkčního CodeGraphu.

# Cross-vendor review (Opus): whole wave `fb6078d..58a1d5b`

I reviewed production `45e2133` and the docs at `58a1d5b`. I did not run any builds or tests. The shell was blocked after one read-only `git` command, so I also could not run `codegraph explore`. Everything below comes from reading the source with Read/Grep and the saved `wave.diff`. The contrast ratios are my own sRGB arithmetic, not measurements of rendered pixels.

**RISK: low**
**CATEGORIES:** tests, public-api, concurrency, build. I found nothing in persistence or security.

## FINDINGS (max 5)

**1 · P3 · concurrency/tests. The source gap is verified; the user-visible duplicate navigation is a hypothesis I have not reproduced.**
- **Where:** `feature/workout/impl/.../active/ActiveWorkoutViewModel.kt:305-312`, `ActiveWorkoutScreen.kt:134`, `RoomWorkoutStore.kt:56`, `composeApp/.../AppNavHost.kt:49`
- **Trigger:**
  1. Finish succeeds and `finally` sets `isSaving=false`, so Finish and Cancel are enabled again.
  2. `state.session` stays non-null until Room's invalidation emits again. If an earlier observer failure killed the load job, it stays non-null for good.
  3. A second Finish is accepted. `completeOnce` returns `Success(firstCompletion=false)`, which sends a second `WorkoutFinished`, which calls an unguarded `nav.popBackStack()`.
  4. A second Cancel in the same window hits `check(!isCompleted)` (`OfflineFirstWorkoutRepository.kt:309`) and shows a false "unknown error" snackbar. No data is lost.
- **Expected:** a successful terminal command stays terminal, as the editors do with `closed = true` (`DurableEditorController.kt:108`).
- **Actual:** the lock is released after success. `ActiveWorkoutViewModelTest.kt:131` asserts that release, so the test locks in the gap.
- **How likely a double pop is:** both effects would have to be buffered before the `RESUMED` collector handles the first one. That window is narrow.
- **Fix:**
  - On `Success`, keep a sticky `isFinished`/`isSaving` flag and ignore all later intents.
  - Change line 131 to: "after success, a second Finish/Cancel is ignored."
  - Make the pop idempotent in the host, either with `dropUnlessResumed { nav.popBackStack() }` or `nav.popBackStack<ActiveWorkoutRoute>(inclusive = true)`.

**2 · P3 · accessibility. The contrast failure is verified by calculation; the clipping is a hypothesis.**
- **Where:** `feature/workout/impl/.../SetTypeBadgeStyle.kt:42-43`, rendered at `WorkoutDetailScreen.kt:352,361-373`
- **Trigger:** a dark-theme workout detail with a DROPSET set.
- **Why it fails:** the "D" text is `#818CF8` on a background of 20% `#6366F1`, over 50% `surfaceVariant`, over the `AppCard` `surface`. The composite is about `#272F5A`, which gives **≈4.26:1**. That is below 4.5:1 for 11sp bold text.
- **Other pairs:** the remaining light/dark badge and PR pairs I computed pass (≥5.1:1).
- **Why tests miss it:** `DesignSystemAccessibilityTest.kt:34-42` covers only DS tokens, not feature-owned colours or alpha-blended backgrounds.
- **Clipping hypothesis:** the fixed `size(24.dp)` plus `clip` probably cuts off two-digit set numbers at fontScale 2. The detail screen is not in the captures, so this is unconfirmed.
- **Fix:**
  - Use content `#A5B4FC` (≈6.4:1 on the same composite) or an opaque container.
  - Use `sizeIn(minWidth = 24.dp, minHeight = 24.dp)` instead of the fixed size.
  - Add the feature badge pairs, with their composited backgrounds, to a contrast test.

**3 · P3 · public-api/guard strength. Verified by reading `verify.py`; I did not execute it.**
- **Where:** `tools/architecture/verify.py:186-192`, `test_verify.py:145-147`, `feature/workout/api/.../WorkoutActions.kt:39-43`
- **Trigger:** a feature-impl file whose name does not end in `Screen.kt`, `Root.kt` or `ViewModel.kt` (for example `features/routines/data/Foo.kt`) injects the Koin `single<WorkoutStore>` and calls `completeOnce`.
- **Actual:** the guard passes. That recreates the A1 bypass, where a workout is completed without updating plan progress. `ExerciseCatalogImport` has the same exposure.
- **Expected:** only `:composeApp` and the implementing impl module may reference these workflow participants.
- **Fix:**
  - Make the rule module-based, with a negative fixture (`feature/a/impl/ThingUseCase.kt`) and a positive one (`composeApp`).
  - Better still, mark both interfaces with a `@RequiresOptIn` annotation such as `@WorkflowParticipant`, so the compiler enforces it.

**4 · P3 · build/evidence gap. Not a defect.**
- **Where:** `build-logic/.../contentapp.multiplatform.gradle:20`, `G-a6-command.sh:9`, `.github/workflows/verify.yml:53-54`
- **Actual:** `iosArm64` is declared as a supported target, but neither G nor CI ever compiles it. Only `compileKotlinIosSimulatorArm64` runs.
- **Fix:** add `:composeApp:compileKotlinIosArm64` to the macOS job, or state the limitation in the acceptance doc.

**5 · P4 · public-api hygiene. Optional.**
- **Where:** `WorkoutRepository.kt:31,35`, `OfflineFirstWorkoutRepository.kt:144-146,301`
- **Actual:** the legacy `addSet` and `removeExerciseFromWorkout` fall back from an entry ID to the *first* entry with a matching catalogue ID. Now that the same exercise can appear more than once in a workout (A4), that target is arbitrary. No production code calls either method; only tests do.
- **Fix:** move them to the test fixture or an `internal` scope, and keep only the entry-strict commands on the public API.

## Verified OK in source

- **Commands and cancellation:** writes are accepted synchronously into the app FIFO, and they survive the caller being cancelled. Nested writes use savepoints. Typed errors such as `Abort` and `PlanCommandFailure` roll back the whole outer transaction. Room invalidation is refreshed only after the outer commit. Publication commits the aggregate and the draft delete in one write, and a rejected publish keeps the draft session and its lease.
- **No deadlock:** the drafts FIFO waits on the writer, but writer blocks never wait on drafts.
- **A2:** in ActiveWorkout and both Library ViewModels, only the operation that took the lock releases it.
- **A3:** adding an exercise with its first set is one atomic command.
- **A4:** "previous performance" uses one corrected query for both paths, completed sessions only.
- **Analytics and prescriptions:** analytics count actual sets only. Prescriptions are insert-only (ABORT on conflict).
- **Schema:** stays at version 7, with no destructive fallback.
- **A5/A6/A7:** Material3 is an `implementation` dependency and appears only in nucleus. The DS facade exposes no Material types. No fitness or domain types are in DS. The public consumer fixture is meaningful.
- **DS contrast:** all DS token pairs are ≥5.5:1 by my calculation.
- **Removed tests:** the three removed workout-repo tests are replaced by stronger real-SQLite `ExerciseCatalogCommandsTest` tests (seeding, escaping/JSON, rollback, cancellation).
- **Guard fixtures:** they cover aliases, FQNs, wildcards, physical-vs-declared packages and every upward atomic edge.
- **Minor guard false positive:** a commonMain parameter named `platform` used as `platform.name` would be rejected wrongly. This is optional to fix.

## Whole-architecture view (Opus, *not* Astra)

I can't act as Astra or launch it: no subagents, no network. **The Astra review you asked for is still outstanding** and has to be run separately.

My own view as a reusable POC: the core is strong.
- App-owned writer and outbox.
- Draft leases.
- An editor controller used by composition, not a base class.
- `api`/`impl` modules with the app as composition root.
- A Material-hiding DS facade.
- Convention plugins and a real Gradle dependency inventory.
- The Nav2 + typed-routes stack (Kotlin 2.4.21, CMP 1.12.2, Nav 2.10, Room 2.8.5) is a coherent choice. I did not re-check these versions online.

What I'd fix before reusing it:
- Make terminal outcomes state-based, consistently with the editors (finding 1).
- Pass the Czech copy hardcoded in DS molecules/templates (e.g. "Posunout výše", "Zpět") in from callers.
- Give tab icons decorative semantics.

A clean review here is not proof of production readiness.
