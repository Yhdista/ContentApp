# 🛠️ Runtime/API review fix — DONE

✅ Opravené Astra P2 historických statistik, Opus P3 terminálního vlastnictví a úzké P4 veřejného API. K všech consumers a10přesných L1 metod GREEN dle root/Air evidence; žádná plná G v této jednotce.

## 📦 Revize a rozsah

- 📂 Worktree `/Users/yhdista/AndroidApps/ContentApp-worktrees/atomic-task2`, branch `refactor/atomic-task2`, BASE `58a1d5b`. Pracovní strom čistý, implementer nic nemergoval/nepushoval.
- 🧪 Tests-only `e23089af78faa401b9ed8beb150c8ac6679ba815` → tests-only compile oprava `5fe9c342d329afc1f868966c8cc189fd8a1e4f3d` → zdrojová oprava `a460474fb0c3fdf16c3cbecb651863693bf75bda` → fake-only compile oprava **`c21e08599d14f94e954186d8373b990fc61da817`**.
- 🔐 Zdrojová oprava začala až po rootem potvrzeném skutečném RED všech regresí. Root vlastní veškerý Gradle a Air gate. Implementer nespouštěl Gradle, suite/statiku ani agenty.
- 🧭 Řízení yhdi-dev; CodeGraph použit před lokalizací/čtením zdrojů; žádné změny styles/badges/guards/CI/docs mimo tento report a rootem vyžádanou drobnou wrapper-inventory opravu původního task-2-report.

## 🧱 Implementace

- **Historické statistiky:** `ExerciseStatsCalculator` vytvoří mapu pouze dokončených sessions a před každým PB/součtem/history filtruje actual checked série průnikem jejich session ID s mapou. Při dočasně rozpojených Flow emisích čeká série na odpovídající dokončená metadata. History používá skutečný název/čas přes `getValue`, žádný epoch0/title fallback. DAO, schéma, immutable prescriptions a drafty beze změn. Skutečná Room regrese jde přes `OfflineFirstWorkoutRepository.getExerciseStats`, starší dokončenou session a novou aktivní checked sérii; až app `TrainingWorkflow.complete` přidá novou session do správného časového pořadí a součtů.
- **Terminální vlastnictví:** `ActiveWorkoutContract.State.isTerminated` znamená sticky committed výsledek; `isSaving` je pouze průběžný příkaz a stále se uvolňuje ve finally. Úspěšný Finish/Cancel nastaví terminated před buffered efektem, následné intents této instance jsou ignorované. Failure flag nenastaví a dovolí retry. Thumb actions vyžadují !isSaving && !isTerminated. Původní unlock-after-success assertion nahrazuje silnější ověření jediného příkazu i při ukončeném observeru; nové testy ověřují jediný buffered efekt a retry pouze do prvního úspěchu. **Netvrdíme runtime reprodukci dvojitého host popu; NavHost a delayed retained-entry test se nemění.**
- **Úzké veřejné API:** odstraněny `WorkoutRepository.addSet/removeExerciseFromWorkout` i jejich katalogové fallbacky. Strict `addSetToEntry/removeWorkoutExercise` přímo vlastní operaci pro konkrétní occurrence ID, bez compatibility aliasu. Produkční vnější callers legacy příkazů neexistovaly. Reálné fixture helpers vytvoří explicitní occurrence atomickým příkazem a načtou její počáteční actual set; další série/removal používají její přesné ID. Původní volume/PR/rollback/renumber/repeated-entry assertions zachovány. VM fake má pouze testový seedSet helper.

## 🔴 RED a opravené compile chyby

Root/Air skutečné assertion výsledky: kalkulátor40→1000 a0→80; Room expected1→actual2; API reflection legacy metoda existuje; VM5 expected1→actual2commands nebo retry2→3. Strict duplicate-entry test již PASS. Celkem **9očekávaných selhání +1PASS**, RED7s+5s.

První VM testovací kompilace měla odvození singleton typu expected effect listu. `5fe9c34` explicitně uvádí `listOf<ActiveWorkoutContract.Effect>`; assertions nezměněny. Skutečný VM RED byl zaznamenán až pak, před source fixem.

První zdrojový K běh20s našel4bare legacy delegáty v read-only Detail/History fakech, přehlédnuté implementerem při hledání dotted callers. `c21e085` upravuje pouze tyto bodies na typed `error("Not used by this read-only screen")`; obě obrazovky tyto edit příkazy nepoužívají. Žádné assertions/produkce při této opravě neměněny.

📎 Evidence: `/Users/yhdista/AndroidApps/ContentApp/docs/reviews/evidence/2026-10-29-hardening/review-runtime-red.json`, root `.yhdista/review-runtime-red.tgz` a `review-runtime-red2.tgz`.

## ✅ K a přesné L1

Root/Air potvrdil na **c21e085** K všech consumers (common metadata, desktop main/test a Android main/hostTest): **exit0,7s,391tasks =31executed +360up-to-date**, BUILD SUCCESSFUL. Přesný příkazový řetězec K spravuje root; implementerovi nebyl celý předán, proto zde není rekonstruovaný odhadem. Air log `/Users/yhdista/.yhdi-mesh/worktrees/contentapp-atomic-build/.gradle/yhdista-logs/gw-20261029T012020Z-3783.log`, lokální souhrn v root `.yhdista/review-runtime-green2.tgz`.

**10unikátních L1 PASS,0failures/errors/skips.** Kalkulátor2+Room/interface3 GREEN na a460474; VM5 GREEN na c21e085 (následná změna pouze4read-only fake bodies). Přesné tasky/filtry:

```sh
gw :feature:workout:api:desktopTest \
  --tests '*ExerciseStatsCalculatorTest.activeAndUnmatchedSetsNeverEnterHistoricalStats' \
  --tests '*ExerciseStatsCalculatorTest.setsWaitForCompletedSessionMetadataWithoutEpochFallback'
gw :feature:workout:impl:desktopTest \
  --tests '*ActiveWorkoutViewModelTest.successfulFinishIgnoresLaterTerminalIntentsBeforeEffectConsumption' \
  --tests '*ActiveWorkoutViewModelTest.successfulCancelIgnoresLaterTerminalIntentsBeforeEffectConsumption' \
  --tests '*ActiveWorkoutViewModelTest.failedFinishAllowsRetryUntilFirstSuccess' \
  --tests '*ActiveWorkoutViewModelTest.failedCancelAllowsRetryUntilFirstSuccess' \
  --tests '*ActiveWorkoutViewModelTest.observerFailureCannotUnlockPendingFinish'
gw :composeApp:desktopTest \
  --tests '*TrainingWorkflowTest.exerciseStatsExcludeActiveCheckedSetsUntilWorkflowFinish' \
  --tests '*TrainingWorkflowTest.repositoryExposesOnlyEntryStrictSetAndRemovalCommands' \
  --tests '*TrainingWorkflowTest.entryCommandsRejectCatalogueIdsWithoutMutatingRepeatedOccurrences'
```

`gw` výše označuje rootem použitý absolutní yhdi-dev wrapper, žádné přímé gradlew. Přesné rozdělení těchto filtrů do jednotlivých root gate invokací určuje uložená evidence; celý class/module běh v této jednotce nebyl.

📎 Case-by-case autoritativní GREEN: `/Users/yhdista/AndroidApps/ContentApp/docs/reviews/evidence/2026-10-29-hardening/review-runtime-green.json`. Root archivy `review-runtime-green.tgz` + `review-runtime-green2.tgz` obsahují log souhrny a testové výsledky. Implementer přečetl obě JSON evidence a finální gw souhrn; nevydává to za svůj nový běh.

## 📋 Odloženo / limity

Root výslovně určil existující P4/atomic/cancellation/PR/persistence testy a delayed workout retained-entry test do **finální scoped workout-api/impl + app sady po UI jednotce**, nikoli další jednotlivé běhy nyní. Příslušné fixtures/fakes jsou compile-ověřené K. Žádná další plná G, iOS/APK/native smoke ani host double-pop runtime proof v této jednotce. Žádné nové známé scoped blockers.

Self-check: `git diff 58a1d5b..HEAD --check` PASS; dotted i bare caller search neukazuje legacy repository calls/overrides (samostatný soukromý addSet UI handler není repository API). Schéma/drafty/NavHost/delayed test nezměněny, workingtreeclean.

## ⏱️ Časy

Začátek interakce nebyl instrumentován; první tests-only commit2026-10-29T02:14:48+01:00, source fake-fix commit02:20:04, dokončení reportu **2026-10-29T02:21:57+01:00**. Přesná celková délka neznámá. Root hlásí RED7+5s, první K20sFAIL, závěrečné K/VM7sPASS: **nejméně39s kombinovaných Gradle gate časů**. Samostatný čas kalkulátor/Room GREEN, pure compile/test CPU a Air transfer/gate-wait rozpad nebyly předány, neoznačujeme je nulou ani čistým testovým časem. Implementer žádný heavy krok nespouštěl.
