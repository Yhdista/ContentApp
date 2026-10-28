# 🧱 Realizace auditu atomic struktury a architektury

📅 29. 10. 2026. Specifikace: [společný audit](../../reviews/2026-10-29-atomic-architecture-audit.md) a [Astra audit](../../reviews/2026-10-29-astra-architecture-audit.md). Uživatel výslovně požádal pustit se do navržených úprav. Výchozí produkční commit: `fb6078d`.

## ⚙️ Realizační postup — yhdi-dev

Uživatel výslovně aktivoval `yhdi-dev` v 00:37 CET. Tento postup nahrazuje původní SDD orchestrace. Jde o jednu vlnu šesti navazujících balíků: **K po každém balíku → jedna závěrečná G → triáž a eskalační review → Astra kontrola architektury**. Specifikace oprav níže zůstává závazná. Shared rules a briefy: [pravidla vlny](../../plans/2026-10-29-hardening-wave-rules.md). Aktuální stav: [MERGE-STATE](../../../MERGE-STATE.md).

- K = kompilace dotčených modulů a consumers včetně common metadata a testových source sets, výhradně přes `yhdi-dev/scripts/gw`.
- L1 = pouze konkrétní `--tests` pro hlášené chyby nebo sdílený persistence/concurrency invariant, na kterém stojí další balík. Přesný filtr/invariant musí být v briefu/reportu.
- Testy se průběžně píší a kompilují, ostatní běhy/statika/review patří do G. Níže uvedené ověření určuje požadované pokrytí, nikoli další plné gate po každém balíku.
- Milník pro vyzkoušení: po balíku3 debug APK / desktop run příkaz označený „před finální G“. Pokračovat bez čekání na reakci.
- Jeden integrátor, integrační větev a další implementační balíky ve vlastních připravených worktrees. Balík1 doběhl v existujícím checkoutu, kde začal před novým pokynem. Na jednom stroji jeden build; mesh příprava na Air a Windows souběžně, bez společných source writes.

## Global Constraints

- Zachovat lokální Room jako SSOT, app-owned FIFO writer, atomický aggregate/outbox commit, nested savepoint rollback a cancellation kontrakt přijatých příkazů.
- Zachovat immutable workout prescription, actual-only statistiky/exportové rozlišení, revision CAS, cyklické plány, needsReview a existující durable raw drafts včetně stabilních ID a lease.
- Žádný feature impl nesmí záviset na cizím impl. App skládá DB/DI/workflow/navigaci; foundation neobsahuje platformní UI ani fitness business pravidla.
- commonMain bez android.*, java.awt.*, UIKit.* importů. Podporované cíle Android, desktop JVM a iOS simulator compile musí zůstat funkční. Nepoužívat destructive migration. Schéma6 neměnit; změna schématu musí mít explicitní migraci a test.
- Screens nesmějí volat DAO/Room/network; ViewModely nemají implementovat storage publish protokol. Domain validation a feature draft modely zůstanou oddělené.
- Material3 konstrukce a importy soustředit do designsystem/nucleus. Zachovat jediný Gradle designsystem modul; vrstvy řídit packages a automatickou kontrolou. Fitness UI zůstává ve workout.
- Obecné UI prvky musí mít přizpůsobitelné, malé rozhraní (Modifier, potřebné sloty/parametry), správnou sémantiku a minimální cíle interakce. Běžný text kontrast alespoň4.5:1. Ověřit světlé/tmavé téma, šířku320dp a fontScale2.0 pro upravované formuláře/interakce.
- Nezapojovat cloud/auth, nemigrovat uživatelskou DB, nepublikovat/nepushovat. Auditové reporty a jejich reprodukce jsou historický důkaz pro fb6078d; nové opravy nebudou přepisovat jejich původní výsledek.
- Používat existující CodeGraph před lokalizací/čtením kódu. Protože hlavní strom je již indexovaný, každá nová kopie podle yhdi-dev dostane vlastní ověřený index před workerem; po velkém přesunu/merge sync. Jen jeden implementer mění zdroje v danou chvíli. Mesh může souběžně dělat izolovanou read-only přípravu.
- Průběžně vést canonical claude-mem work_state a integrační MERGE-STATE; předchozí ledger zůstává historií. Každý balík má vlastní commit a K důkazy. Nezávislé review pokrývá celou vlnu po G. Testy zaměřit na chování, regresní scénáře a skutečné hranice modulů, ne kopie implementace.

## Task 1: 🔐 Opravit čtyři runtime chyby a zúžit workout příkazy

**Vstupy:** audit A1–A4, baseline probe v docs/reviews/evidence/2026-10-29-atomic-architecture/ContentAppArchitectureProbe.kt. **Oblast:** workout API/impl, app workflow, příslušné ViewModely knihoven a jejich testy.

1. Nejprve převést čtyři auditové scénáře na trvalé testy s očekávaným správným chováním; doložit RED před opravou. Pro compile-boundary odebrání legacy API lze použít kontrolu veřejného interface a integrační test jediného aplikačního vstupu namísto runtime volání odebrané metody.
2. Odstranit veřejné legacy `WorkoutRepository.completeWorkout` a jeho druhou implementaci/payload; dokončení z aplikace vede přes `CompleteWorkout`/app workflow. `WorkoutStore.completeOnce` zůstává explicitním nízkoúrovňovým účastníkem workflow. Případné přesunutí tohoto kontraktu do jasně označeného integration package zachová app přístup bez feature impl cyklu. Upravit fakes/testy a všechny aplikační callers.
3. Jedna metoda `addExerciseWithInitialSet` vlastní entry+první set+outbox ve společném writer příkazu. ActiveWorkoutViewModel již neskládá dvě suspend writes. Ověřit rollback druhého outbox insertu, accepted-command caller cancellation a opakované výskyty stejného katalogového cviku. Vytvoření katalogového cviku je samostatně uložený katalogový příkaz; připojení do workoutu může být samostatná následná akce.
4. Oddělit observation failure od terminálního command locku v ActiveWorkout a obou Library ViewModelech. Jen vlastník pending command smí uvolnit busy; žádná chyba dřívějšího odběru ani úspěšná emise nesmí tento zámek resetovat. Testovat druhý Finish a Duplicate při pending command + observer failure. Nezaměňovat UI ochranu s databázovou idempotencí.
5. „Předchozí výkon“ pro nový workout znamená poslední dokončenou sérii z poslední dokončené session daného katalogového cviku. Řadit podle COALESCE(endTimeEpochMs,startTimeEpochMs) DESC, startTimeEpochMs DESC, stabilního session ID, exercise order DESC, setNumber DESC a set ID pro nerozhodné případy; nerekonstruovat čas ze set UUID. Vyloučit nedokončenou session. Nezavádět nové persisted set timestampy pouze kvůli této opravě. Testovat opačné lexikografické ID, staré/nové prefixy, aktivní session a opakované workout entry.
6. Aktualizovat bezprostředně související API dokumentaci. Zbytek katalogového vlastnictví/duplicit patří Task2, Material UI Task3.

**Ověření:** L1 pouze přesné workout VM/Library VM/Room regresní metody pro A1–A4; K včetně metadata a consumers. Kompletní desktop/Android testy a iOS compile až v G. Doložit filtry, časy a počty.

## Task 2: ♻️ Centralizovat durable editory a vlastnictví katalogu

**Vstupy:** Task1 workout kontrakty, audit A9–A10. **Oblast:** core storage/presentation, routines/trainingplans/exercises impl/API, app DI/workflow, dotčené fakes/testy.

1. Extrahovat opakovaný editor session/protocol do controlleru používaného kompozicí. Jedno místo vlastní lease acquire/release, raw decode/version, autosave enqueue/ack, barriers, terminal lock a publish/discard lifecycle. Doménové raw modely, codec, load, validation/save, revision conflict a choices zůstanou feature parametry/callbacky; nevytvářet univerzální formulářový mega-ViewModel.
2. Uložení agregátu + smazání draftu je skrytá transakční publikační operace s jedním vlastníkem. ViewModel nesmí znát DraftStore.delete, storage kind a pořadí CommandCoordinator.write + repository save + delete. Zachovat raw `-`/`1.`, exclusive live lease, žádné vzkříšení draftu po publikaci, success až po commitu, zápisy přijaté před zánikem calleru, conflict SaveCopy z raw obsahu a nezávislé recoverable choices.
3. Oba editory skutečně převést na controller a odstranit kopie protokolu. Ověřit obě feature přes jejich veřejné prezentační chování; existující retention/lease/rollback/navigation testy zůstanou smysluplné.
4. Exercises feature vlastní veřejné create/update/archive příkazy, ID/validaci a granularní outbox. Oddělit interní seed/import operace od běžných uživatelských mutací. Workout nemá veřejně vlastnit katalogové create/seed protokoly. Přímý zákaz outboxu při seed by musel být doložen produkční politikou; dosavadní seed data a ID zachovat.
5. Přesunout fitness validační chyby/mapování mimo obecnou foundation; formátování čísel pojmenovat doménově neutrálně nebo ponechat fitness helper ve workout. Pro mezidoménové chyby nahradit `toString()`/enum-name mapování explicitními exhaustive mappery.
6. Žádné schema změny bez nutnosti; zachovat všechny existující draft payloady a verzi1. Aktualizovat relevantní dokumentaci rozhraní.

**Ověření:** RED/GREEN pro nové chybějící invarianty veřejných katalogových writes; existing editor/draft/retention tests po extrakci, Room outbox rollback a accepted command cancellation. K bez plné sady; konkrétní L1 persistence/concurrency invarianty uvést v briefu. Celá desktop/Android sada + platform compile v G.

## Task 3: 🎨 Dotáhnout atomic design, UI kontrakty a přístupnost

**Vstupy:** Task2 tenké ViewModely a feature UI. Audit A5–A8. **Oblast:** designsystem a všichni UI consumers, zejména workout.

1. Rozdělit `LibraryComponents.kt`: nucleus pouze theme/tokens/interní Material adaptéry, atoms semantické button/text/input/icon/selection/feedback/layout, molecules form field/notice/reorder actions, organisms topbar/section/navigation, templates list/editor/detail layoutové skeletony. Vyšší vrstva může skládat nižší bez povinné prázdné mezivrstvy. Žádná další Gradle granularita.
2. Odstranit přímé Material3 importy/konstrukce mimo nucleus ze všech dosavadních21souborů i nových consumers. Migrace musí využít skutečné společné komponenty a tokeny; nevytvářet ve feature jen lokální přejmenované kopie Material konstrukcí. Nucleus může vystavovat technické typy potřebné k integraci, ale semantic veřejné komponenty mají vlastní soudržný kontrakt a nesmí jen exportovat celý Material balík.
3. Každý layout emitující veřejný composable přijímá `Modifier=Modifier` na kořen. Akce mají sémantické varianty a content slot; input podporuje keyboardOptions, single/multiline, error/supporting text. Potřebné přístupnost/focus/testTag lze přidat bez forků. Generic dialog dostává oba popisky/sloty; editorový copytext není hardcoded v nucleus. Tabs mají obsah/ikony od app.
4. Opravit veřejný dependency contract StatusBadge: preferovat samostatný UI stav/label/tone a mapování ItemSyncStatus u consumeru. Designsystem nebude potřebovat fitness/doménové typy. Pokud zůstává veřejný foundation typ, musí být explicitně api a consumer compile ověřen; rozhodnutí vysvětlit.
5. Sjednotit opakované card/frame/action/field konstrukce ve workout i knihovnách. Fitness ExerciseCard/SetRow/ProgressionChart zůstávají feature UI, ale skládají společné stavební prvky. Moved/reorder helpers bez kopírování mezi editory. Zachovat business chování a navigační callbacky.
6. Opravit konkrétní kontrastní páry light/dark, Primary komentář, oddělit success/pending/error content od grafických akcentů. Přenést fitness PR význam z globálních tokenů do feature; obecný accent/success může zůstat.
7. Set complete přepínač vystavuje checked state a smysluplný label; icon-only akce mají popis účelu. Inputs mají čitelný label; zachovat emoji jako doplněk. Zajistit alespoň48dp interakční plochy a flexibilní výšku/rozložení při320dp,fontScale2.0. Sjednotit workout Root state collection s lifecycle-aware odběrem bez porušení effect ownership.
8. Přidat komponentové light/dark previews a smysluplné contrast/semantics/layout testy v designsystem nebo UI integračním test hostu. Přidat izolovaný compile consumer test veřejného designsystem rozhraní.

**Ověření:** společné UI testy, real NavHost/Room LibrarySmokeTest a retained editors, vzhled při320dp/fontScale2.0 light+dark (uložit screenshot evidence), žádné Material3 importy mimo nucleus, desktop/Android testy + APK/iOS compile.

## Task 4: 🛠️ Build konvence, bezpečné výchozí nastavení a automatické hranice

**Vstupy:** stabilní moduly a atomic API Task3, audit A12–A13. **Oblast:** build-logic/Gradle/config/CI, bezpečné network a obecné kontroly.

1. Vytvořit malé convention plugins pro společnou KMP knihovnu, Compose a potřebné Android nastavení; odstranit opakované target/SDK/plugin/test setup tam, kde mají stejný význam. Verze lifecycle a dalších knihoven centralizovat v catalogu. Veřejné api a privátní implementation zůstanou explicitní v konkrétním modulu.
2. Přidat automatickou architektonickou kontrolu napojenou na Gradle check/CI: žádný cizí feature impl, foundation bez Compose/Room/Koin/fitness imports, designsystem bez feature/model/storage, commonMain bez platform imports, Material3 import/construction pouze nucleus. Testovat, že porušení vzorovým probe skutečně selže; nevkládat vadný soubor do produkce. Kontrola musí respektovat legitimní app composition a test sources.
3. Přidat consumer compile verification pro design system, relevantní component tests a minimální CI pipeline (desktop/Android tests, APK, arch checks; macOS iOS compile). Nevyžadovat tajné klíče ani deploy. Přesné příkazy zapsat do docs.
4. Nepoužitý HttpClientFactory nesmí mít LogLevel.ALL. Buď odstranit nepoužitý scaffolding včetně nepotřebných dependencies, nebo bezpečný default bez bodies/citlivých headers a test konfigurace. Zvolit jednoduché řešení bez vytvoření nového produkčního backendu.
5. Zachovat auditové baseline probes jako historické; nová pipeline je nesmí spouštět s očekáváním vadného stavu. Trvalé testy mají očekávat správné chování.

**Ověření:** Gradle configuration/arch checks, negativní architecture probe, nezávislý design system consumer, celé unit/UI sady a platformní kompilace. Bez instalace/updatu globálních nástrojů nebo resetování mesh flotily.

## Task 5: 📦 Ověřit a modernizovat kompatibilní KMP toolchain

**Vstupy:** izolovaná mesh příprava se zdroji, Task4 convention plugins. Audit A11. **Oblast:** Kotlin/CMP/Room/SQLite/Koin/Navigation/AGP a host/build integrace, potřebné compatibility adaptéry/testy.

1. Ověřit aktuální dostupné stabilní artefakty a jejich oficiální kompatibilitu, včetně lokálního JDK/Xcode/Android SDK. Zapsat konkrétní cílovou matici před změnou. Nevolit prerelease jen kvůli vyššímu číslu; nepřepínat knihovnu navigace bez potřeby.
2. Aktualizovat jako kompatibilní sestavu. Přechod z legacy Android plugin kombinace na aktuální KMP Android library plugin a samostatný Android host provést, vyžaduje-li jej ověřená cílová sestava. Neponechat obě souběžné kopie host infrastruktury. Dokumentovat nové názvy tasks/cesty.
3. Zachovat typované routes, nav entry ownership/restore, DB migration z6 i stávající7. Schéma starých verzí se nesmí změnit kvůli pouhému upgradu compileru. Potřebuje-li toolchain nové schéma, vytvořit novou verzi a skutečnou migraci.
4. Workaround `withRestorableEntries` a ruční Room invalidace odstranit jen pokud nový stack a reálné regression testy prokáží správné chování bez nich. Nepřepisovat MVI/Channel pouze kvůli modernizaci verzí.
5. Pokud oficiálně uváděný artefakt není skutečně dostupný nebo konkrétní kombinace neprojde, controller zvolí nejnovější ověřenou kompatibilní sadu a přesně zdokumentuje zbývající rozdíl. Nesmí prohlásit neotestovaný bump za hotovou modernizaci ani tiše vynechat platformu. Prozkoumat a vyřešit reálné chyby, ne se zastavit při prvním dependency problému.

**Ověření:** čistý build vhodný pro změnu toolchainu, všechny testy a architecture gates, skutečná6→aktuální migration a7→aktuální při změně, Android APK, iOS simulator compile, desktop realUI; izolovaný Android device smoke podle dostupnosti. Žádná normální uživatelská databáze.

## Task 6: 📚 Aktualizovat dokumentaci a ověřit celý základ

**Vstupy:** konečné implementované rozhraní Task1–5, původní audit jako historický důkaz.

1. Aktualizovat README/docs/architecture.md/docs/mvi.md podle skutečných souborů, module graphu a příkazů. Popsat atomic layers, veřejná UI API, command ownership, shared editor controller, katalog, nový build a CI. Smysluplná emoji, funkční lokální odkazy a diagramy.
2. Přidat závěrečný acceptance report s mapováním A1–A13 na změny, regression tests, omezení a přesné verze. Neprezentovat nepřidaný cloud/auth/iOS host/runtime coverage jako hotové; backlog má konkrétní důvod.
3. Spustit finální sjednocený gate na výsledném stromu, zachytit počty a zda byly testy skutečně provedeny nebo využily up-to-date výsledky. Native smoke jen v izolované instanci. Vyhodnotit schema diffs, veřejný consumer, arch negative probe, screenshoty, links a git diff check.
4. Nezávislý whole-branch review modelem Astra: spec, veřejné invarianty, atomic encapsulation/duplication, lifecycle, maintainability, test evidence. Po G levná triáž uloženého diffu, eskalace jiným výrobcem podle dostupnosti a požadovaný Astra review; nálezy opravit a cíleně ověřit. Další plná G pouze při zneplatnění jejího předpokladu podle yhdi-dev, s uvedeným důvodem.

**Hotovo:** opravy jsou reviewované, všechny relevantní gates zelené, dokumentace odpovídá implementaci, výsledky jsou konkrétní a reviewovatelné. Lokální feature branch; žádný automatický push ani merge do sdílené větve.
