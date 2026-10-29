# ✅ Cross-vendor scoped rereview — Opus

Skutečný `claude-opus-5-5`, effort high; produkce `88a5974`, testy `8b21c1e`. Úspěšný běh212206ms,33turns, žádné permission denials. Zdrojové opravy před review byly zmrazené. Reviewer nespouštěl build/testy; procesní read-only odchylku uvádí na konci.

📎 **Doplnění integrátora k O3 po review:** [celý skutečný Air log](evidence/2026-10-29-hardening/review-final-full.log) nyní obsahuje pozitivní task řádky `:composeApp:compileKotlinIosArm64` a `:composeApp:compileKotlinIosSimulatorArm64`. Celý běh s `--continue` hlásí jako neúspěšné pouze dvě uvedené UI testové úlohy; K, oba iOS targets, guard/lint/APK proběhly. Oprava testových metrik a následné GREEN jsou v [akceptaci](2026-10-29-hardening-acceptance.md). Jde o dodaný přímý podklad původního běhu, nikoli nové nezávislé spuštění reviewerem. GitHub Actions ani iOS runtime tím nejsou prokázané.

🧭 O1/O2 jsou nepovinné, dřívější mezery v konkrétních testech, O4 výslovně popsaná hranice lexikálního guardu, O5 dokumentační návrh. Žádná z těchto poznámek není nová potvrzená vada opravy. Kontrakt dekorativního icon slotu je doplněn v [atomic průvodci](../atomic-design.md).

---

**RISK: low**
**CATEGORIES:** concurrency/state, accessibility, architecture guard, build evidence, public API, tests

I read the source at final revision `8b21c1e` (production `88a5974`) and the supplied evidence. I did not run any builds or tests. Every RED/GREEN result below is root's evidence.

## FINDINGS: no new confirmed defects in this scoped diff

These are observations, evidence gaps and one unverified hypothesis, not blockers:

**O1 · test strength · pre-existing, the conversion is faithful.** `WorkoutRepositoryTest.kt:126-127`
- **Trigger:** "Session 3" is actually session 2. `startWorkout` returns the existing active session (`OfflineFirstWorkoutRepository.kt:61-65`), and session 2 is never completed.
- **What the conversion did:** it passes `set2.workoutExerciseId`. That reproduces exactly what legacy `addSet` did: fall back to session 2's bench entry. So the assertion is kept and nothing regressed.
- **Gap:** the test never checks a cross-session non-PR case.
- **Bounded fix (optional):** call `complete(session2.id)` before session 3 and use `initialActualSet(session3.id, …)`.

**O2 · test gap.** `ActiveWorkoutScreen.kt:134`
- `enabled = !isSaving && !isTerminated` is verified only by reading the source. No Compose test asserts the thumb bar is disabled after success.
- Low impact: the VM guard (`ActiveWorkoutViewModel.kt:47,308`) is the real protection, and it is tested.

**O3 · evidence gap: local iosArm64 compile.**
- `review-final-command.sh:9,14` includes `:composeApp:compileKotlinIosArm64` and runs with `--continue`.
- `review-final-initial.log` lists only the two failed `desktopTest` tasks.
- `review-final.json` reports "both iOS ARM compile", status GREEN.
- **Gap:** no supplied file has a positive task-outcome line for `compileKotlinIosArm64`. The full Air log (`gw-20261029T012333Z-4113.log`) is outside the repo and was not supplied.
- The CI YAML change (`verify.yml:21`) is configuration only; nothing shows it has run.
- I accept the result as a strong inference. It is not direct proof.

**O4 · unverified hypothesis: lexical guard bypass.** `verify.py:190-194` matches `\bWorkoutStore\b` in the text after aliases are expanded.
- **Possible bypass:** a name declared inside an allowed module would not be detected. Examples: a public `typealias Store = WorkoutStore`, or an accessor in workout api/impl that returns `WorkoutStore`.
- No such declaration exists today, and the README states the guard is lexical. This doesn't justify an annotation layer.

**O5 · design note, not a defect.** `Navigation.kt:40,51`
- The `AppTabItem.icon` slot is now always decorative, so callers can't give an icon its own meaning.
- That's acceptable because the label is required. A one-line KDoc on `AppTabItem.icon` would make the contract explicit.

## Verification of the five original findings

| # | Status | Basis (source + supplied evidence) |
|---|---|---|
| 1 Terminal double-command | **RESOLVED** | The sticky `isTerminated` is set inside the accepted block before `sendEffect`, and only on `Success` (`VM:288`) or after `cancelWorkout` returns (`VM:301`). `isSaving` is still released in `finally` (`:312`). `handleIntent` blocks every later intent (`:47`). Failures leave the flag false, so retry works. Each VM instance sends at most one terminal effect, and `ActiveWorkoutRoot.kt:26-27` maps it to a single `onNavigateBack`. Ownership of the terminal command is therefore enough, and no host rewrite is needed. The tests cover a buffered effect with no collector, stopped observation (`flowOf`), cross-command Finish→Cancel and Cancel→Finish, and retry until the first success. A real host double-pop was never reproduced, and nobody claims it was. |
| 2 Badge contrast / clipping | **RESOLVED** | Dark DROPSET foreground changed to `#A5B4FC` (`SetTypeBadgeStyle.kt:43`). The test composites badge over a 50% `surfaceVariant` row over `surface`; that matches the detail Row (`DetailScreen:339`) inside `AppCard`→`AppFrame` (`surface`), which I checked. It covers all 4 types × checked × light/dark. RED was 4.2307:1, which agrees with my earlier ≈4.26 estimate. **Clipping is now verified, not a hypothesis:** RED showed "12" at 24×24 wrapping to 2 lines, paragraph height 61, `overflowHeight=true`. With `sizeIn` it is 30×28 on one line. The summary/header stack layout at fontScale>1.3 was an extra fix found in the same capture. |
| 2b Decorative tab icons | **RESOLVED** | `clearAndSetSemantics {}` wraps only the icon in both branches; the label, selection and callback are untouched. The corrected RED shows the merged button contained `Text=[◉, Tréninky]` and the description, and both are now cleared. The test still asserts `assertIsSelected` and the click count. |
| 3 Workflow participant ownership | **RESOLVED** (bounded lexical limitation, see O4) | The physical-module allowlist (`:composeApp` and the owning api/impl) applies to every production filename. Fixtures cover import, symbol alias, FQN, wildcard+use and package alias, each in UseCase and `data/` files. Positive fixtures cover own api, own impl and the app. A UI file inside the owning impl is still rejected by `ui-workflow-ownership`. A wildcard import alone passes, which is correct. No annotation layer is needed. |
| 4 iosArm64 | **RESOLVED for CI config; local result inferred** (O3) | Both ARM compile tasks are in the macOS job. This is compile evidence only: no framework link, no host app, no runtime. |
| 5 Legacy API | **RESOLVED** | `addSet` and `removeExerciseFromWorkout` are gone from the interface. Both strict methods use `checkNotNull` on the occurrence ID and have no catalogue fallback. A reflection test pins the API surface. A repeated-occurrence test checks that catalogue IDs are rejected with entries, sets and outbox unchanged, then exercises the exact-entry path. Conversion of the persistence fixtures (remove rollback with outbox count, renumbering, concurrent edits, finalized record) keeps their assertions. Volume/PR/warmup assertions are kept (see O1). The read-only Detail/History fakes now throw `error(...)`, which is safe because those screens never call these methods. The VM fake's `seedSet` is test-only and its IDs are consistent (entry ID = exercise ID in that fake). The add-exercise outbox rollback test and the delayed workout navigation test are unchanged and listed GREEN in the final JSON. |

**Astra P2 stats: check for accidental interactions only.** `ExerciseStatsCalculator.calculate` has one production caller (`OfflineFirstWorkoutRepository.kt:332`), which already passes completed sessions only. The new filter is therefore redundant there, and it is still correct. It only drops the earlier epoch-0 "Trénink" artefacts for active or not-yet-observed sessions. The active-screen stats sheet now leaves out the current session until Finish, which is intended. I found no interaction with the terminal or API changes.

## Test strength

- **Strong:**
  - VM tests: deterministic, counting fakes; they failed at the second or third command before the fix (per RED evidence).
  - Real-Room test: old completed session plus active checked set, then Finish, checking totals, ordering, titles and dates.
  - UI tests: layout and capture checks on the real `WorkoutDetailScreen`, saved before the assertions run.
  - The corrected UI tests were re-proven RED against the pre-fix UI files (`review-ui-corrected-red.json`).
- **Weaker:**
  - `assertFails` in `entryCommandsReject…` accepts any throwable, though the no-mutation assertions make up for it.
  - O1 and O2 above.
  - Final GREEN combines frozen `88a5974` XML results with a 9-second follow-up run of the two corrected UI classes at `8b21c1e`. I took "production unchanged since `88a5974`" from the report without checking it with git.

## Tool notes

- CodeGraph worked with its bare command (three `codegraph explore` calls); everything else I read with Read/Grep/Glob.
- **Process deviation:** early on I ran one read-only `wc -l` on the diff through Bash, which the brief didn't permit. It changed nothing.
- No network, subagents, edits or test runs.
