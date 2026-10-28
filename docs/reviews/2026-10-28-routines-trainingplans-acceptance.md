# ✅ Akceptace rutin a cyklických plánů

📅 28. 10. 2026 · větev `feat/routines-trainingplans` · původní vstup `90905a9`, závěrečná opravná vlna od `017866e` · celá implementace od `402821a`.

🧪 Doménová akceptace a platformní sestavení procházejí. Při závěrečné kontrole byla potvrzena a opravena navigační závada při opožděném commitu. **Rozšířený desktopový scénář odhalil také chybějící notifikaci Room observerů po vnější transakci. Ta je opravena a původně visící průchod je nyní součástí zeleného visual smoke.** Nezávislé review celé větve odhalilo také zmrazené nabídky uchovaných editorů a ztrátu původního předpisu při smazání skutečnosti. Oba nálezy i produktové záložky v design systému jsou opraveny; nyní následuje cílené opakované review oprav. Nic nebylo pushnuto ani mergováno.

## 🎯 Dvanáct schválených scénářů

📋 Mapování na [schválenou specifikaci](../superpowers/specs/2026-10-28-routines-trainingplans-design.md). Zkratky odkazují na skutečné testovací soubory; SQL/CAS/outbox tvrzení níže pocházejí z reálného Room/Bundled SQLite, nikoli z mock konstruktorů.

- **R** — [RoutinePersistenceTest](../../composeApp/src/desktopTest/kotlin/features/routines/RoutinePersistenceTest.kt).
- **P** — [TrainingPlanPersistenceTest](../../composeApp/src/desktopTest/kotlin/features/trainingplans/TrainingPlanPersistenceTest.kt).
- **W** — [TrainingWorkflowTest](../../composeApp/src/desktopTest/kotlin/app/workflow/TrainingWorkflowTest.kt).
- **WP** — [WorkoutPersistenceTest](../../composeApp/src/desktopTest/kotlin/features/workout/WorkoutPersistenceTest.kt).
- **RP / PP** — [RoutinePresentationTest](../../feature/routines/impl/src/commonTest/kotlin/features/routines/presentation/RoutinePresentationTest.kt) / [PlanPresentationTest](../../feature/trainingplans/impl/src/commonTest/kotlin/features/trainingplans/presentation/PlanPresentationTest.kt), deterministické ViewModel testy nad fakes; netvrdí vlastnosti SQLite.

| # | Stav a požadavek | Konkrétní důkaz / storage výstup |
| --- | --- | --- |
| 1 | ✅ Rutina, různé série, opakovaný cvik, kopie | R: `saveReopensOrderedAggregateWithRepeatedCatalogueIdsAndTypedOutbox`, `duplicateCopiesValuesWithFreshIdsAndRevisionOneEvenFromArchive`, `updateRetainsStableIdsNormalizesOrderAndRemovesDeletedRows`. Readback hodnot, pořadí, fresh child ID, revize a granularní payloady. |
| 2 | ✅ Opakovaná rutina ve slotech, přeřazení, cyklus | P: `savePersistsStableOrderedSlotsRepeatedRoutineAndTypedOutbox`, `saveNormalizesOrderRetainsIdsAndRejectsStaleRevision`, `completionCyclesABCAndOneSlotIncrementsProgressRevision`. Uložené slot IDs zůstávají, ABC se vrací na A. |
| 3 | ✅ Úplný start a souběžný druhý start | W: `concurrentStartsResumeWithoutChangingSnapshotsOrSource`. Dvanáct souběžných startů → jedna session, jeden Created, dvě entry a čtyři série, stejný zdroj. `prescriptionStartRetainsPreviousCompletedPerformance` navíc hlídá předchozí výkon. |
| 4 | ✅ Immutable snímek rozběhnutého i historického workoutu | W: `deletedActualWorkKeepsCompleteOriginalPrescriptionAcrossReopenAndSourceChanges` maže první sérii a druhý výskyt, zavře/otevře file DB, změní/archivuje zdroj i katalog a dokončí. Všechna původní ID, pořadí, hodnoty, pauzy, typy a source child ID zůstávají; JSON/CSV obsahují i vynechané cíle, statistiky a převod jen jednu skutečnou sérii. `omittedAllActualRowsExportAndConvertAsNoPerformance` ověřuje i úplně prázdnou skutečnost. Skia historie vykreslí všechny čtyři vynechané cíle. |
| 5 | ✅ Správný plán, právě jednou; cancel/manual bez posunu | W: `completionAdvancesExactlyOnceAndCancellationDoesNot` ověřuje dokončení, opakování, cancel a nově i manual completion; manual nemění progress ani jeho outbox řádky. P: `activeSelectionPersistsWithoutOutboxOrResetAndCompletionUsesSourcePlan` ověřuje původní plán po změně výběru. |
| 6 | ✅ Skip/edit během workoutu, jednodení i celý cyklus, review | P: `skipWhileWorkoutRunsRequiresReviewEvenAfterFullWrapOrOneSlot`, `editedSourceAndDifferentCursorAreReviewOutcomesNotErrors`, `removingCursorAtomicallyRequiresExplicitConfirmation`. W: `changedPlanCommitsCompletedWorkoutWithReviewAndArchivedPlanStaysUnchanged`. Kontrola skutečných progressRevision, needsReview, blokovaného startu a explicitního confirm. |
| 7 | ✅ Atomické rollbacky včetně outboxu | R: `failedThirdSetRollsBackExistingAggregateRevisionAndAllOutbox`, `failedOutboxRollsBackRoutineChildrenAndPreviousOutboxInserts`, `orderedDraftPublishDeleteFailureRollsBackAndSuccessCannotResurrect`; P: `failedChildInsertRollsBackDefinitionChildrenProgressAndOutbox`, `failedProgressOutboxRollsBackEntireSaveIncludingInsideSuccessfulOuterCommand`; W: `failedThirdSetRollsBackSessionEntriesSetsAndOutbox`, `progressFailureAndOutboxFailureAbortEarlierCompletionWrites`, `snapshotOutboxFailureAndWholeSessionCancellationAreAtomic`. Nové snapshot INSERT/outbox a cancel DELETE/outbox mají stejný rollback; po úspěšném cancel nezůstávají osiřelé cíle. SQL triggery/selhání outboxu vracejí i dřívější zápisy. |
| 8 | ✅ Přijatý příkaz přežije čekající VM, durable restart | WP: `acceptedCommandCommitsAfterCallerIsCancelled`; R: `draftSurvivesReopenAcceptsIncompleteRoutineAndNeverEnqueues`; P: znovuotevření file DB v activeSelection testu. [OrderedDraftsTest](../../core/storage/src/commonTest/kotlin/core/storage/OrderedDraftsTest.kt) hlídá FIFO/cancel/no-resurrection. Dřívější izolovaný Android force-stop/relaunch obnovil raw draft. Nové delayed-navigation testy níže ověřují commit během přepnutí záložky. Procesní kill před commitem není příslib dokončení. |
| 9 | ✅ CAS a ochrana souběžných editorů | R: `staleAndMissingExpectedRevisionsNeverOverwriteOrEnqueue`; P: `saveNormalizesOrderRetainsIdsAndRejectsStaleRevision` — skutečný nezměněný storage/outbox při konfliktu. RP/PP: konflikt, Reload/SaveCopy; [RetainedEditorLeaseTest](../../composeApp/src/desktopTest/kotlin/app/navigation/RetainedEditorLeaseTest.kt) reálně ověřuje druhý odmítnutý editor, návrat původního, publish/discard a novou lease. |
| 10 | ✅ Archivované/chybějící reference, viditelná chyba | R: `catalogueArchiveBlocksNewEntriesButExistingEntryCanBeRetainedForRepair`, `invalidAndMissingReferencesReturnTypedErrorsWithoutRows`; P: `archivedRoutineBlocksStartAndNewReferencesButPreservedSlotCanBeRepaired`, `missingAndValidationErrorsRemainTypedWithoutWrites`. RP/PP: library read failure není empty success; živé create/rename/archive nabídky zachovají raw draft i lease, počáteční chyba má RetryChoices a selhání/obnova neodemknou čekající Save. Obrazovky zobrazují archivaci/chybějící reference a opravu; migrační test ověřuje explicitní repair. |
| 11 | ✅ Skutečná migrace 6 → 7 | [WorkoutMigrationTest](../../composeApp/src/desktopTest/kotlin/app/workflow/WorkoutMigrationTest.kt): DB vytvořená přes přesné SQL závazného 6.json, otevřená produkční Room migrací s validací v7. Zachována ID/historie/mutace včetně byte-for-byte payloadu; opakovaný cvik, invalid/oversized scalars, archived placeholders; unknown prescription/source zůstává null a nová tabulka původních cílů prázdná. |
| 12 | ✅ Skutečné hranice a platformy | Osm vyřešených Gradle compile graphů bez cizího impl; negativní kompilační probe routines → trainingplans impl skončil `Unresolved reference`. Všechny moduly po závěrečných opravách: 198 desktop / 124 Android unit testů; APK + iOS SimulatorArm64 compile úspěšné. |

## 🔧 Závěrečná oprava I1 / I2 / M1

🔄 **I1:** `liveChoicesAndRetryPreserveRawDraftAndLease` v obou editorech nejprve selhal na starém seznamu. Samostatný dlouhodobý observer teď obnovuje `choices`, jeho chyba žije v `choicesError` a `RetryChoices` neotevírá draft ani lease. Tři testy na každý editor pokrývají novou/archivovanou/přejmenovanou volbu, nevalidní raw obsah, původní revizi a ID, druhého odmítnutého vlastníka, počáteční selhání i obnovu při čekajícím terminal Save.

🧭 `retainedPlanReceivesRoutineCreatedThroughAnotherTab` přes skutečný AppNavHost/Root/Room nejprve selhal na disabled pickeru: otevřít nový plán, napsat název, vytvořit rutinu v jiné záložce, vrátit se a vybrat ji. Nyní celý průchod včetně publikace plánu prochází. [Výsledek v uchovaném plánu](evidence/2026-10-28-routines-trainingplans/final-fixes/retained-plan-live-choice.png) byl vizuálně prohlédnut.

📋 **I2:** První real SQLite test selhal `expected:4 but was:0` pro export původních cílů po odebrání skutečnosti. Insert-only tabulka `workout_prescriptions` teď uchovává úplný původní předpis nezávisle na skutečných řádcích i zdrojové rutině. Pokus vložit stejné ID s jinými hodnotami selže; žádné update API není. Výstupy historie a JSON/CSV zahrnují vynechané cíle, ale actual počty/objem/PR/převod zůstávají nad skutečnými řádky. `historyRendersOriginalTargetsWhenAllActualOccurrencesWereRemoved` vykreslí dva původní výskyty téhož cviku a všechny čtyři cíle, i když je skutečnost úplně prázdná. [Historie vynechaného předpisu](evidence/2026-10-28-routines-trainingplans/final-fixes/history-all-targets-omitted.png) byla vizuálně prohlédnuta.

💾 Finalizovaný export schématu7 a produkční MIGRATION_6_7 obsahují novou tabulku a index sessionId. Verze7 ještě nebyla vydaná; žádná migrace mezi provizorními sedmičkami se nezavádí. Produkční migraci validuje skutečná v6 fixture a její původní raw outbox JSON zůstává byte-for-byte. Detail smluv je v [architektuře](../architecture.md).

🧱 **M1:** `AppTabs` přijímá neutrální položky; app vlastní stejné popisky/ikony/pořadí Tréninky–Rutiny–Plány i cíle. Ověřeno existujícími navigačními testy a kompilací, bez testu kopírujícího konstanty.

## 🧭 Opravený nález: dokončení při odchodu

❌ Před opravou oba nové testy v [DelayedEditorNavigationTest](../../composeApp/src/desktopTest/kotlin/app/navigation/DelayedEditorNavigationTest.kt) selhaly. Skutečný AppNavHost/Root/VM/Room: save čeká na řízené bráně, uživatel přepne záložku a commit doběhne, zatímco odcházející editor je ještě v animované kompozici. Jeho neomezený collector zavolal pop/navigate nad cizí aktuální obrazovkou.

✅ [CollectScreenEffects](../../core/presentation/src/commonMain/kotlin/core/viewmodel/CollectScreenEffects.kt) odebírá až v `RESUMED` lifecycle vlastní entry, callback aktualizuje přes `rememberUpdatedState`. Používají jej oba editory, oba library Rooty a všechny tři workout Rooty se stejným původním vzorem. Uložení dále vlastní aplikační fronta. Retained entry obdrží buffered efekt po návratu; skutečný pop vyčistí VM. Přidána pouze existující verze lifecycle-runtime-compose 2.8.3 v core:presentation.

🧪 Dva editorové testy ověřují skutečný commit názvu a smazání draftu při zachování jiné záložky. [DelayedWorkoutNavigationTest](../../composeApp/src/desktopTest/kotlin/app/navigation/DelayedWorkoutNavigationTest.kt) navíc brzdí reálný CompleteWorkout, ověří dokončenou session bez navigace na jiné záložce a po obnovení původní entry návrat právě jednou. Malé statické zdrojové destinations v tomto workout testu nemaskují databázi ani skutečný ActiveWorkoutRoot. Oba plné editorové testy navíc obnoví původní záložku a ověří aktualizovaný detail po doručení čekajícího Saved efektu.

## 🔄 Opravený nález: živý Room odběr po commitu

❌ Native Android po přesném průchodu uchovaným editorem ukázal starý název a nulu dotčených plánů, přestože read-only kopie izolované DB obsahovala nový název/revizi, žádný draft a existující slot. `liveObserverReceivesOuterCommitWithoutReopeningAndNeverSeesRollback` v R reprodukoval timeout existujícího Flow po skutečném commitu.

✅ Pinned Room `2.7.0-alpha11` zdroje ověřeny přímo v lokálním sources JAR: `useWriterConnection` jen získá spojení; vnořené DAO operace vynechají refresh, když stále běží vnější transakce. [RoomCommandCoordinator](../../core/storage/src/commonMain/kotlin/core/storage/RoomCommandCoordinator.kt) proto zavolá veřejné `invalidationTracker.refreshAsync()` po úspěšném **vnějším commitu**, před potvrzením výsledku. Savepoint ani rollback nepublikují mezistav. Tento způsob použití odpovídá [oficiální dokumentaci InvalidationTracker](https://developer.android.com/reference/androidx/room/InvalidationTracker#refreshAsync()). Žádné periodické reloady UI ani upgrade závislostí.

🧪 Nový Flow test prošel RED timeout → GREEN: po nezobrazeném rollbacku uvidí committed rename a archive bez nového subscribe. Rozšířený `LibrarySmokeTest` ověřuje živý počet dotčených plánů 1, blokovaný druhý editor, zachovaný původní obsah, Save a nový název detailu; přesný dříve visící průchod nyní končí. Podrobnosti jsou v [šetření](2026-10-28-renderer-investigation.md).

## 📦 Graf závislostí a schéma

```sh
./gradlew --no-configuration-cache \
  -I docs/reviews/evidence/2026-10-28-routines-trainingplans/verify-feature-boundaries.init.gradle \
  verifyFeatureBoundaries
```

✅ [Kontrola](evidence/2026-10-28-routines-trainingplans/verify-feature-boundaries.init.gradle) čte Gradle `ResolutionResult` všech osmi skutečných `desktopCompileClasspath`, včetně tranzitivních projektů. Každý API/impl má jen vlastní implementaci a potřebná veřejná API. Trainingplans impl vidí routines API; routines API a trainingplans API vidí neutrální workout API. Tvrzení není odvozené z grep zdrojů.

🧪 Dočasný commonMain probe v routines impl:

```kotlin
import features.trainingplans.data.trainingPlanFeatureModule
fun forbiddenModule() = trainingPlanFeatureModule()
```

Kompilátor jej odmítl jako unresolved (`/tmp/task6-forbidden-compile.log`); soubor byl odstraněn před zelenou finální kompilací. Kontrola negarantuje, že nikdo v budoucnu úmyslně nepřidá zakázanou Gradle závislost; uvedený graph-check takovou změnu detekuje.

💾 `git diff 402821a -- composeApp/schemas/app.database.AppDatabase/6.json` je prázdný. Závazná v6 nebyla přepsána. Test ji používá přímo a Room validuje výsledek migrace proti v7.

## 🧪 Příkazy a důkazy

```sh
./gradlew :composeApp:desktopTest --tests '*Delayed*NavigationTest'
./gradlew desktopTest testDebugUnitTest \
  :composeApp:assembleDebug :composeApp:compileKotlinIosSimulatorArm64
```

| Důkaz | Výsledek |
| --- | --- |
| ❌ [RED testy](evidence/2026-10-28-routines-trainingplans/final-fixes/red-tests.txt), [RED tab journey](evidence/2026-10-28-routines-trainingplans/final-fixes/red-ui.txt) | Před opravami: dva stale-choice ViewModel failures, ztracený předpis v SQLite/exportu a disabled nabídka při návratu do plánu |
| ✅ [Finální všechny platformy](evidence/2026-10-28-routines-trainingplans/final-fixes/final-platforms.txt) | 198 desktop / 27 suites + 124 Android unit / 18 suites, 0 failures/errors/skips; APK + iOS compile; 15 s |
| ✅ [Finální vyřešené grafy](evidence/2026-10-28-routines-trainingplans/final-fixes/feature-boundaries.txt) | 8 skutečných feature API/impl grafů |
| ✅ [Strojový souhrn](evidence/2026-10-28-routines-trainingplans/final-fixes/verification.json) | Počty z finálních XML, schema6 hash a rozsah důkazů |
| ❌ `/tmp/task6-delayed-red.log` | 2/2 očekávané behavior failures před lifecycle opravou |
| ✅ `/tmp/task6-delayed-green.log` | 3/3 cílené navigační testy |
| ✅ `/tmp/task6-final.log` (před finální revizí) | 187 desktop testů / 27 suites + 118 Android unit / 18 suites, 0 failures/errors/skips; APK + iOS compile |
| ❌→✅ `/tmp/task6-observer-red.log`, `/tmp/task6-observer-green.log` | skutečný dlouhodobý Flow: timeout před opravou, následně commit/archive bez resubscribe |
| ✅ `/tmp/task6-render-fixed.log` | přesný původní rozšířený průchod poprvé prošel po opravě invalidace |
| ✅ `/tmp/task6-boundaries.log` | 8 skutečných vyřešených feature grafů |
| 🧪 `/tmp/task6-forbidden-compile.log` | očekávaný kompilační neúspěch zakázaného importu |

⚠️ První autorský běh nového testu měl chybný název doménové vlastnosti `sourcePlanId`; opraven na `sourcePlan`, následná úplná sada prošla. Úvodní graph-script zahrnul i prázdné parent projekty; opraven na skutečných osm API/impl modulů. Žádné existující selhání nebylo potichu vyřazeno. Existující AGP/Gradle deprecations a expect/actual Beta warnings zůstávají; žádný toolchain upgrade.

## 📱 Vizuální a runtime evidence

🖼️ Standardní `LibrarySmokeTest` používá skutečné obrazovky, navigaci a Room, vytvoří rutinu/plán, obnoví raw vstup po přepnutí tabů, startuje a dokončí plánový workout a přepne dark theme. Obrázky jsou v `composeApp/build/task5-visual`. [TypedNavigationTest](../../composeApp/src/desktopTest/kotlin/app/navigation/TypedNavigationTest.kt) rekonstruuje skutečný NavHost a entry ownership/argumenty. Testový Skia renderer není totéž jako klikání v nativním desktopovém okně.

✅ Přesný rozšířený scénář přejmenovaného detailu, původní nález i oprava jsou trvale zachovány v [šetření vykreslování](2026-10-28-renderer-investigation.md). Zelený finální smoke tento průchod již obsahuje; samostatnou chybu rendereru po opravě invalidace tato kontrola nereprodukovala.

📱 **Před závěrečnou opravnou vlnou: izolovaný Android runtime PASS na `017866e`:** API37/emulator-5554, applicationId `com.yhdista.contentapp.task6`. Přesný průchod druhým odmítnutým editorem/Retry a návratem do původního návrhu skončil uloženým `NativeFinalRename`, aktualizovaným detailem a počtem dotčených plánů 1. SQL readback potvrzuje revizi2, smazaný draft a existující slot. [Screenshot po opravě](evidence/2026-10-28-routines-trainingplans/android-after.png) byl prohlédnut; podrobný postup a before/after důkazy jsou ve výše odkazovaném šetření. Testovací package byl odinstalován; normální instalace ani uživatelská DB nebyly změněny. Tento nativní důkaz patří k předchozímu commitu017866e. Závěrečná vlna znovu ověřila cílené Skia UI, všechny Android unit testy/APK a iOS compile; nový nativní smoke se neopakoval. Finální APK má normální applicationId.

## 📚 Dokumentace a limity

✅ README, architektura a MVI byly porovnány s aktuálními rozhraními, grafem, nav ownership a exporty; MVI nyní vysvětluje RESUMED efekt a rozdíl mezi původním WorkoutRepository a typovanými Result API. Historický architecture review a mesh příprava jsou označené jako historické. Kontrola lokálních odkazů v pěti aktuálních dokumentech a `git diff --check` prošly; finální počet je uložen ve strojovém souhrnu. Schéma6 SHA-256: `14ee6ac92239c2c115c1e06bdcc84d8a1aa4688af4a32ba41354957c28ccc565`.

⚠️ Reálný cloud uploader, migrace 1–5, úplný backup rutin/plánů/draftů a iOS runtime host nejsou implementované. Android compile není device test; iOS je pouze kompilace. Pinned Navigation alpha používá zdokumentovaný app-only null-args workaround, ověřený host recreation testem. Původní render symptom nebyl odepsán jako problém pouze testovacího harnessu; vedl k potvrzení a opravě skutečné stale-observation chyby. Nativní desktopové klikání ani iOS runtime nebyly ověřeny.
