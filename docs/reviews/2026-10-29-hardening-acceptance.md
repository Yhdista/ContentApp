# 🧱 Akceptace atomic a architektonického hardeningu

📅 29. 10. 2026. Výchozí produkční revize `fb6078d`; opravy navazují na [společný audit](2026-10-29-atomic-architecture-audit.md) a [nezávislý Astra audit](2026-10-29-astra-architecture-audit.md). Řídící postup je `yhdi-dev`, jedna implementační vlna s K po balících a společnou závěrečnou G.

✅ **Implementace a ověření dokončené na produkčním `45e2133`; nezávislá review právě navazují.** Agregace352testů je zelená; po posledním atomic/API zásahu znovu prošlo všech13dotčených UI testů, platformní K, guard/lint/APK. Android native smoke je doložený samostatně.

## 🎯 Audit → změna → důkaz

| Audit | Implementace | Dosavadní důkaz |
| --- | --- | --- |
| 🔐 A1: druhá cesta dokončení obcházela plán | Repository už completion nevystavuje; UI volá `CompleteWorkout`, app vlastní transakci workout + plán | K všech consumers + runtime regrese, [balík1](evidence/2026-10-29-hardening/task-1.md) |
| 🔒 A2: observer odemykal čekající příkaz | Busy uvolňuje pouze vlastník operace, nikoli chybová/úspěšná emise odběru | Pending Finish/Duplicate + observer failure, balík1 |
| 💾 A3: přidání cviku a první série byly dva commity | `addExerciseWithInitialSet` přijímá jeden příkaz pro entry + sérii + oba outbox řádky | Skutečný rollback druhého outbox zápisu a cancellation, balík1 |
| 🕒 A4: předchozí výkon podle UUID | Pořadí podle času dokončené session a pořadí cviku/série; aktivní session se vyloučí | Opačné ID, aktivní session, opakované zařazení cviku, balík1 |
| 🧩 A5: skrytá veřejná dependency | `StatusBadge` přijímá UI label/tone; modul nemá foundation dependency | Samostatný consumer reprodukoval RED a po změně kompiluje, [balík3](evidence/2026-10-29-hardening/task-3.md) |
| 🎨 A6: nejednotné atomic vrstvy | Sdílené atoms/molecules/organisms/templates, Material adaptéry v nucleus; workout používá společné prvky | K všech consumers +13UI +17guardfixtures PASS; nucleus nemá zpětné reference |
| 🔌 A7: příliš úzké UI API | Root Modifier, obsahové sloty, varianty akcí, vstupní konfigurace, neutrální tabs/dialog | Public consumer a UI interakce, balík3 |
| ♿ A8: kontrast/sémantika/velké písmo | Textové tokeny, checked label série, min. plochy, pružné formuláře a záložky | Čtyři cílené testy + screenshoty 320dp/font2 obou témat; následná přesná kontrola textových bounds |
| ♻️ A9: duplikovaný editorový protokol | `DurableEditorController` kompozicí; `Session.publish` vlastní agregát/outbox/delete draft | Čtyři controller regrese + K retained/nav consumers, [balík2](evidence/2026-10-29-hardening/task-2.md) |
| 🏋️ A10: nejasný katalog a fitness ve foundation | Exercises vlastní create/update/archive/outbox; seed/import oddělen; obecné chyby a UiText přemístěny | Čtyři reálné SQLite katalogové testy, zachované seed IDs a JSON payloady, balík2 |
| 📦 A11: stará matice knihoven | Stabilní Kotlin2.4.21/CMP1.12.2/Navigation2.10.0, nový Android KMP plugin a samostatný host | K všech targets + reálná migrace6→7 a nav recreation bez helperu PASS; [matice](../build-toolchain.md), G +cílené opravy PASS |
| 🛠️ A12: ruční architektonická pravidla | Tři malé konvence, skutečný Gradle dependency inventory, source guard a pozitivní/negativní fixtures, Linux/macOS CI | Konfigurace/K +skutečný inventory/17fixtures/lint PASS |
| 🔇 A13: nepoužitý HTTP stub logoval všechno | `HttpClientFactory` a nepotřebné Ktor dependencies odstraněny | Všichni consumers kompilují bez Ktor; potřebná Json dependency testů je explicitní |

## 🧪 Co už prošlo

- ✅ Balík1 `0a8a6d9`: šest RED regresí před opravou, 17 přesných GREEN L1 metod. K common metadata + desktop/Android production/test consumers PASS. Historický audit se nepřepisuje.
- ✅ Balík2 `32a5a78`: K všech 13 modulů, 406 tasks; osm cílených testů controlleru/katalogu bez chyb. Nahrazené testy zrušeného workout katalogového API mají skutečné SQLite náhradní pokrytí.
- ✅ Balík3 `494fd12`: na Air 411 skutečně provedených kompilačních tasks (345s), čtyři L1 testy PASS (10s). Veřejný consumer nepřidává Material/domain/storage dependency.
- ✅ Následná vizuální oprava `7aba0e6`: K PASS; velké navigační popisky jsou na celých řádcích. Finální test `29efd04` měří hranice skutečně vykreslených řádků, velikost voleb a callbacky; PASS5s. Původní overflow příznak starého Compose porovnával odstavec omezený na208px s intrinsic textovým prvkem112px, přestože znaky zabíraly111,95px; nejde o povolení ořezaného textu.
- 📦 Milník `494fd12`: Android debug APK na Air PASS42s, 262 tasks (85 executed). Uživatel dostal lokální artefakt před G; běžná instalace ani databáze nebyly měněny.

🖼️ [Komponenty světlé](evidence/2026-10-29-hardening/ui-before-toolchain/controls-light-320-font2.png), [tmavé](evidence/2026-10-29-hardening/ui-before-toolchain/controls-dark-320-font2.png), [série velké písmo](evidence/2026-10-29-hardening/ui-before-toolchain/set-row-light-320-font2.png), [kompaktní série](evidence/2026-10-29-hardening/ui-before-toolchain/set-row-compact-320-font1.png) byly vizuálně prohlédnuté. Jsou to důkazy balíku3 před modernizací toolchainu; konečná sada bude uvedena samostatně. Souhrn revizí: [JSON evidence](evidence/2026-10-29-hardening/task3-verification.json).

## 🚦 Společná G a opravy

První celá G na `caea2ad` provedla desktop/Android host testy, architektonické kontroly a fixtures, veřejného consumer, shared/app lint, Android APK a iOS simulator compile. Trvala260s,631tasks (380executed/251up-to-date); skončila červeně na čtyřech tasks. Finální agregace po cílených opravách: **220desktop/32suites +132Android host/unit/20suites,0fail/error/skip**; [strojové výsledky a revize](evidence/2026-10-29-hardening/G-green-9cbdace.json). Není to tvrzení, že všechny tasks běžely znovu na jediné revizi.

- ✅ `c858837`: scanner nyní rozlišuje skutečné root `platform.*` od legitimního `androidx.compose.ui.platform`/`core.platform`; přidaná pozitivní fixture zachovává negativní případy. Foundation deklaruje VIBRATE pro vlastní Android feedback adapter, nepoužitý INTERNET odstraněn z hostu. `verifyArchitecture testArchitectureGuard :foundation:lintAndroidMain :androidApp:lintDebug :androidApp:assembleDebug` PASS29s,417tasks;13fixture metod.
- ✅ Ostatní moduly:135desktop testů ve21suites +132Android host/unit testů ve20suites bezfail/error/skip. `composeApp` se do tohoto čísla nezapočítává, jeho celý desktop module se po opravách spouští znovu.
- 🧪 První UI zásek byl neomezené čekání na idle při cleanup. Fixture nyní má30sdeadline, společné zachování původní výjimky a zavírá externí zdroje až po disposal scény.
- 🔎 Celý běh pak odhalil odlišný problém: Koin4.2.2 default Compose local držel první globální instanci přes `stopKoin/startKoin`; druhý test selhal už při úvodní kompozici. Výslovný `KoinIsolatedContext` vlastní instance (`347d9f5`) odstranil všechny ClosedScope chyby. Assertions zachovány. Dvě zbývající volby cviku byly prázdné kvůli fixture, která obcházela produkční `ExerciseCatalogImport.ensureDefaults()`. `9cbdace` volá stejný bootstrap, ověřuje skutečně uložený aktivní Benchpress a čeká na enabled picker. Celý `:composeApp:desktopTest` PASS7s,85testů/11suites,0fail/error/skip.

✅ **Poslední A6 `45e2133`: PASS85s,753tasks (219executed/534up-to-date).** Znovu ověřeny common metadata, desktop/Android production i test consumers, Android host main/unit/instrumented source compile, iOS SimulatorArm64,9aplikačních navigačních +4komponentové UI testy (13/13),17guardfixtures, skutečný Gradleinventory, veřejný consumer, KMP+hostlint a běžné APK. [Přesný příkaz](evidence/2026-10-29-hardening/G-a6-command.sh), [výsledky](evidence/2026-10-29-hardening/G-a6.json), [gw souhrn](evidence/2026-10-29-hardening/G-a6.log), [příčiny a opravy](evidence/2026-10-29-hardening/G-fixes.md).

🧹 Lint:0errors,0ModifierParameter warnings (před opravou14unikátních míst). Zbývá18reportovaných warnings včetně opakování z dependencies: starší target35, novější dostupný AGP mimo zvolenou ověřenou matici, nepoužité launcher/template resources a dvě drobnosti staršího kódu. Nejde o nulový-warning výsledek. Nevznikl lint suppression ani nová baseline.

## 📱 Native smoke a aktuální vizuální důkazy

✅ [Android smoke report](evidence/2026-10-29-hardening/native-android/README.md): nový host, tři tabs, vytvoření návrhu a cviku, systémové Zpět, obnovení po ukončení procesu, publikace rutiny, startworkout, dokončení série a workoutu, návrat ke zdroji a historie. Oddělený QA package, původní data nedotčena.

🖼️ Na finálním `45e2133` znovu prošly komponentové testy a root vizuálně prohlédl všech5 nově vytvořených snímků: [světlé komponenty](evidence/2026-10-29-hardening/ui-modern/controls-light-320-font2.png), [tmavé komponenty](evidence/2026-10-29-hardening/ui-modern/controls-dark-320-font2.png), [světlá série font2](evidence/2026-10-29-hardening/ui-modern/set-row-light-320-font2.png), [tmavá série font2](evidence/2026-10-29-hardening/ui-modern/set-row-dark-320-font2.png), [kompaktní série font1](evidence/2026-10-29-hardening/ui-modern/set-row-compact-320-font1.png). Jde o skutečné captures, nikoli golden image test.

## 📦 Modernizace a zachované kontrakty

✅ Integrovaný balík4–5 `caea2ad`: K zahrnuje common metadata, desktop production/test, Android main/host test, Android host main/unit/instrumented compile, veřejný design consumer a iOS SimulatorArm64. Poslední K `cb375e0` PASS9s (434 tasks,36 executed/398 up-to-date) navazuje na cold kompilace; nejde o434 znovu provedených tasks. Následující finalcommit měnil CI/dokumentaci/guard fixture, produkční zdroje nezměnil.

✅ Přesné L1: `WorkoutMigrationTest.actualSchemaSixMigratesLosslesslyAndRoomValidatesSchemaSeven` a `TypedNavigationTest.entryOwnershipRestorationAndWorkoutReturnsToSource`, dva testy bez fail/error/skip,7s. Pomocný nav helper byl odstraněn z produkce i testových grafů, assertions obnovy zůstaly. Explicitní Room invalidace je zachovaná.

🧰 Při modernizaci se opravily skutečné rozdíly API: Compose Preview namespace, standardní `kotlin.use` pro SQLite AutoCloseable a explicitní Json dependency desktop testů po odebrání Ktor. Nezměnilo se SQL ani schéma. Nový KMP plugin má zapojený `com.android.lint`; CI kontroluje shared `lintAndroidMain` i app `lintDebug` s dependencies.

📱 Android host vlastní jediný launcher/Application/resources; `composeApp` je sdílená KMP knihovna s desktop vstupem. Compile SDK37, min27 a target35 zachovány. CI explicitně zajišťuje JDK17; lokální/Air Gradle21 s compile toolchain17 také prošel. CI konfigurace sama není důkaz spuštění GitHub Actions.

## 💻 Mesh a reprodukovatelnost

💻 Podle pokynu uživatele běžela příprava na obou dalších počítačích: Air řešil migraci toolchainu, Windows konkrétní atomic UI a architektonické kontroly. Obě kopie i pracovní worktrees měly vlastní ověřený CodeGraph před workerem. Windows worker měl omezené shell čtení konfigurace; jeho podklad není vydáván za nezávislou validaci buildů. Root ověřil skutečné konfigurace a dostupnost kandidátních Maven artefaktů přímo.

🔧 Air následně skutečně zkompiloval a otestoval balík3. Cold build zahrnoval stahování dependencies. Zpoždění kvůli cizímu místnímu gate nebylo počítáno jako běh testů; žádný cizí daemon nebyl zastaven. Na každém stroji jediný build, přes `yhdi-dev/scripts/gw`, aktuální profil 2g Gradle / 1g Kotlin / 1 worker.

## 📱 Platformní rozhodnutí a hranice

📱 Uživatel výslovně zvolil moderní stack: Android, desktop JVM, iOS zařízení `iosArm64` a simulátor na Apple Silicon `iosSimulatorArm64`. `iosX64` byl odstraněn; nové stabilní Navigation/Lifecycle/SQLite tento cíl nepublikují. Samostatný iOS host/framework export není součástí této změny; iOS compile není runtime test.

💾 Historická Room schémata6/7 a raw draft JSON verze1 se zachovávají. Skutečný cloud sync, migrace1–5, plná záloha/obnova aplikace, auth, kalendář a notifikace zůstávají mimo rozsah. POC má být udržitelný základ, nikoli tvrzení o připravenosti libovolné budoucí aplikace k produkčnímu vydání.
