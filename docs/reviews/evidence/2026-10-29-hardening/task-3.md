# 🎨 Task3 — atomic UI a přístupnost

Stav: **DONE — finální ověření potvrzené integrátorem**. Integrovaný commit `29efd04` (produkční tab fix `7aba0e6`, ostatní UI `494fd12`). Air K a cílené L1 prošly; přesné následné kroky jsou v závěrečné sekci tohoto reportu. Původní předání níže zachovává časovou posloupnost: worker tehdy předával zdroj před finálním Air ověřením. Celá G a review vlny stále čekají. Branch `refactor/atomic-task3`, BASE `284d9e9`.

## 🧱 Změny

- Jeden produkční Gradle designsystem; skutečné packages atoms/molecules/organisms/templates. `LibraryComponents.kt` rozdělen na veřejné semantic komponenty a interní nucleus adaptéry. Material3 importy/konstrukce jen nucleus; Material dependency je implementation. Žádné Material2/alias/FQ obcházení.
- `AppStyle.colors` vrací vlastní `AppColorTokens`/`ToneColors`; typography vlastní `AppTextStyles` složené z Compose `TextStyle`. Veřejné API nevystavuje ColorScheme/Typography/SheetState/SnackbarHostState. `AppTheme` bez layoutu zůstává bez Modifieru.
- Všechny veřejné layout emitující komponenty mají root Modifier. `AppButton` má semantic varianty a content slot; AppInput label, keyboardOptions, singleLine/multiline, error/supporting text. AppConfirm dostává oba popisky, AppTabItem composable icon/content. Shared ReorderActions a generic `List.moved` používají oba editory.
- `StatusBadge(label,tone,modifier)` bez doménových typů; mapování u workout consumeru. Designsystem odstranil foundation dependency. Samostatný verification-only consumer deklaruje pouze designsystem a ověřuje API napříč vrstvami.
- Workout reálně používá společné texty/cards/frames/inputs/actions/selection/sheets/snackbars. Fitness karty, set form, PR paleta, stats a kalkulátor zůstávají feature UI. Stabilní keys a callbacky zachovány. Root collectors lifecycle-aware, CollectScreenEffects beze změny.
- Set completion má Checkbox/checked semantics a popis série. Políčka mají label/jednotky/kontext; 38dp vstupy odstraněny. Kompaktní režim má horní akční řadu a dvě číselná pole vedle sebe při dostupné šířce≥280dp a fontScale≤1.3. Jinak se formulář přirozeně skládá svisle; 320dp/fontScale2 pokrývají runtime testy. Běžná výška setu je runtime omezena na méně200dp, bez pevné výšky inputů. Raw `-`/`1.` přežívá numerickou echo emisi ViewModelu. Typ série a čipy mají selected semantics. Ikonové akce mají popis účelu. Skutečné společné action bounds nejméně48dp.
- Kontrastní light primary/onSurfaceVariant/status foreground opraven; ToneColors oddělují content/container/accent/onAccent. PR význam přesunut z globálních tokenů do workout. Light/dark previews u společných controls i setu. Topbar odděluje title/actions do přirozeně vysokých řad; tabs mají přirozenou výšku a obalují labels, bez pevné Material navigation výšky.

## 🧪 Povolené ověření

K (všechny moduly/consumers + test source sets, bez běhu testů):

```sh
/Users/yhdista/.codex/plugins/cache/ai-toolbox/yhdi-dev/0.4.3/skills/yhdi-dev/scripts/gw compileCommonMainKotlinMetadata compileKotlinDesktop compileTestKotlinDesktop compileDebugKotlinAndroid compileDebugUnitTestKotlinAndroid :verification:designsystem-consumer:compileKotlinDesktop
```

A5 RED baseline: `gw :verification:designsystem-consumer:compileKotlinDesktop`, exit1 (Unresolved reference core; Cannot access ItemSyncStatus), log `/Users/yhdista/AndroidApps/ContentApp-worktrees/atomic-task3/.gradle/yhdista-logs/gw-20261028T235207Z-30313.log`. Consumer neměl přímou foundation dependency.

První K GREEN: exit0,411tasks(243executed,168up-to-date),20s Gradle/96s wall; log `/Users/yhdista/AndroidApps/ContentApp-worktrees/atomic-task3/.gradle/yhdista-logs/gw-20261028T235801Z-52024.log`. Z toho přibližně76s foreign gate wait. Další K GREEN před posledním compact/nav/card doladěním: exit0,411tasks(49executed,362up-to-date),9sGradle/17swall; log `/Users/yhdista/AndroidApps/ContentApp-worktrees/atomic-task3/.gradle/yhdista-logs/gw-20261028T235944Z-58922.log`.

**Finální commit494fd12 dosud neověřen K.** Čekající K65219/65259 od01:02:04 skončilo exit143 po255s čekání na foreign HEVY_CLONE owner64141, bez spuštění Gradle a s prázdným logem `/Users/yhdista/AndroidApps/ContentApp-worktrees/atomic-task3/.gradle/yhdista-logs/gw-20261029T000204Z-65219.log`. Root v01:06 požádal zrušit pouze tuto vlastní čekající úlohu, vytvořit source commit a předat přesnou revizi k izolované Air verifikaci. Ověřeno před ukončením: foreign gate64141, naše65259 pouze čekající, log0bytes. Cizí procesy zachovány.

L1 invariants a filtry před spuštěním:

| Filtr | Runtime invariant |
| --- | --- |
| `*DesignSystemAccessibilityTest.textColorPairsMeetContrastRequirement` | Skutečně zvolená light/dark paleta má u běžného textu nejméně4.5:1. |
| `*DesignSystemAccessibilityTest.publicControlsRemainOperableAtLargeFont` | Real Compose controls mají actual interaction bounds≥48dp uvnitř320dp, fontScale2; input, callback, checked a selected fungují. |
| `*SetRowAccessibilityTest.completionHasCheckedStateAndLabel` | Skutečný set toggle má smysluplný label, off→on state a callback; bounds≥48dp. |
| `*SetRowAccessibilityTest.inputsFitAt320DpWithDoubleFontScale` | Obě reálná pole mají48dp bounds uvnitř320dp, labely a raw-/1. přežijí numeric echo při fontScale2 light/dark. |

Průběžné L1 GREEN před posledními compact/nav/card změnami: **4tests,0failures,0errors,0skips**, dvě suites. Exit0,4sGradle/30swall. Log `/Users/yhdista/AndroidApps/ContentApp-worktrees/atomic-task3/.gradle/yhdista-logs/gw-20261029T000027Z-61086.log`.

Finální L1 recept pro Air (stejné4povolené metody; DS metoda nyní navíc ověřuje pružnou topbar/tabs a set-completion metoda běžný kompaktní režim):

```sh
/Users/yhdista/.codex/plugins/cache/ai-toolbox/yhdi-dev/0.4.3/skills/yhdi-dev/scripts/gw :designsystem:desktopTest --tests '*DesignSystemAccessibilityTest.textColorPairsMeetContrastRequirement' --tests '*DesignSystemAccessibilityTest.publicControlsRemainOperableAtLargeFont' :feature:workout:impl:desktopTest --tests '*SetRowAccessibilityTest.completionHasCheckedStateAndLabel' --tests '*SetRowAccessibilityTest.inputsFitAt320DpWithDoubleFontScale'
```

XML průběžného běhu:
- `/Users/yhdista/AndroidApps/ContentApp-worktrees/atomic-task3/designsystem/build/test-results/desktopTest/TEST-ui.designsystem.DesignSystemAccessibilityTest.xml`
- `/Users/yhdista/AndroidApps/ContentApp-worktrees/atomic-task3/feature/workout/impl/build/test-results/desktopTest/TEST-features.workout.ui.components.SetRowAccessibilityTest.xml`

📷 Průběžné captures, zkontrolované workerem i rootem:
- `/Users/yhdista/AndroidApps/ContentApp-worktrees/atomic-task3/designsystem/build/task3-visual/controls-light-320-font2.png`
- `/Users/yhdista/AndroidApps/ContentApp-worktrees/atomic-task3/designsystem/build/task3-visual/controls-dark-320-font2.png`
- `/Users/yhdista/AndroidApps/ContentApp-worktrees/atomic-task3/feature/workout/impl/build/task3-visual/set-row-light-320-font2.png`
- `/Users/yhdista/AndroidApps/ContentApp-worktrees/atomic-task3/feature/workout/impl/build/task3-visual/set-row-dark-320-font2.png`

Jsou to komponentové captures, nikoli screenshot comparison ani celé app screeny; černý zbytek je prázdný testcanvas. Patří k průběžnému GREEN, nikoli k finálnímu494fd12. Finální AirL1 vygeneruje aktualizované4captures plus `feature/workout/impl/build/task3-visual/set-row-compact-320-font1.png`; compact test ověřuje, že oba inputy sdílejí řadu a normální set má přirozenou výšku<200dp.

## 🔍 Self-review a předání

Vlastní diff a volající přepojeny; `git diff --check` čisté. Ruční inventura importů našla Material3 pouze4nucleus soubory (AppTheme/AppTypography/AppTokens/MaterialAdapters); žádná arch statická gate neběžela. CodeGraph sync proveden po přesunech. Source commit má51changedfiles,1083insertions/1810deletions. Pracovní strom čistý. Build registry změna je pouze include verification-only modulu; catalog/toolchain bez zásahu. Workout lifecycle-runtime-compose2.8.3 je explicitní dependency odpovídající stávajícím editorům; centralizaci verzí vlastní Task4.

Known gap: finálníK/L1/captures pro494fd12 čekají na root/Air. Starší GREEN se nevydává za ověření finálního stromu.

## ⏱️ Čas a meze

Start přibližně00:51CET29.10.2026 (odhad z prvního čtení briefu); source handoff01:06:33CET. Celkem přibližně15.5min. Lokální Gradle čas všech dokončených kompilačních iterací včetně consumer RED podle BUILD řádků přibližně76s; L1 samotný Gradle příkaz4s (zahrnuje přípravu, ne čistý čas assertions). FinálníK255s byla jen queue, nikoli compile. Další zrušená supersededqueue41378 trvala103s. Tyto dvě queue částečně překrývají jiné čekání, proto jejich součet není čisté elapsed duration. DalšíKwall/Gradle92/10s a96/20s ukazují přibližně158s gate/preflight režie; oddělené stopky gate nejsou k dispozici, jde o odhad. Celkový průběh ovládla foreign gate queue, proto root zvolil skutečný mesh offload. Jen jeden vlastní čekající gate; superseded čekající K41417 ukončen, cizí procesy zachovány. Foreign HEVY_CLONE gate způsobil čekání; nejde o náš compile hang. Dřívější compile iterace chybovaly na migrovaných signaturách/importech, jejich logy zůstávají v `.gradle/yhdista-logs/`.

Neproběhla plná sada/statika/APK/iOS/clean ani nezávislé review. Native screenreader a plná navigační/Room UI sada patří do G. Parent připraví debug milník po integraci. Žádný push/merge do main.

## 🔧 Následná vizuální oprava navigace — 7aba0e6

Root doložil exact494fd12 na Air: K PASS411fresh tasks/345s, čtyři L1 PASS/10s. V následně stažených captures našel při320dp/fontScale2 popisky navigace rozdělené po znacích do několika řádků. Původní bounds/selection test nedostatečně měřil čitelnost textu.

Source fix `7aba0e6` mění pouze `designsystem/.../organisms/Navigation.kt` a existující `DesignSystemAccessibilityTest.kt`. AppTabs při fontScale>1.3 nebo dostupné šířce na položku<96dp použije svislé full-width choices, každé s ikonou a celým popiskem vedle sebe. Při běžném písmu a dostatku prostoru zůstávají sloupce s ikonou nad popiskem. Slots, root Modifier, callback index a selected semantics zachovány; žádná pevná výška ani fitness názvy v DS.

Stejná metoda `*DesignSystemAccessibilityTest.publicControlsRemainOperableAtLargeFont` nyní přes skutečný `GetTextLayoutResult` ověřuje pro každý popisek lineCount1 a absenci width/height overflow. Navíc měří full-width choice≥280px při density1, height≥48px a svislé nepřekrývající se bounds. Původní selection/callback assertions zachovány.

**K/L1 pro7aba0e6 PENDING root/Air.** Worker na výslovný pokyn nespustil žádný build/test; pouze CodeGraph-first čtení, úzkou úpravu, vlastní diff kontrolu a source commit. Žádná plná sada. Pro runtime opravy stačí výše uvedený jeden změněný filtr; root řídí finální K a captures z Air cache.

### 🔬 Diagnostika skutečného overflow — 5f5020f

Root: K7aba0e6 PASS411tasks(8executed403up-to-date); přesná L1 metoda selhala na didOverflowWidth/Height pro Tréninky, přestože single-line assertion prošla. CodeGraph potvrdil cestu AppSelection→AppButton→NucleusAction(Button); dropdown NucleusChoice se tohoto flow neúčastní. Příčina zatím neprokázána; bez spekulativního zásahu do produkčních controls.

Commit `5f5020f` mění pouze stávající test: capture proběhne před assertions a všechny3TextLayoutResult se uloží do `designsystem/build/task3-visual/navigation-text-layout-light-320-font2.txt` (pro dark analogicky, pokud light projde), spolu s println evidence. Metrics: size/constraints/paragraphwidth+height/line0rect, obě overflow flags, density/fontScale/style sizes a skutečné text/choice bounds. Assertions včetně absence obou overflow zůstávají. Root provede jeden stejný přesný Air filtr; worker žádný build nespustil. **Čeká se na skutečná runtime metrics pro kauzální opravu.**

### ✅ Kauzální závěr textových metrik — 29efd04

Root Air doložil: Tréninky měřenýText112×33px, constraintmaxWidth208px, paragraph208×33px, jediný vykreslený řádek[0,0,111.95,33], overflowWidth=true/height=false. Rutiny měřená šířka83px, glyphright82.71px, paragraph218px; Plány72px, glyphright71.31px, paragraph213px. Choice bounds12..308px. Skutečné popisky jsou celé uvnitř měřených Text bounds. Starší Compose porovnává šířku původního paragraph constraintboxu proti intrinsicky změřenému Text a hlásí nevyužitý horizontální prostor jako overflow. Nebylo doloženo produkční ořezání.

Test-only commit `29efd04` proto přímo kontroluje hranice KAŽDÉHO vykresleného řádku: left/top≥0, right/bottom≤layout.size. Zachovává lineCount1, zákaz vertikálního overflow, whole-choice bounds≥280×48px, svislou geometrii a selection/callback assertions. Screenshot před assertions, metrics soubor a detailní failure messages zůstávají; hlučný system-out odstraněn. Produkční kód beze změny. **Přesný Air filtr pro29efd04 čeká na root; worker lokální build/test nespustil.**

## ✅ Finální potvrzení integrátora — 01:18CET

- Exact494fd12 na Air: K411fresh tasks PASS345s,4L1 PASS10s (0fail/errors/skips). Nativní JBR17,2g/1g/1worker.
-7aba0e6 production responsive tabs: K411PASS3s (8executed/403uptodate). Test odhalil falsepositive paragraphwidth ve starém Compose; diagnostika5f5020f naměřila skutečný text111.95px v112px prvku, nikoli clipping.
-29efd04 test-only fix: per-line renderedbounds + singleline + noheightoverflow + fullchoicebounds/stack/callback. Exact `:designsystem:compileTestKotlinDesktop :designsystem:desktopTest --tests '*DesignSystemAccessibilityTest.publicControlsRemainOperableAtLargeFont'` PASS5s; log Air gw-20261029T001821Z-98967.log. Ostatní3metody od úspěšného běhu obsahově beze změny.
- Root vizuálně zkontroloval 320/font2 světlé/tmavé prvky a compact set320/font1; stackednavlight screenshot potvrzuje celé názvy. Milník494fd12 APK PASS42s předG, lokálně artifacts/atomic-milestone-494fd12/ContentApp-debug.apk.
- Integrovaný HEAD29efd04, Task3DONE. Celá G a review celé vlny stále čekají.
