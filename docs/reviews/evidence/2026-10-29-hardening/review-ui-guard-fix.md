# 🎨 Review opravy UI / guard / CI

✅ Dokončeno a rootem ověřeno: `88a5974`, tests-only metrická korekce `8b21c1e`, čistá větev `refactor/atomic-task3`. Root dokončil dotčené K/L1/širší suites i skutečnou kompilaci obou iOS ARM cílů; konečné výsledky níže. Worker nespustil Gradle ani remote změny. Tests-only snapshot: `a8e1cc0`; základ runtime/fake oprav: `c21e085`.

## 🔬 Co bylo skutečně potvrzeno

Root Air exact RED: **3 UI testy selhaly očekávaně za6s**.

| Nález | Důkaz | Bounded oprava |
|---|---|---|
| Dark DROPSET kontrast | Compose4.2306633:1, správný alpha composite; nezávislá float matematika4.2605:1 | foreground818CF8→A5B4FC, background beze změny |
| Dvouciferný badge se ořezává | `12`: dvě řádky, velikost24×24, výška řádku28, overflowHeight=true; screenshot ukázal pouze1 | `sizeIn(minWidth=24.dp,minHeight=24.dp)` místo fixed size |
| Dekorativní ikony tabů se čtou | nový contentDescription byl přístupný; finální kontrakt používá merged accessibility tree | obě responsive větve obalí icon slot `clearAndSetSemantics {}`; viditelný label/selection/callback zachován |
| Dva sourozenecké řádky detailu nemají místo | stejný screenshot: Délka cvičení skoro po písmenech, Skutečnost uvnitř slova | summary a exercise header se skládají při fontScale>1.3 nebo dostupné šířce<240dp; jinak původní kompaktní Row |

Běžné320dp/font1 mají po paddingu256dp a zachovávají kompakt. Žádná feature/domain abstrakce v DS, produkční badge se testuje přímo uvnitř skutečného WorkoutDetailScreen. Žádné fixed výšky pro nové responsivní řádky.

## 🧪 Testovací kontrakty

- Feature kontrast: všechny4typy × dokončeno/nedokončeno × light/dark. Badge nad50%surfaceVariant nad AppCard surface, foreground také composite; limit4.5:1.
- Skutečný detail320px/fontScale2: badge12 single-line, bez vertical overflow, rendered horizontal line extents uvnitř measuredText, paragraphHeight uvnitř measured height a skutečných screen bounds. Rozšíření téhož testu hlídá Celkový objem, Délka cvičení, Bench a Skutečnost: přirozené word wraps povolené, zlomy uvnitř slova zakázané, actual horizontal line extents a paragraph height kontrolované.
- Zachován celý existující light/dark320font2 DS test: input/rawtext, toggle48dp, whole navigation labels, selected/callback. Nové assertion vyžaduje skryté icon contentDescription v merged accessibility tree; unmerged testovací strom záměrně umí ukázat raw potomky pod clearAndSetSemantics.

Přesné filtry root:

```sh
:feature:workout:impl:desktopTest --tests '*WorkoutDetailAccessibilityTest.featureBadgePairsMeetContrastOnActualDetailLayers' --tests '*WorkoutDetailAccessibilityTest.doubleDigitBadgeFitsActualDetailAtDoubleFontScale'
:designsystem:desktopTest --tests '*DesignSystemAccessibilityTest.publicControlsRemainOperableAtLargeFont'
```

Captures se ukládají **před assertions** do:
- `feature/workout/impl/build/review-ui-visual/detail-{light,dark}-320-font2.png`
- `feature/workout/impl/build/review-ui-visual/detail-{light,dark}-layout.txt`
- dosavadní DS `designsystem/build/task3-visual/controls-{light,dark}-320-font2.png` a text metrics.

Root RED capture byl prohlédnut workerem: `.yhdista/review-ui-red/detail-light-320-font2.png`. Finální captures jsou v [ui-final](ui-final/detail-light-320-font2.png).

## 🧱 Guard: RED → GREEN

WorkoutStore smí pouze workout/api, workout/impl a composeApp. ExerciseCatalogImport analogicky exercises/api, exercises/impl a composeApp. Fyzický module guard platí pro každý produkční soubor, včetně obyčejného UseCase/data. Screen/Root/ViewModel zákaz zůstává i ve vlastním impl.

Tests-only `python3 tools/architecture/test_verify.py`:19metod,20očekávaných failing subcases za1.925s; původních17metod a ownUI zákaz PASS. Negativní variants: import, symbolalias, FQN, wildcard+participant use, packagealias; oba participanti × dva obyčejné foreign soubory. Pozitivní ownapi/ownimpl/app.

Po opravě stejný příkaz **19metodPASS1.970s** a `python3 tools/architecture/verify.py --root .` **PASS**. `git diff --check` PASS. RED log `/tmp/review-ui-guard-red.log`. Komentáře/literály a původní physicalmodule/atomic kontroly zachované. Wildcard samotný bez participant use není přístup. Scanner zůstává lexikální, ne compiler/dataflow sensor.

## 🍎 CI a meze

macOS job `ios-arm` nyní kompiluje `:composeApp:compileKotlinIosArm64` i `:composeApp:compileKotlinIosSimulatorArm64`; stejný lokální kontrakt je v `docs/build-toolchain.md`. iosX64 se nevrací. Skutečný iosArm64 i simulator compile root ověřil; žádný claim iOS runtime, framework linku nebo host aplikace.

Rozsah zůstal u potvrzených review nálezů2–4 a dekorativních ikon, plus dvou přímo zachycených řádků stejného detailu. Runtime/API, schéma, navigační assertions a další architektura nedotčeny. Finální root verification GREEN.

## 📐 Tests-only metrická korekce po prvním affected běhu

Root88affected běh odhalil dva nesprávné předpoklady nových assertions, nikoli další produkční opravu: unmerged semantics zachovávají raw potomky pod clearAndSetSemantics, a font leading může dát `getLineTop=-1` pro zcela měřený28px paragraph. Skutečný opravený badge měl30×28, jednu řádku, glyphright29.671875, overflowHeight=false. Commit `8b21c1e` mění pouze oba testy: accessibility merged tree; line horizontal extents + paragraphHeight/actualbounds + nooverflow/singleline. Kontrola zlomu uvnitř slov, původní nav selection/callback a všechny produkční opravy zachované. Screenshot stále před assertions, metrics doplněny o paragraphHeight. Root tyto opravené testy spustil proti pre-fix produkci pro nový citlivostní RED a poté na finální produkci; výsledky níže.

## ✅ Finální ověření integrátorem

Root/Air provedl dotčený běh na `88a5974`: K všech consumers a testů, oba iOS ARM cíle, celé dotčené suites, 19 guard fixtures, skutečný Gradle inventory, public consumer, lint a APK. 86s / 793 tasks / 189 executed; jediné dvě neúspěšné assertions jsou výše vysvětlené metrické předpoklady. Lint 0 errors, 0 ModifierParameter warnings, 18 dalších dokumentovaných warnings.

Opravené testy `8b21c1e` + původní tři produkční UI soubory z `a8e1cc0`: všechny tři regrese znovu RED9s, včetně decorative description v merged stromu a badge12 se dvěma řádky/paragraphHeight61px. Potom se stejné tři soubory v izolované Air build kopii obnovily přesně z finálního HEAD; žádné produkční změny proti `88a5974`. Obě celé UI třídy (2 detail + 2 DS testy) PASS9s / 47 tasks / 18 executed. Původní chyby tedy testy dál zachycují.

[Finální evidence](review-final.json): **231 desktop + 138 Android host/unit, 0 failures/errors/skips**. Z toho 217 dotčených testů ověřených touto opravnou vlnou a 152 zachovaných z předchozí zmražené G. [Finální příkaz](review-final-command.sh), [UI GREEN příkaz](review-ui-final-green-command.sh), [UI GREEN log](review-ui-final-green.log), [opravené testy na původním UI](review-ui-corrected-red.json). Native Android smoke pochází z dřívější uvedené revize; zde je skutečný JVM/Skia/Room a compile důkaz, ne nový native/device runtime.
