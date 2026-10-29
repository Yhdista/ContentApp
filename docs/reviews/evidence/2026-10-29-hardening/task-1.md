# 🔐 Task 1 — runtime opravy a workout příkazy

✅ **DONE**. Commit `0a8a6d9fe2d6643f1ab8f487630068fe6e696358`; základ `38f9f4e`, branch `refactor/atomic-foundation`. Bez push/merge. Zdrojový commit obsahuje 20 souborů, +350/−108 řádků. Rootovy `.gitignore` a `MERGE-STATE.md` nebyly zahrnuty.

## 🎯 Výsledek a rozhodnutí

- **A1 / completion bypass:** `WorkoutRepository.completeWorkout` a druhá implementace/payload byly odstraněny, včetně nepoužívané completion metody v interním local datasource. Aplikační vstup zůstává `CompleteWorkout` → app-owned `TrainingWorkflow`; `WorkoutStore.completeOnce` zůstává výslovně dokumentovaný low-level integrační účastník. Nezměnil jsem jeho package, protože přesun nebyl nutný pro odstranění aplikační bypass cesty. Reflection test veřejného interface hlídá odstraněnou metodu; integrační test přes `CompleteWorkout` dokládá jediné posunutí plánu a idempotentní retry.
- **A2 / split add:** veřejný entry-only `addExerciseEntry` byl nahrazen `addExerciseWithInitialSet(sessionId, exerciseId, weightKg, reps): WorkoutExercise`. Vrací čerstvý workout occurrence, vytvoření occurrence + první série + obou outbox řádků vlastní jedna vnější writer transakce. Entry helper je private. VM volá jediný command. Vytvoření vlastního katalogového cviku zůstává samostatný katalogový příkaz. Reálný Room test dokládá rollback při druhém outbox insertu, accepted-command cancellation a nezávislé opakované výskyty stejného cviku.
- **A3 / command ownership:** obecná `onOperationFailure` všech tří VM už neuvolňuje busy/isSaving. Pouze vlastní `finally` přijatého commandu uvolňuje zámek. Pending Finish/Duplicate odolá selhání odběru; knihovny odolají i úspěšné emisi během příkazu. Chyba samotného commandu zámek uvolní a dovolí retry. Plan combine přenáší pouze data a redukuje aktuální State až v collect, místo odloženého nahrazení dřívějším State snapshotem. UI guard není vydáván za databázovou idempotenci.
- **A4 / previous performance:** SQL vyžaduje dokončenou session i sérii. Pořadí: `COALESCE(endTimeEpochMs,startTimeEpochMs) DESC`, `startTimeEpochMs DESC`, `session.id DESC`, `COALESCE(entry.orderIndex,-1) DESC`, `setNumber DESC`, `set.id DESC`. Legacy řádky bez occurrence se zachovají díky LEFT JOIN. Žádné nové timestampy ani změna schématu. Testy zahrnují opačné lexikografické ID, starý timestampový prefix a UUID, aktivní session, opakované occurrence, nerozhodné časy/ID a null endTime fallback.
- 📚 Aktualizován `docs/mvi.md` a KDoc bezprostředně souvisejících rozhraní. Ostatní scope Task2–6 se neměnil.

## 🧩 Minimální prerequisite mimo původní runtime scope

K common metadata odhalilo latentní chybu v dosud neměněném `AppNavHost.kt:70`: `Unresolved reference 'startDestinationId'`. Root výslovně schválil minimální opravu pro splnění K. `popUpTo(nav.graph.startDestinationId)` nahrazeno `popUpTo<WorkoutHistoryRoute>`; tento route je deklarovaný graph start. `saveState`, `restoreState` i `launchSingleTop` zůstávají stejné. Dostupnost typed overloadu byla potvrzena v lokálním sources JAR aktuální navigation-common 2.8.0-alpha10, commonMain/NavOptionsBuilder.kt. Task5 nesmí tuto opravu při modernizaci odstranit bez navigační regrese. Samostatný UI běh se zde nespouštěl; navigační sada patří do G.

## 🔴 RED před produkčními změnami

Čistý běh 00:37:42–00:37:47 CET, Gradle hlásí **4 s**, exit1. Log: [task-1-red.log](task-1-red.log).

```sh
./gradlew :feature:workout:impl:desktopTest :feature:routines:impl:desktopTest :feature:trainingplans:impl:desktopTest :composeApp:desktopTest --tests '*ActiveWorkoutViewModelTest*' --tests '*RoutinePresentationTest*' --tests '*PlanPresentationTest*' --tests '*WorkoutPersistenceTest*' --tests '*TrainingWorkflowTest*' --continue
```

Tento běh byl zahájen ještě před převzetím nově aktivované yhdi-dev instrukce, proto má původní širší rozsah a používá přímý gradlew. Následující běhy používají výhradně gw a úzké L1 metody.

| Sada | Testů | Očekávané regrese |
|---|---:|---:|
| workout impl | 38 | 1 — `observerFailureCannotUnlockPendingFinish` |
| routines impl | 18 | 1 — `observerFailureCannotUnlockPendingDuplicate` |
| trainingplans impl | 23 | 1 — `observerFailureCannotUnlockPendingDuplicate` |
| composeApp vybrané persistence/workflow | 33 | 3 — public bypass, split-add rollback, chronology |
| **Celkem** | **112** | **6** |

Atomic RED test cílil na tehdejší dostupný aplikační protokol `addExerciseEntry` + `addSetToEntry` a očekával rollback obou. Po zavedení nového interface byl vstup testu přepojen na `addExerciseWithInitialSet`; identické rollback assertions zůstaly. Další permanentní VM test hlídá, že UI nový command skutečně používá a nepřidává druhý write. Historický audit/probe nebyl měněn.

Před čistým RED proběhla diagnostika test harnessu: experiment s přímým interním VM z composeApp nebyl kompilovatelný a byl nahrazen testem veřejného repository protokolu. Nový VM test musel uklidit i timer výchozího fixture VM. Dva rozpracované test executory byly ukončeny; čistý RED výše žádný hang nemá. Diagnostický čas před čistým RED cca 55 s (odhad), není vykazován jako důkaz regrese.

## 🟢 GREEN — jmenovaná L1 výjimka pro hlášené A1–A4

Přesný spustitelný příkaz a všechny `--tests` filtry: [task-1-L1.sh](task-1-L1.sh). Log: [task-1-green.log](task-1-green.log). **gw PASS exit0, 32 s**, Gradle29 s, 58 actionable tasks.

| Task | Spuštěno | Fail/skipped |
|---|---:|---:|
| `:feature:workout:impl:desktopTest` | 4 | 0/0 |
| `:feature:routines:impl:desktopTest` | 2 | 0/0 |
| `:feature:trainingplans:impl:desktopTest` | 2 | 0/0 |
| `:composeApp:desktopTest` | 9 | 0/0 |
| **Celkem** | **17** | **0/0** |

GREEN zahrnuje výše uvedené nové regrese a existující completion progress rollback / idempotence / prescription previous-performance testy. Po GREEN následovalo pouze odstranění nepoužívané datasource completion metody/importů a schválená typed-navigation prerequisite; finální podobu ověřuje K. Žádné testy nebyly smazány ani oslabeny. Staré completion fixtures byly přesměrovány na low-level store v jeho vlastním modulu nebo na app action; persistence edit-guard test si finalizovaný DB stav pouze připravuje přímo DAO.

## 🧪 K — finální kompilace

Přesný recept: [task-1-K.sh](task-1-K.sh). Zahrnuje sedm modulů (`workout:api/impl`, `routines:api/impl`, `trainingplans:api/impl`, `composeApp`) pro `compileCommonMainKotlinMetadata`, `compileTestKotlinDesktop`, `compileDebugUnitTestKotlinAndroid`; také `:composeApp:compileKotlinDesktop` a `:composeApp:compileDebugKotlinAndroid`, které kompilují produkční consumers a jejich závislosti.

- První K: **FAIL34 s** na již zmíněné latentní common metadata chybě. [Log](task-1-K-failed.log).
- Finální K: **PASS exit0, 16 s**, Gradle13 s, **394 actionable tasks (35 executed,359 up-to-date)**. [Log](task-1-K-passed.log).
- `git diff --check` čistý; self-review ověřil celý vlastní diff, všechny přepojené callers/fakes a odstraněný payload.
- Finální K je kompilace, **nespouští Android unit testy**. Plné desktopTest/testDebugUnitTest, iOS compile/link, statika, spec/quality/Astra review se podle nově aktivovaného yhdi-dev provádějí jednou v závěrečné G pod root integrátorem.

## ⏱️ Časy a stroj

Práce přibližně 00:35–00:42 CET (start orientační podle předání, první test script vznikl00:36:42). K34+16=50 s, úzký L1 GREEN32 s, čistý RED4 s; předchozí diagnostika přibližně55 s. Přesné dělení compile/test v jednotlivých Gradle bězích není měřeno, uvádím kombinovaný čas. Gate čekání nebylo zaznamenáno, status před startem ukazoval unlocked; 3s rozdíl gw/Gradle zahrnuje wrapper overhead, nepovažuji jej automaticky za frontu.

Použito rootem nastavené `.yhdista/gw.env`: Gradle2g/Kotlin1g/1worker. Status:18cores,64GiB, pressurewarn, no active swap, DEFER_BUILD0. Žádné změny globálních Gradle nastavení ani ukončení cizích procesů. Žádní vlastní subagenti.

## 📂 Změněné soubory

- `composeApp/src/commonMain/kotlin/app/navigation/AppNavHost.kt`
- `composeApp/src/desktopTest/kotlin/app/workflow/TrainingWorkflowTest.kt`
- `composeApp/src/desktopTest/kotlin/features/workout/WorkoutPersistenceTest.kt`
- `docs/mvi.md`
- `feature/routines/impl/src/commonMain/kotlin/features/routines/presentation/RoutineLibraryViewModel.kt`
- `feature/routines/impl/src/commonTest/kotlin/features/routines/presentation/RoutinePresentationTest.kt`
- `feature/trainingplans/impl/src/commonMain/kotlin/features/trainingplans/presentation/PlanLibraryViewModel.kt`
- `feature/trainingplans/impl/src/commonTest/kotlin/features/trainingplans/presentation/PlanPresentationTest.kt`
- `feature/workout/api/src/commonMain/kotlin/features/workout/domain/WorkoutActions.kt`
- `feature/workout/api/src/commonMain/kotlin/features/workout/domain/WorkoutRepository.kt`
- `feature/workout/impl/src/commonMain/kotlin/features/workout/active/ActiveWorkoutViewModel.kt`
- `feature/workout/impl/src/commonMain/kotlin/features/workout/data/OfflineFirstWorkoutRepository.kt`
- `feature/workout/impl/src/commonMain/kotlin/features/workout/data/dao/WorkoutDao.kt`
- `feature/workout/impl/src/commonMain/kotlin/features/workout/data/datasource/RoomWorkoutLocalDataSource.kt`
- `feature/workout/impl/src/commonMain/kotlin/features/workout/data/datasource/WorkoutLocalDataSource.kt`
- `feature/workout/impl/src/commonMain/kotlin/features/workout/data/model/WorkoutSyncPayloads.kt`
- `feature/workout/impl/src/commonTest/kotlin/features/workout/ActiveWorkoutViewModelTest.kt`
- `feature/workout/impl/src/commonTest/kotlin/features/workout/WorkoutDetailViewModelTest.kt`
- `feature/workout/impl/src/commonTest/kotlin/features/workout/WorkoutHistoryViewModelTest.kt`
- `feature/workout/impl/src/commonTest/kotlin/features/workout/WorkoutRepositoryTest.kt`

## ➡️ Předání

Task1 implementace a K jsou dokončeny. Žádná známá otevřená funkční závada v rozsahu A1–A4. Root pokračuje Task2; full G a nezávislé review jsou vědomě odložené podle aktualizované orchestrace, nejsou deklarované jako hotové.

📎 Archivní kopie logů v této složce doplňují původně chybějící odkazované soubory. Odstraněné koncové mezery řádků nemění výstup ani výsledky; původní lokální logy zůstávají v `.superpowers/sdd/2026-10-29-atomic-architecture-hardening/`.
