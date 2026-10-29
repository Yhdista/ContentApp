# 🛠️📦 Balík4–5 — konvence, hranice a moderní KMP

📅 29.10.2026. Implementer `/root/atomic_build`; integrátor root. FÁZE=IMPLEMENTACE,
společná hardening vlna, yhdi-dev K/L1/G. BASE `29efd04`.

**Předání:** branch `refactor/atomic-build`, finální commit
`caea2ad51db8d96834f8837cfedb647caf685e75`, worktree
`/Users/yhdista/AndroidApps/ContentApp-worktrees/atomic-build`. Lokální tracked
strom čistý. Air kopie `/Users/yhdista/.yhdi-mesh/worktrees/contentapp-atomic-build`
má stejný HEAD a synchronizovaný vlastní CodeGraph; jeho `.codegraph/` je
očekávaný lokální untracked nástroj. Žádný push/main merge ani cizí proces kill.

## ✅ Co se změnilo

- Malé precompiled Groovy convention plugins `contentapp.multiplatform`,
  `contentapp.compose`, `contentapp.desktop-consumer` sdílejí cíle, SDK,
  toolchain/bytecode17, commonTest závislosti, host test enablement a jeden test
  fork. Groovy nezavádí druhou verzi Kotlin compileru pro build-logic. Namespace
  a veřejné `api`/soukromé `implementation` zůstávají explicitní v modulech.
- Všech13produkčních KMP modulů používá nový Android KMP library plugin;
  `android.builtInKotlin=false` a `android.newDsl=false` odstraněny.
  `withHostTest {}` zachovává commonTest na Androidu; nové tasky jsou
  `compileAndroidMain`, `compileAndroidHostTest`, `testAndroidHostTest`.
- Jediný Android host je `androidApp`: přesunuté nezměněné Activity/Application,
  app manifest s plně kvalifikovanými entry classes, kompletní původní
  `src/main/res`, example unit test a skutečný app-context instrumented test.
  Package/applicationId `com.yhdista.contentapp`, min27/target35/compile37
  zachovány. Shared `composeApp` namespace je `com.yhdista.shared`; platformní
  Room DI adaptér zůstává v shared `androidMain`, initKoin a AppNavHost veřejné.
- Verze centralizované, hardcoded Lifecycle2.8.3 odstraněn. Explicitní catalog
  Compose souřadnice nahrazují deprecated dependency aliases; jediná Material3
  dependency je stabilní1.9.0 v designsystem. Native desktop runtime se vybírá
  podle OS/arch hosta. Tooling je jen debug dependency Android hostu.
- Uživatelem výslovně zvolený scope Android/desktop/iosArm64/iosSimulatorArm64;
  iosX64 odstraněn ze všech dříve podporujících modulů a KSP konfigurací.
  Consumer byl a zůstává desktop only. KSP zachován pro Android/desktop/obaARM.
- `verifyArchitecture` napojen na root `check`, module `check` a CI. Gradle
  generuje skutečné declared dependency inventory, takže catalog alias nebo
  multiline syntax nemůže obcházet dependency hranici. Pythonstdlib scanner
  mapuje importy na fyzického vlastníka, včetně falešného `api` package v impl.
  Hlídá foreign impl, foundation/framework/fitness, DS/domain/storage,
  commonMain platform reference, Material3 pouze nucleus a úzké Screen/Root/VM
  storage/workflow vlastnictví. DI/controller factories a generic controller
  jsou legitimní bridges, nikoli zákaz celé presentation vrstvy.
- 12 pozitivních/negativních fixture metod mimo produkt, včetně alias/fullyqualified
  Material, multiline dependencies, neutrálně pojmenované storage package,
  komentářů/raw strings a UI publish sequencing. **Pouze syntax compile ověřen;
  spuštění guardů a fixtures náleží G.** Senzor není Kotlin parser/dataflow/API
  review; omezení jsou výslovně v tools/architecture/README.md.
- CI explicitně instaluje JDK17, SDK `platforms;android-37.0` a build-tools37.0.0,
  Linux Xvfb/Skia/font/X11 předpoklady; spouští explicitní desktop/Android host
  testy, design consumer, architecture+fixtures, debug APK a instrumented source
  compile. AGP9.3.1 KMP main lint vyžaduje `com.android.lint`; convention jej
  zapíná, skutečné `lintAndroidMain` ověřeno task inventářem. Host lintDebug má
  `checkDependencies=true`. Mac job kompiluje Apple Silicon iOS simulator.
  **CI samotné ještě nebylo spuštěno.** Žádný secret/deploy ani native allTests.
- Nepoužitý HttpClientFactory s LogLevel.ALL a všechny Ktor dependencies/aliasy
  odstraněny. JSON test fixtures mají vlastní explicitní desktopTest dependency,
  nepoužívají náhodný dřívější Ktor transitivní přístup. Napier pro obecné chyby
  zachován; žádný nový backend ani payload logging.
- Nový guide `docs/build-toolchain.md`; root-owned README/architecture/mvi/atomic
  docs, MERGE-STATE a .gitignore nezměněny. Historické audit probes netknuty.

## 📦 Verze a kompatibilita

| Součást | Výsledek |
|---|---|
| Kotlin/compiler |2.4.21 |
| CMP / explicitní Material3 |1.12.2 / stabilní1.9.0 |
| Navigation / Lifecycle KMP |2.10.0 /2.11.0 |
| Room runtime+compiler / SQLite bundled |2.8.5 /2.7.1 |
| Koin / KSP |4.2.2 /2.3.12 |
| AGP / wrapper |9.3.1 /9.5.0, ponecháno v Kotlin published matrix |
| Coroutines / Serialization |1.11.0 /1.12.0 |
| Activity Compose / CoreKtx / Views Material / Napier |1.13.0 /1.19.1 /1.14.0 /2.7.1 |
| JVM |compile toolchain17 + bytecode17; CI explicit17; doporučen místní Gradle21 |

Root metadata/POM/module evidence v toolchain-target-matrix a JSON podkladech
ověřena před implementací. Implementer navíc přímo stáhl oficiální supporting
POMy: Coroutines1.11.0 stojí na Kotlin2.2.20, Serialization1.12.0 na Kotlin2.4.10,
Activity1.13/Core1.19.1 na Kotlin2.1.20; splňuje vybraný2.4.21. Compose explicit
runtime/ui/preview/test/desktop artefakty1.12.2 HTTP200. Oficiální Google SDK
repository2-3.xml potvrzuje přesný balík android-37.0 a build-tools37.0.0.

AGP9.5 je novější než zvolená9.3.1, ale mimo rootem ověřenou Kotlin matrix.
Material3 má vlastní stable řadu; implicitní compose.material3 by vybíral alpha.
Air skutečný Gradle daemon použil nativní Homebrew OpenJDK21.0.12.1, kompilátor
toolchain nativní17; žádná globální instalace/změna. Xcode27.1 je mimo publikovanou
Kotlin Xcode26.4 matrix: **skutečný iOS simulator compile prošel**, nikoli tvrzení
oficiálně testované kombinace27.1 ani iOS runtime.

Kompilační migrace: šest Preview importů na androidx.compose.ui.tooling.preview;
šest SQLite.use importů na standardní kotlin.use. SQLite2.7.1 oficiální zdroje
potvrzují AutoCloseable u Connection/Statement. SQL a testové assertions bezezměn.

## 🧪 K — přesný recept a důkazy

Pouze Air, protože místní stroj obsadil cizí HEVY_CLONE. Žádný lokální build
nebyl zařazen do fronty. Rozpočet Air2gGradle/1gKotlin/1worker/1fork; předflight
root healthok, DEFER0,5GiB available, load1.22, gate unlocked. Všechen Gradle přes
`/Users/yhdista/ai-toolbox/plugins/yhdi-dev/skills/yhdi-dev/scripts/gw` přes mesh.

```text
compileCommonMainKotlinMetadata compileKotlinDesktop compileTestKotlinDesktop
compileAndroidMain compileAndroidHostTest
:androidApp:compileDebugKotlin :androidApp:compileDebugUnitTestKotlin
:androidApp:compileDebugAndroidTestKotlin
:composeApp:compileKotlinIosSimulatorArm64
```

Root selectors pokrývají všech13produkčních modulů i public DS consumer
(metadata/desktop). commonTest je kompilován desktop i Android host targetem;
žádný test task nebyl použit jako compile zkratka. Changed native target má
skutečný simulator compile bez linkování.

| Run / log basename | Exit | gw čas | Výsledek |
|---|---:|---:|---|
|002207Z-99294 |1 |36s |config: předčasný desktop runtime configuration lookup; opraven lazy sourceSets |
|002301Z-99432 |0 |8s |config+actual task inventory |
|002346Z-99686 |1 |91s |K1: starý Preview import; opraven |
|002541Z-99893 |1 |103s |K2--continue:427tasks,422executed; pouze SQLite.use import app4targets; opraven |
|002757Z-219 |1 |14s |K3--continue:434tasks,50executed; production všechny platformy green, desktopTest JSON chybí; opraven |
|002842Z-368 |0 |9s |**K GREEN cb375e0**,434tasks,36executed/398uptodate |

Finální caea2ad proti cb375e0 mění pouze CI prostředí, guide a Python scanner
fixture/ownership senzor. Produkční KMP/Android/Kotlin test strom shodný.
Syntax obou Python souborů `python3 -m py_compile` a `git diff --check` prošly.

## 🧪 L1 — pouze dvě jmenované metody

Invarianty návazné na G: lossless schema6→7 + Room validace7 a typed navigation
entry ownership/recreation/return. Integrátor výslovně doplnil bounded pokus
odstranit withRestorableEntries a dokázat stejnou restore regresi bez helperu.

```text
:composeApp:desktopTest
--tests app.workflow.WorkoutMigrationTest.actualSchemaSixMigratesLosslesslyAndRoomValidatesSchemaSeven
--tests app.navigation.TypedNavigationTest.entryOwnershipRestorationAndWorkoutReturnsToSource
```

Log `gw-20261029T002912Z-523.log`: **exit0,7s**,74tasks,23executed/51uptodate.
XML: přesně2testy,0failures,0errors,0skipped. Navigation0.897s,
migration0.189s (XML test case time, nikoli celý build).

Nav helper odstraněn z production AppNavHost a3testových graph callsites;
smazán jediný RestorableGraph.kt. Žádný test smazán ani assertion oslabena.
Stejná TypedNavigation regrese prošla bez dekorace; odstranění ponecháno.
**Room refreshAsync zůstává**, outer commit/rollback test nebyl nad rámec
briefu spouštěn. Historické UI/retained/editor testy zůstávají pro plnou G.

## 🔐 Schémata a data

Po novém Room/KSP generování na všech K platformách i po L1 byteidentical:

```text
6.json 14ee6ac92239c2c115c1e06bdcc84d8a1aa4688af4a32ba41354957c28ccc565
7.json 2823cffd3dd4249e9b77bfe020908bffca86215a9abbf8f68377650ac1234c31
```

AppDatabase.version7, MIGRATION_6_7 SQL, immutable prescriptions, actual-only
chronology, CAS/raw JSON drafts version1, lease a app FIFO writer nezměněny.
Není nová schema verze ani destructive fallback; skutečná migration L1 prošla.

## ⏱️ Časy a předání G

Převzetí přibližně01:18CET (dispatch), dokončení01:31CET; přesný první gw start
01:22:07CET. K včetně red compile iterací217s, config44s, L1 wrapper7s;
celkem268s Gradle příkazů. Nejde o čistý testový čas. Doložená dlouhá gate
fronta0s: místní cizí práci jsme obešli Air offloadem, nečekali lokální lock.
Krátký status/lock/mesh overhead není samostatně rozlišen v gw souhrnu.
Jednotka pod20min, čisté runtime testy pouze dvě povolené metody.

Evidence stažena do root `.yhdista/task-45-evidence/yhdista-logs/` a
`.yhdista/task-45-evidence/desktopTest/` (JUnit XML i binární výstupy).
Air gate předán rootu, žádný další worker build naplánován.

➡️ G integrátora: celé desktop/UI a Android host/unit sady, architecture scanner
+12fixture metod, design consumer, `lintAndroidMain :androidApp:lintDebug`, APK,
native/device smoke/screenshots podle dostupnosti, iOS simulator compile na
finálním integrovaném stromu, schema/link/diff kontrola a celé architecture review.
Android device test byl pouze kompilován; device runtime, iOS device compile/link
a iOS runtime, skutečný GitHub Actions běh nebyly v tomto balíku provedeny.

Oficiální zdroje: Android KMP migration guide a JetBrains agent migration skill;
neaplikovatelný lokální agp-9-upgrade skill nebyl použit. Agent použil
yhdi-dev, KMP Compose Architect a yhdi-mesh. Žádní další subagenti/review ani plné
testové/statické sady v implementační jednotce.
