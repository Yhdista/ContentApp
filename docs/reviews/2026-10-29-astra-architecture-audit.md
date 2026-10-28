# 🔍 Nezávislý Astra audit architektury ContentApp

**Datum:** 29. 10. 2026 · **Revidovaný stav:** `fb6078d`, celý strom, nikoli pouze poslední diff. **Záběr tohoto reportu:** ne-UI architektura, moduly, vlastnictví domény, perzistence, workflow, MVI, DI, platformy a udržovatelnost základu. Atomic UI/designsystem audit řeší [společný hlavní report](2026-10-29-atomic-architecture-audit.md).

## 🎯 Verdikt

ContentApp má použitelnou architektonickou kostru pro další obsahovou aplikaci, ale není ještě hotovým základem, který lze bezpečně kopírovat beze změn. API/impl graf, lokální SSOT a společný transakční writer jsou skutečné funkční mechanismy. Největší problém není nedostatek Gradle modulů: některé veřejné kontrakty stále dovolují obejít doménový invariant a několik uživatelských operací není zapouzdřeno jako jeden celek. Kritické řízení editorových draftů je navíc dvakrát zkopírované.

Čtyři konkrétní problémy jsou níže potvrzené spustitelným probe. Žádný nález P1 není z těchto důkazů odůvodněný. P2 znamená opravit při nejbližším zpevnění základu; P3 je přiměřený následný refactoring nebo podmíněné rozšíření.

## 🧪 Jak byl audit ověřen

- Před čtením kódu byl použit existující CodeGraph. Byly přečteny aktuální `docs/architecture.md`, `docs/mvi.md`, schválený návrh rutin/plánů, Gradle graf a zdroje všech hlavních ne-UI subsystémů.
- Probe používá **již zkompilované produkční desktop třídy**, skutečnou in-memory Room databázi a Bundled SQLite. Kompilace probe použila lokálně dostupný Kotlin compiler 2.2.10; nic se neinstalovalo.
- MVI probe používá skutečný `ActiveWorkoutViewModel`, řízený test dispatcher a vložené Flow/CompleteWorkout rozhraní. Nepředstírá runtime Android/iOS test.
- Probe a log: [zdroj reprodukce](evidence/2026-10-29-atomic-architecture/ContentAppArchitectureProbe.kt), [runtime výsledky](evidence/2026-10-29-atomic-architecture/astra-runtime-probes.log). Pomocný Gradle task pouze vypsal desktop test classpath. První pokus vyřešit classpath v `projectsEvaluated` selhal na Gradle exclusive-lock pravidle; opravený `doLast` task uspěl. To není chyba projektu.
- Produkční zdroje projektu nebyly upraveny. Probes jsou přiložené jako důkazy auditu, nejsou zařazené do Gradle testovacích sad. Kompletní build/test gate prověřoval hlavní reviewer; tento audit jej souběžně neopakoval.

## 🚩 Potvrzené nálezy

### 1. P2 · Veřejné dokončení workoutu obchází posun plánu

**Typ:** porušitelný invariant veřejného interface; **jistota:** vysoká, skutečný Room probe.

📍 `feature/workout/api/src/commonMain/kotlin/features/workout/domain/WorkoutRepository.kt:37`; `feature/workout/impl/src/commonMain/kotlin/features/workout/data/OfflineFirstWorkoutRepository.kt:343`; `feature/workout/impl/src/commonMain/kotlin/features/workout/data/RoomWorkoutStore.kt:54`; `composeApp/src/commonMain/kotlin/app/workflow/TrainingWorkflow.kt:28`.

Veřejný `WorkoutRepository.completeWorkout` dokončí session a zapíše outbox bez `recordCompleted` plánu. Správný `TrainingWorkflow.complete` posouvá plán pouze při `firstCompletion=true`. Jakmile někdo použije první veřejnou cestu, pozdější zavolání správného workflow již chybu nedožene.

**Prokázaný scénář:** plán A→B, start z A, `WorkoutRepository.completeWorkout(id)`, potom `TrainingWorkflow.complete(id)`. Session je dokončená, druhý příkaz hlásí `firstCompletion=false`, plán zůstává A se stejnou progressRevision. Současný ActiveWorkoutRoot/ViewModel používá správnou integrační akci; netvrdím, že běžné dnešní tlačítko vždy selhává. Problém je, že veřejné rozhraní nabízené dalším feature umožňuje porušit pravidlo bez varování a nepůjde jej zpětně automaticky napravit.

🛠️ Odstranit legacy dokončení z běžného `WorkoutRepository`; jediným aplikačním vstupem bude `CompleteWorkout`. Nízkou `completeOnce` ponechat pouze jako jednoznačně označeného účastníka workflow. Sjednotit duplicitu algoritmu a COMPLETE payloadu: dnes legacy zapisuje `CompleteSessionSyncPayload`, nová větev serializuje celou session. Doplnit integrační test, který hlídá jedinou veřejnou podporovanou cestu dokončení.

### 2. P2 · Přidání cviku s výchozí sérií jsou dva commity

**Typ:** atomicita a životnost příkazu; **jistota:** vysoká, skutečný Room probe.

📍 `feature/workout/impl/src/commonMain/kotlin/features/workout/active/ActiveWorkoutViewModel.kt:118`; `feature/workout/impl/src/commonMain/kotlin/features/workout/data/OfflineFirstWorkoutRepository.kt:143`; tentýž soubor `:157`.

`addExercise` nejprve čeká na `addExerciseEntry`, potom volá `addSetToEntry`. Každá metoda má vlastní aplikačně vlastněnou transakci. Zánik ViewModelu mezi přijetím příkazů nebo selhání druhého outbox insertu tak zanechá první část uloženou. ApplicationScope chrání přijatý jednotlivý příkaz, nikoli toto složené UI workflow.

**Prokázaný scénář:** druhý `workout_set` outbox insert je odmítnut. Zůstává 1 workout entry a 0 sérií. Opakování celé uživatelské akce může založit další entry. Nejde o rollback bug `RoomCommandCoordinator`; jeho dvě samostatné transakce fungují správně, špatně je zvolená velikost příkazu.

🛠️ Vlastnit `addExerciseWithInitialSet` v workout data/domain rozhraní a obě změny provést v jediném vnějším `write`. ViewModel zavolá jeden příkaz. Ověřit selhání druhého zápisu i zrušení čekajícího ViewModelu. U vytvoření vlastního katalogového cviku výslovně rozhodnout, zda je samostatné uložení katalogu záměrné, nebo rovněž část jednoho uživatelského příkazu.

### 3. P2 · Nesouvisející chyba uvolní zámek probíhajícího příkazu

**Typ:** souběh MVI; **jistota:** vysoká, skutečný ViewModel probe.

📍 `feature/workout/impl/src/commonMain/kotlin/features/workout/active/ActiveWorkoutViewModel.kt:40`, `:73`, `:284`; `feature/routines/impl/src/commonMain/kotlin/features/routines/presentation/RoutineLibraryViewModel.kt:17`; `feature/trainingplans/impl/src/commonMain/kotlin/features/trainingplans/presentation/PlanLibraryViewModel.kt:17`.

Sdílená obsluha `onOperationFailure` nastavuje `isSaving=false` / `busy=false` pro libovolnou `launchOperation`. Selhání dříve založeného katalogového, usage nebo statistického odběru tak odemkne dosud čekající finish/start/duplicate. V editorových VM je tato třída chyby již vědomě vyřešená pomocí odděleného `launchTerminal`; ostatní obrazovky mají starý vzor.

**Prokázaný scénář:** Finish čeká na gate, `isSaving=true`; katalogový Flow vyhodí chybu; `isSaving=false`, ačkoli finish stále čeká. Další Finish je přijat a počet volání completion vzroste na 2. Datová idempotence zabraňuje dvojímu posunu plánu, ale UI nemá slibovanou ochranu před opakováním a může generovat více completion efektů či další konfliktní příkazy. Konkrétní druhotná navigace závisí na lifecycle; není tvrzena jako prokázaná.

🛠️ Jen operace vlastnící zámek jej smí uvolnit. Oddělit observation/statistics errors od command failure a terminálního busy stavu. Doplnit pending command + pozdní observer error test pro workout i knihovny; současný test „mutatingIntentsAreIgnoredWhileFinishIsPending“ nevyvolá souběžnou chybu, proto tuto díru nenajde.

### 4. P2 · „Poslední výkon“ vybírá náhodné ID, nikoli poslední čas

**Typ:** datová správnost; **jistota:** vysoká, skutečný Room probe.

📍 `feature/workout/impl/src/commonMain/kotlin/features/workout/data/dao/WorkoutDao.kt:60`; `feature/workout/impl/src/commonMain/kotlin/features/workout/data/RoomWorkoutStore.kt:35`, `:43`; `feature/workout/impl/src/commonMain/kotlin/features/workout/data/OfflineFirstWorkoutRepository.kt:197`, `:388`.

`getLastPerformanceForExercise` řadí `ORDER BY id DESC`. Set ID jsou random UUID, navíc se mísí ID s prefixem `set-` a holé UUID. Výsledek není chronologický a starší legacy ID může systematicky předbíhat novější workouty z rutiny. Query zároveň nevyjadřuje, zda požaduje dokončenou session, nebo pouze odškrtnutou sérii.

**Prokázaný scénář:** stará dokončená session s end=200 a set ID `zz-old`, nová s end=400 a set ID `aa-new`. DAO vrátí starou session. Tyto hodnoty jsou deterministická fixture demonstrující přesně stejnou závislost na náhodném lexikografickém pořadí.

🛠️ Definovat význam „předchozí výkon“ jednou v workout doméně a dotazu: typicky join na dokončenou session, pořadí podle jejího end/start času a stabilní tie-breaker série. Pokud je důležité pořadí dokončení jednotlivých sérií, přidat odpovídající čas/pořadí; nevyrábět chronologii z UUID. Testovat odlišné prefixy, opačné lexikografické ID a nedokončenou aktivní session.

### 5. P2 · Kritický editorový protokol je duplicitně implementovaný ve dvou ViewModelech

**Typ:** návrhová slabina a duplicita odpovědnosti; **jistota:** vysoká.

📍 `feature/routines/impl/src/commonMain/kotlin/features/routines/presentation/RoutineEditorViewModel.kt:19`; `feature/trainingplans/impl/src/commonMain/kotlin/features/trainingplans/presentation/PlanEditorViewModel.kt:19`; společný nízký mechanismus `core/storage/src/commonMain/kotlin/core/storage/OrderedDrafts.kt:12`.

Oba 144řádkové editory opakují lease acquire, JSON decode/version, edit/ack, retry, Back/barrier, discard, reload, terminal lock a publish→transaction→delete. Rozdíl doménové validace neodůvodňuje dvě kopie téhož algoritmu životnosti. `OrderedDrafts` je správně společný mechanismus, ale zatím neskrývá vyšší editorový protokol; jeho ordering a abort pravidla musí znát každá feature.

🛠️ Vytvořit společný controller/session pro durable editaci pomocí kompozice, s explicitními callbacky codec/load/validate/publish a výsledkem success/conflict/failure. Raw doménové modely, picker state a UI ponechat jednotlivým feature. Nezavádět jeden univerzální generický mega-ViewModel ani celý framework formulářů. Transakční publish+draft delete má mít jednoho vlastníka a testovací místo. Teprve odstranění kopie je přínos; přidat další wrapper a oba algoritmy ponechat by nepomohlo.

### 6. P2 · Vlastnictví katalogových příkazů zůstalo rozdělené mezi exercises a workout

**Typ:** zapouzdření domény a outbox; **jistota:** vysoká.

📍 `feature/exercises/api/src/commonMain/kotlin/features/exercises/api/ExerciseCatalog.kt:5`, `:13`, `:18`; `feature/exercises/impl/src/commonMain/kotlin/features/exercises/data/RoomExerciseCatalog.kt:29`, `:39`; `feature/workout/api/src/commonMain/kotlin/features/workout/domain/WorkoutRepository.kt:14`; `feature/workout/impl/src/commonMain/kotlin/features/workout/data/OfflineFirstWorkoutRepository.kt:48`, `:55`.

Exercises vlastní entity a DAO, ale veřejná `saveExercise/saveExercises` pouze uloží data. Outbox musí dodat volající; `archive` jej naopak vlastní samo. Vytvoření vlastního cviku, validace, generování ID, INSERT payload a seed katalogu stále vlastní workout repository. Další feature používající exercises tedy nedostane jeden samostatný doménový vstup pro bezpečný zápis.

Je to výslovně dokumentované chování, nikoli skrytý dnešní rollback bug. Pro reuse ovšem caller musí znát interní sync protokol a writer, což oslabuje právě nově vyčleněný modul. Už existují dva veřejné vstupy se zásadně jinými zárukami.

🛠️ Přesunout create/update/archive a jejich outbox do exercises implementace. Oddělit případné interní import/seed účastníky od běžných katalogových příkazů. Workout může kompozičně používat katalogové rozhraní, ale nemusí jej veřejně zrcadlit. Tím lze zmenšit i široký `WorkoutRepository` bez mechanického zakládání nové vrstvy pro každou metodu.

### 7. P2 před zapojením sítě · Připravený HTTP client loguje celé requesty/response

**Typ:** nebezpečný výchozí stav reusable infrastruktury; **jistota:** vysoká v konfiguraci, podmíněná runtime dopadem.

📍 `composeApp/src/commonMain/kotlin/core/network/HttpClientFactory.kt:22`–`:28`.

Factory bez podmínky instaluje `LogLevel.ALL` a posílá message do Napier. Nemá sanitizaci. V současné aplikaci není factory připojena ke skutečnému uploaderu, proto toto není tvrzení o prokázaném současném úniku dat. Po připojení auth/API se ale z kopírovaného POC stane nebezpečné výchozí chování.

🛠️ Buď odstranit dosud nepoužitou factory a Ktor scaffolding, nebo nyní nastavit bezpečný default bez bodies/citlivých headers; diagnostiku explicitně opt-in. `sanitizeHeader` je dostupný mechanismus, nikoli ochrana textových bodies. [Oficiální Ktor logging](https://ktor.io/docs/client-logging.html).

## 🧭 Další architektonické kroky — bez zbytečného přepisování

- **P3 · Modernizace host/build rozdělení.** `composeApp/build.gradle.kts:1` kombinuje KMP a Android application plugin, knihovny starý Android library plugin. `gradle.properties:20`–`:21` nastavuje `android.builtInKotlin=false` / `android.newDsl=false`, tedy současný build je vědomě na přechodovém režimu. Oficiální současný směr je samostatný Android host a `com.android.kotlin.multiplatform.library` pro sdílené knihovny. Plánovat kontrolovanou migraci se zachováním regression testů; netvrdit, že dnešní zelený build již nefunguje. [Android KMP plugin](https://developer.android.com/kotlin/multiplatform/plugin?hl=en), [JetBrains AGP 9 migrace](https://kotlinlang.org/docs/multiplatform/multiplatform-project-agp-9-migration.html).
- **P3 · Gradle convention plugins.** Téměř všechny moduly kopírují stejné targety, hierarchy, Android SDK a test dependencies; lifecycle-runtime-compose 2.8.3 je navíc natvrdo mimo version catalog v několika build souborech. Konvence pro základní KMP knihovnu a Compose feature zlepší lokalitu změn. Není důvod povinně dělit už malé core moduly na další Gradle projekty.
- **P3 před cloud synchronizací · Stabilní pořadí a dependencies outboxu.** `MutationDao.kt:13` řadí pouze wall-clock `createdAt`; zápisy používají `clock.nowMillis()`, takže kolize a návrat hodin nemají formální pořadí. `SyncManager.kt:59` pokračuje další mutací i po neúspěchu předchozí. Před konkrétním backendem bude třeba definovat persistované pořadí, pravidla blokování následných mutací a serverovou idempotenci. Dnes je uploader null a toto není aktivní cloud bug.
- **P3 · Typové mapování chyb.** `TrainingWorkflow.kt:48` mapuje různá doménová enum přes `error.toString()` a porovnání názvů. Přidání/přejmenování chyby se tiše změní na STORAGE. Malé explicitní exhaustive mappery jsou čitelnější a kompilátor upozorní na novou variantu.
- **P3 · Základ musí být doménově neutrální.** Při vyjmutí obecné kostry odstranit fitness error/formatting/semantic tokeny z foundation/designsystem; hlavní report uvádí přesné příklady. Neskrývat doménovou závislost jen přejmenováním balíčku `core`.

## ✅ Co na architektuře zachovat

1. **Skutečné feature API/impl hranice.** Žádná z prohlédnutých feature impl Gradle závislostí nevede na cizí impl. App jako composition root smí znát entity a DAO, aby sestavila jednu Room databázi. Samotná veřejnost těchto Room typů pro app není důvod přesunout je všechny do anonymního core modulu.
2. **Jeden writer, skutečné savepointy, commit před efektem.** `RoomCommandCoordinator.kt:33` správně rozlišuje vlastní ambient transaction, přijaté příkazy vlastní application scope, invalidace se hlásí až po vnějším commitu. Room aktuálně doporučuje writer/immediate transaction pro KMP atomické zápisy. [Room KMP transakce](https://developer.android.com/kotlin/multiplatform/room).
3. **Raw durable drafty a lease.** Neplatný uživatelský text se neodhazuje přes doménovou validaci. Publication abortuje vnější transakci při typed failure a při selhání draft delete. Lease brání dvěma uchovaným nav entries přepsat tutéž editaci. Refactoring má centralizovat tyto záruky, ne je zahodit.
4. **Neměnný snapshot a čistá pravidla postupu.** Prescription není FK svázán s odstranitelnou skutečnou sérií a má insert-only DAO. `PlanProgressRules` odděluje rozhodování od persistence, progressRevision kontroluje i oběhnutí cyklu. To je užitečná hluboká doménová struktura.
5. **Platformní adaptéry a nezávislé obrazovky.** Foundation commonMain neobsahuje Android/UIKit importy, platformy dodávají čas/clipboard/feedback, Room configuration je společná. Typované routes přenášejí ID; ViewModel patří nav entry, nikoli jen životnosti jedné kompozice.
6. **Testy reálných rizik.** Nejsou jen testy kopírující řádky implementace: existují skutečné rollback/CAS/migration testy a NavHost/lease/delayed-navigation regression. Nově prokázané mezery jsou přesně na švech skládání příkazů a souběžných operací, které samotné úspěšné repository testy nepokryjí.

## ⚖️ Co není nález chyby

- **Offline-only sync, chybějící iOS host, migrace jen 6→7 a export bez obnovy** jsou přiznaný rozsah POC. iOS compile neprokazuje runtime parity; host a device smoke jsou práce před deklarací plné iOS podpory, nikoli důkaz nefunkčního dnešního Android/desktop POC.
- **Navigation workaround** chrání konkrétní připnutou verzi a má runtime regression. Odstranit jej pouze po upgradu a testu obnovy hosta; doporučení jiné navigační knihovny samo o sobě není defect.
- **Channel pro jednorázové UI efekty** má v projektu explicitně omezenou životnost; DB commit je business autorita. Současná Android doporučení preferují UI events zpracovat do state a lifecycle-aware collection. Je rozumné tento tradeoff zdokumentovat a otestovat, není přesné celý stávající MVI označit za chybný pouze kvůli Channel. [Doporučení Android architektury](https://developer.android.com/topic/architecture/recommendations).
- **Nejnovější verze každé knihovny** není automatická architektonická povinnost. Toolchain, Room, Compose, lifecycle, Koin a navigation je třeba modernizovat jako kompatibilní ověřenou sadu, ne jednotlivými náhodnými bump.

## 🛠️ Doporučené pořadí zpevnění

1. Opravit čtyři runtime potvrzené chyby a uchovat jejich probes jako cílené produkční regression testy.
2. Zúžit veřejné workout/catalogue příkazy tak, aby běžný caller nemusel znát interní pořadí, transaction a outbox protokol.
3. Extrahovat společný durable-editor controller a současně sjednotit vlastnictví command/observer chyb.
4. Dokončit obecné UI kontrakty a atomic skládání podle hlavního reportu; neřešit je proliferací Gradle modulů.
5. Zkonvencionalizovat build, naplánovat podporovaný Android KMP plugin/host split a přidat automatické architektonické kontroly; iOS host/cloud rozšiřovat až podle konkrétního produktového požadavku.


## 🔬 Přesná reprodukce čtyř runtime probes

Uchovat pohromadě [ContentAppArchitectureProbe.kt](evidence/2026-10-29-atomic-architecture/ContentAppArchitectureProbe.kt) a [contentapp-run-architecture-probes.py](evidence/2026-10-29-atomic-architecture/contentapp-run-architecture-probes.py). Před spuštěním je zkopírovat do dočasného adresáře podle [postupu reprodukce](evidence/2026-10-29-atomic-architecture/README.md). Archivovaný runner používá pro classpath Gradle `--offline`. Skript předpokládá již zkompilované baseline desktop artefakty a lokálně cached Kotlin 2.2.10 compiler; nic nestahuje ani neupravuje zdroje. Při prvním spuštění získá aktuální classpath pomocným dočasným Gradle init taskem. Celý postup:

```sh
python3 "$audit_tmp/contentapp-run-architecture-probes.py" "$PWD"
```

Pro opakování s již vytvořenou classpath:

```sh
python3 "$audit_tmp/contentapp-run-architecture-probes.py" "$PWD" --skip-classpath
```

Skript provádí tyto přesné kroky (hodnoty proměnných jsou sestaveny v jeho zdroji, shell nevyžaduje ručně opisovat tisíce znaků classpath):

```text
./gradlew -I <artifact-directory>/contentapp-audit-classpath.gradle
  :composeApp:auditClasspath --no-configuration-cache --console=plain --offline
  -Daudit.classpath.output=<artifact-directory>/contentapp-audit-classpath.txt

java -cp <compiler2.2.10.jar:script-runtime2.2.10.jar:desktop-test-classpath>
  org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
  -no-stdlib -no-reflect
  -Xfriend-paths=<repo>/feature/workout/impl/build/libs/impl-desktop.jar
  -classpath <desktop-test-classpath>
  -d <artifact-directory>/contentapp-audit-probe-classes
  <artifact-directory>/ContentAppArchitectureProbe.kt

java -cp <artifact-directory>/contentapp-audit-probe-classes:<desktop-test-classpath>
  ContentAppArchitectureProbeKt
```

Friend path zpřístupní pouze probe interní ViewModel; nijak nemění veřejnost produkčních tříd. První dva a čtvrtý probe používají skutečný Room. Třetí probe používá skutečný ViewModel a řízené závislosti. Každý očekávaný nesprávný stav je ohlídán `check`; exit 0 znamená, že byly reprodukovány všechny čtyři popsané vady, nikoli že je produkt bezchybný.

```text
PROVEN completion bypass: completed=true, workflow retry firstCompletion=false, nextSlot=A instead of B, revision unchanged
PROVEN split add command: second outbox fails, persisted entries=1 sets=0; first transaction cannot roll back
PROVEN terminal lock leak: pending finish unlocked by catalogue Flow error; second Finish accepted, completion calls=2
PROVEN latest-performance chronology: old session end=200 chosen over newer end=400 because zz-old sorts after aa-new
```
