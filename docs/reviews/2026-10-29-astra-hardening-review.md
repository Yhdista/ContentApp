# 🔍 Finální nezávislá revize Astra: architektura a atomic základ

✅ **Architektura po hardeningu je použitelný, rozumně ohraničený základ pro další lokální obsahovou aplikaci. Původní strukturální překážky A1–A13 jsou řešené skutečnými změnami odpovědností a kontrolami.** V celém současném stromu jsem našel **jeden další P2 problém v historické projekci statistik**; existoval už před touto vlnou a nevyžaduje změnu architektury. Před převzetím hotového fitness příkladu jej doporučuji opravit. Obecný základ nepotřebuje další povinné vrstvy ani přepis navigace.

📅 29. 10. 2026 · reviewer **GPT-6 Astra** · produkční snapshot **`45e2133`**, dokumentační HEAD **`58a1d5b`** · základ diffu `fb6078d`. Jde o skutečnou nezávislou revizi modelem Astra, nikoli převyprávění jiného review.

**RISK: low.** Prověřené kategorie: veřejné API, souběh, perzistence, testy, bezpečné výchozí nastavení, build a rozsah. Žádný nový P0/P1 nález. Toto není potvrzení produkční připravenosti libovolné budoucí aplikace.

## 🚩 Jeden konkrétní nález

### P2 · Aktivní workout se ve statistikách zařadí jako nejstarší historický výkon

**Stav důkazu:** deterministicky potvrzená cesta aktuálním zdrojovým kódem; v tomto review jsem nespouštěl nový runtime probe. Nejde o regresi zavedenou hardeningem.

📍 [OfflineFirstWorkoutRepository.kt:337](../../feature/workout/impl/src/commonMain/kotlin/features/workout/data/OfflineFirstWorkoutRepository.kt#L337), [WorkoutDao.kt:75](../../feature/workout/impl/src/commonMain/kotlin/features/workout/data/dao/WorkoutDao.kt#L75), [ExerciseStatsCalculator.kt:24](../../feature/workout/api/src/commonMain/kotlin/features/workout/domain/ExerciseStatsCalculator.kt#L24), [ProgressionChart.kt:208](../../feature/workout/impl/src/commonMain/kotlin/features/workout/ui/components/ProgressionChart.kt#L208).

**Trigger:** existuje dokončený workout určitého cviku. Uživatel spustí nový workout stejného cviku, odškrtne skutečnou sérii a před dokončením celého workoutu otevře statistiky cviku.

**Očekáváno:** historická projekce má konzistentní množinu session a sérií. Buď zobrazuje pouze dokončené workouty, nebo zahrne aktuální workout se skutečným názvem a časem a výslovným významem rozepsaného výkonu.

**Skutečnost:** repository kombinuje `getCompletedSessions()` s `observeCompletedSetsForExercise()`. Druhý vstup přes `RoomWorkoutLocalDataSource.kt:73` používá DAO filtr pouze `isCompleted = 1` na sérii, nikoli na její session. Dokončená série aktivního workoutu tedy projde. Calculator ji přijme na řádku 11, nenajde její session v mapě a na řádcích 30–32 doplní **čas `0L` a název `Trénink`**. `ProgressionChart.kt:37` řadí podle tohoto času; právě provedený výkon se stane nejstarším bodem. Při existující starší dokončené session proto popisek „Aktuálně“ na řádku 219 ukazuje starší dokončený výkon a změna vůči „Start“ se počítá v nesprávném pořadí. Výpočty stále používají skutečné série, takže nejde o záměnu výkonu s předpisem.

🛠️ **Ohraničená oprava:** pro současnou historickou projekci filtrovat skutečné série podle ID dodaných dokončených sessions ještě před všemi statistikami a seskupováním. Neznámá session nesmí vytvářet falešný čas `0`. Pokud má být živý výkon součástí statistiky, dodat místo toho odpovídající session a oddělit tento význam v kontraktu. Přidat jednu regresi se starší dokončenou session a novou aktivní session s dokončenou sérií; ověřit členství, časové pořadí, součty a výsledek po dokončení nové session. Stávající `ExerciseStatsCalculatorTest` obsahuje pouze session odpovídající všem zahrnutým sériím, takže tento rozpor nezachytí.

## 🧱 Co architektonicky skutečně drží

| Oblast | Závěr z aktuální implementace |
| --- | --- |
| Sestavení aplikace | `composeApp` vlastní databázi, DI, typovaný NavHost a workflow mezi feature. `androidApp` je jediný Android host. Feature Gradle moduly nezávisí na cizích `impl`; veřejné Room typy pro sestavení app databáze jsou záměrná integrační hranice. |
| Přijaté příkazy | `RoomCommandCoordinator.kt:33` zařadí vnější příkaz do aplikační FIFO; čekající ViewModel nevlastní jeho životnost. Vnořený zápis používá stejný writer a savepoint. Kontrakt výslovně nepovoluje paralelní práci uvnitř jednoho příkazu. |
| Rollback a pozorování | Feature výjimka vrací savepoint; `TrainingWorkflow.kt:48` převádí typed child failure na abort vnějšího příkazu. `RoomCommandCoordinator.kt:56` hlásí invalidaci po vnějším commitu. Tím se odlišuje bezpečný commit od pouhého úspěchu vnořené metody. |
| Dokončení workoutu | Běžný `WorkoutRepository` už nevystavuje completion. UI přijímá `CompleteWorkout`; `TrainingWorkflow.kt:28` ukládá session, postup plánu a outbox v jedné transakci. `WorkoutStore` je dokumentovaný integrační účastník a guard jeho použití v Root/Screen/ViewModel odmítá. |
| Velikost příkazu | `addExerciseWithInitialSet` ukládá occurrence, první sérii a oba outbox záznamy v jednom vnějším příkazu. Samostatné vytvoření katalogového cviku je nyní explicitně dokumentovaná produktová volba; jeho případné nepřipojení do workoutu nevytváří poloviční workout entry. |
| Katalog | Exercises vlastní `create/update/archive`, validaci, ID a outbox. Oddělený `ExerciseCatalogImport` zachovává ID a nevrací bundled/remote data do uživatelského outboxu. App bootstrap seeduje jen prázdný katalog; workout už neimplementuje jeho zápisový protokol. |
| Předpis a postup | `RoomWorkoutStore.kt:42` ukládá samostatný insert-only předpis. Delete/renumber skutečných sérií jej nemění. `PlanProgressRules` kontroluje revision definice, slot i progressRevision; konflikt neztrácí výkon a vyžaduje kontrolu dalšího slotu. Dokončení nerevaliduje později změněnou rutinu. |
| Export a migrace | Export čte konzistentní snapshot a odděluje skutečné série od předpisů. Migrace 6→7 zachovává legacy hodnoty a původní outbox; nevymýšlí historický předpis. Konfigurace nemá destruktivní fallback. Archivovaná evidence potvrzuje beze změny schémata 6/7 v této hardening vlně. |

📍 Hlavní čtené cesty: `app/di/Koin.kt`, `AppDatabase.kt`, platformní `PlatformModule.kt`, `DatabaseConfiguration.kt`, `Migrations.kt`, `TrainingWorkflow.kt`, `RoomCommandCoordinator.kt`, routine/plan repositories a `PlanDefinitionWriter.kt`, `RoomWorkoutStore.kt`, `OfflineFirstWorkoutRepository.kt`, workout DAO/datasource/export a exercises API/implementace. Závěr tedy nevychází pouze z diffu nebo dokumentace.

## 🔄 Durable editory a MVI

✅ **A9 je odstranění duplikovaného algoritmu, nikoli jen nový název obálky.** `DurableEditorController` vlastní open/lease, raw codec, autosave ACK, barrier, terminální zámek a uzavření editoru. Feature factories dodávají namespace, verzi codec a počáteční load; ViewModely drží raw doménový návrh, validaci, choices a prezentaci výsledků. `OrderedDrafts.Session.publish` na řádku 66 má jedinou implementaci pořadí save → delete draft ve společné transakci. Typed neúspěch se uvnitř publikace mění na výjimku, takže draft není smazán při `Result.Error`.

✅ `OrderedDrafts.open` kontroluje aktivitu vlastníka před i po čtení; lease nepřebírá jiná nav entry ani převod workoutu. Release je ve stejné FIFO jako přijaté autosavy. `put` se přijímá před spuštěním čekající UI coroutine a generace odmítá pozdní zápisy po publication/discard. Raw JSON v1, původní expectedRevision a dětská ID zůstávají ve feature. Save-copy kopíruje aktuální nepublikovaný obsah s novými ID.

✅ Busy stav ActiveWorkout i obou knihoven uvolňuje vlastní příkaz ve `finally`, nikoli sdílená observation failure. Choice odběry editorů mají vlastní chybu/retry a nepřepisují draft ani lease. Zbývající podobnost feature ViewModelů představuje zejména mapování intentů a produktových výsledků; další generický superclass by nyní nepřinesl úměrnou hodnotu.

✅ Roots používají lifecycle-aware odběr stavu, `rememberScreenViewModel` skutečný nav-entry store a `CollectScreenEffects` odběr až v `RESUMED`. Typované routes přenášejí ID. Modernizace odstranila restore workaround a zachovala test skutečné rekreace NavHostu, identity ViewModelů a jejich vyčištění při pop.

⚖️ Channel effect není trvalý záznam výsledku. Kritická business autorita zůstává v DB; procesní rekreace neobnovuje doručení každého snackbar/navigačního efektu. To je dokumentovaný kompromis POC. Samotné uvolnění příkazového busy před spotřebou efektu ještě nedokazuje dvojitý pop: rozhodují aktuální session, stav entry a konkrétní trigger. Předaná terminal-effect/cancel hypotéza proto **není v tomto reportu vydávána za potvrzený defect**.

## 🎨 Atomic struktura a veřejné UI

✅ Jeden modul design systému má nyní skutečné odpovědnosti: nucleus poskytuje theme/tokeny a interní Material adaptéry; atoms základní ovládání, molecules složené interakce, organisms navigaci/top bar a templates page/insets. `MaterialAdapters.kt` nevolá vyšší atomy. Fyzický směr kontroluje guard včetně aliasů, FQN, wildcard importů a nesouladu package/directory. Nevyžaduji prázdnou vrstvu mezi každými dvěma komponentami.

✅ Veřejné API používá vlastní `ActionVariant`, `StatusTone`, `AppStyle` a obsahové sloty. `StatusBadge` už nepřenáší `ItemSyncStatus` ani foundation dependency. Modifier se předává na kořen layoutu; input nabízí keyboard options, singleLine, error/supporting text. `designsystem` exportuje Compose typy potřebné veřejným API a Material zůstává implementation. Izolovaný consumer přitom deklaruje pouze design system.

✅ Workout karty a řádky používají společné plochy, texty a akce; fitness grafy, význam PR, set types, kilogramy a předpisy zůstávají ve workout feature. To je správná hranice reuse. Barvy PR/set types jsou feature mapping, nikoli obecné fitness tokeny v design systému. `AppToggle` poskytuje checked state a label; icon-only akce mají popis účelu. Standardní řádek série zůstává kompaktní, při fontScale2 přechází na vertikální pole.

🧪 Komponentové testy kontrolují konkrétní kontrastní páry obou témat, skutečné bounds/toggle chování a raw vstup při 320 dp/fontScale2. Navigační test kontroluje i vykreslené řádky popisků. Tyto kontroly mají praktický význam, ale nepokrývají všechny fitness grafy, dialogy a budoucí kombinace obsahu. Nativní čtečka obrazovky ani plná tablet/desktop adaptivita tím nejsou automaticky ověřené.

## 🛠️ Build, hranice a kvalita důkazů

✅ KMP/Compose konvence sdílejí cíle, SDK/JVM a test setup bez schování produktových dependencies. Nový Android KMP plugin a samostatný host nahrazují původní compatibility přepínače. Testy hostu a sdílených knihoven používají odpovídající úlohy. Matice verzí je explicitní; posuzuji **doloženou kompatibilní sestavu**, nikoli neověřené tvrzení, že každé číslo je nejnovější dostupné.

✅ Gradle inventory čte skutečně deklarované dependencies konfigurací, včetně catalog aliases a conventions. Source guard mapuje importy na fyzické moduly a hlídá doménovou neutralitu, platformní importy, Material vlastnictví, UI/storage a atomic směr. Pozitivní i negativní fixtures brání triviálnímu „všechno projde“ i odmítání legitimního `androidx.compose.ui.platform`. Veřejný compile consumer doplňuje to, co regex scanner neprokáže. Scanner výslovně není Kotlin parser ani důkaz všech inferred veřejných typů; toto omezení je popsáno přiměřeně.

✅ Odstraněné fake testy seed/create/JSON katalogu mají silnější náhradu v `ExerciseCatalogCommandsTest`: skutečný Room, zachování seed ID/hodnot, escaping, granularita outboxu, rollback a caller cancellation. Testy completion používají app workflow; odstranění legacy API nezanechalo pouze test fake dokončení. Refaktoring UI harnessu zachovává assertions a původní výjimku; explicitní Koin context a cleanup po disposal řeší vlastnictví testových zdrojů. Nenašel jsem důkaz, že by zelená barva vznikla vypnutím původního scénáře.

📎 Převzaté, nikoli tímto reviewerem nově spuštěné výsledky jsou v [akceptaci](2026-10-29-hardening-acceptance.md), [G agregaci](evidence/2026-10-29-hardening/G-green-9cbdace.json) a [finálním A6 výsledku](evidence/2026-10-29-hardening/G-a6.json):

- **220 desktop +132 Android host/unit testů**, bez failures/errors/skips, agregace celé G a cílených oprav. Není to tvrzení o jednom úplně novém běhu všech testů na posledním commitu.
- Na `45e2133` opět source/test compile, iOS SimulatorArm64 compile, **13 dotčených UI testů**, **17 guard fixture metod**, Gradle inventory, consumer, lint a APK. Lint má 0 errors a 0 ModifierParameter warnings; dalších 18 dokumentovaných warningů zůstává.
- [Nativní Android smoke](evidence/2026-10-29-hardening/native-android/README.md) na `c858837` pokrývá draft, systémové Back, ukončení procesu/obnovení, publikaci, workout a historii v izolovaném QA package. Pozdější produkční změny jsou UI/API uspořádání, nikoli nová business logika.
- GitHub workflow je verzovaný a má Linux/Xvfb/Android a macOS simulator compile. Jeho konfigurace sama nedokazuje skutečně proběhlý GitHub Actions run. iOS ARM cíle jsou deklarované; zde doložená kompilace simulátoru není framework link, nativní host ani runtime parity.

## 🌱 Materiální hranice dalšího použití POC

Tyto body nejsou další potvrzené vady ani podmínka přidat nyní nové funkce:

- **Lokální životnost:** přijetí do aplikační FIFO chrání před zánikem caller scope, nikoli před ukončením procesu před commitem. Publikovaný agregát a už commitnutý raw draft jsou durable; rozpracovaný nepřijatý příkaz není job queue pro restart procesu.
- **Cloud:** uploader zůstává null. Outbox je lokální základ, nikoli hotový sync protokol. Před konkrétní integrací bude potřeba persistované pořadí/dependency mutací, serverová idempotence, konflikty a restart/retry pravidla. Smazání nepoužitého HTTP loggeru odstranilo nebezpečný výchozí příklad; nenahrazuje návrh budoucího klienta.
- **Produktová škála:** současné celé agregáty/seznamy a jednoduchá spodní navigace vyhovují malé aplikaci. Velký katalog, dlouhá historie, více jazyků a široké layouty vyžadují měření a konkrétní návrh; dnešní common vrstvy nejsou hotový paging/localization/adaptive framework.
- **Přenos mezi produkty:** převzít foundation, storage/presentation mechanismy a domain-free UI; feature moduly jsou příklad domény. České texty obecných komponent je třeba při další lokalizaci převést na zdroje nebo parametry. Katalogové create + připojení do workoutu je současná produktová hranice, kterou další aplikace musí zvolit podle vlastní uživatelské akce.
- **Platformy a data:** iOS host a device smoke, migrace 1–5, plná záloha/obnova, auth a server jsou mimo schválený rozsah. Odstranění `iosX64` odpovídá výslovné volbě uživatele. Podpora ARM source targetu se nemá prezentovat jako vydaný iOS produkt.

🎯 **Doporučení:** zachovat nynější modulární a atomic strukturu a společné mechanismy; opravit výše uvedenou malou statistickou projekci. Další abstrakce přidávat až podle druhého konkrétního použití. Celá současná architektura již nepůsobí jako dvě paralelní UI/persistence šablony, ale jako jeden srozumitelný základ s jasně popsaným rozsahem.

---

🔎 Metoda: CodeGraph použit před vyhledáváním/čtením zdrojů; aktuální hlavní subsystémy byly čteny i mimo diff. Dále zkontrolovány build konfigurace, původní audit, současná dokumentace, změny testů a uložená evidence. Tento reviewer nespouštěl buildy, testy, nativní UI ani síťové ověřování verzí a neměnil produkční kód. Jediným zapsaným souborem review je tento report.
