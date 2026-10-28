# 🏗️ ContentApp

🏋️ Kotlin Multiplatform POC pro samostatné tréninky, opakovatelné rutiny a cyklické plány. Room uchovává lokální data, MVI řídí obrazovky a jedna typovaná navigace spojuje Android a desktop. Sdílený kód se kompiluje také pro iOS; samostatný iOS host v repozitáři není.

## 🧭 Dokumentace

| 🔎 Co hledáš | Dokument |
| --- | --- |
| 🧱 moduly, DI, transakce a migrace | [Architektura](docs/architecture.md) |
| 🧠 State, Intent, Effect, lifecycle a durable drafty | [Praktický průvodce MVI](docs/mvi.md) |
| 📋🗓️ schválená obchodní pravidla | [Návrh rutin a plánů](docs/superpowers/specs/2026-10-28-routines-trainingplans-design.md) |
| ✅ scénáře, buildy a potvrzené opravy | [Závěrečná akceptace](docs/reviews/2026-10-28-routines-trainingplans-acceptance.md) |
| 🌐 historický audit na druhém počítači | [Mesh příprava](docs/reviews/2026-10-28-mesh-preparation.md) |

## 📱 Průchod aplikací

- 🏋️ **Tréninky:** nový volný trénink, pokračování, historie, statistiky a export skutečných výkonů.
- 📋 **Rutiny:** seznam včetně archivovaných položek, detail a editor jednotlivých sérií. Cvik může být zařazen vícekrát; každé zařazení má vlastní pauzu, pořadí a identitu.
- 🗓️ **Plány:** cyklus pojmenovaných slotů odkazujících na rutiny. Dokončení plánového workoutu posune jeho původní plán právě jednou. Ostatní sloty lze spustit samostatně bez posunu plánu.
- 🧭 **Kontrola postupu:** po změně zdrojového plánu nebo ručním posunu během tréninku vyberte výslovně další slot. Do potvrzení je plánový start i přeskočení blokované.
- 💾 **Rozepsané návrhy:** tlačítko „Nový nebo rozepsaný…“ obnoví nový draft; „Upravit / pokračovat v návrhu“ obnoví draft konkrétní položky. Přepnutí záložky jej zachová. Zpět nabídne pokračovat nebo zahodit. Autosave nevytváří publikovanou rutinu/plán. Jeden návrh má vždy jeden živý editor; druhé otevření nabídne Zpět a opakování po zavření původního editoru, bez přepsání jeho obsahu.
- 🔒 **Konflikt revize:** načíst aktuální uloženou verzi, nebo uložit vlastní nepublikovaný obsah jako novou kopii s novými ID.
- 📤 **Workout → rutina:** detail historie otevře návrh ze skutečných sérií, typů a pauz. Žádné průměrování; nová rutina vznikne až tlačítkem Uložit.

## 🗂️ Moduly

| 📦 Modul | Vlastnictví |
| --- | --- |
| `foundation` | Result/chyby, čas, ApplicationScope, MVI markery, platformní adaptéry |
| `core:storage` | CommandCoordinator, Room writer, outbox/sync a lokální DraftStore/OrderedDrafts |
| `core:presentation` | BaseViewModel, entry-scoped ViewModel holder a editor Back handler |
| `designsystem` | téma, nucleus Material facades, obecné prvky a layouty |
| `feature:exercises:{api,impl}` | společný katalog cviků |
| `feature:workout:{api,impl}` | session, snímek předpisu, skutečné série, historie, export |
| `feature:routines:{api,impl}` | předpisy, validace, drafty a obrazovky rutin |
| `feature:trainingplans:{api,impl}` | cyklus, kurzor/revize postupu, výběr aktuálního plánu a obrazovky |
| `composeApp` | platformní vstupy, složení DB/DI, integrační workflow a typovaný NavHost |

🧱 Žádná feature implementace nezávisí na implementaci jiné feature. Workout API nezná rutiny ani plány; integrační akce skládá aplikace.

## 💾 Data a hranice POC

💾 Změna agregátu a jeho jednotlivé outbox mutace se commitnou atomicky. Přijaté příkazy vlastní aplikační fronta; procesní ukončení před commitem stále znamená rollback. Editor uchovává i neplatný číselný text a při publikaci atomicky odstraní draft. Starý autosave jej po publikaci neoživí.

♻️ **Room schéma 7** a migrace **6 → 7** zachovávají ID šablon jako rutin, historii i staré mutace. Neplatné historické hodnoty se nezkracují; UI nabízí opravu. Staré pořadí workoutu je označeno jako rekonstruované, chybějící původní předpis zůstává neznámý. Migrace 1–5 nejsou implementované; destruktivní fallback je zakázaný.

🔄 **Skutečný backend sync není implementovaný.** Bez uploaderu se outbox neodesílá ani nemaže. Kalendář, notifikace a automatická progrese nejsou součástí této etapy.

📤 **CSV/JSON export je export workoutů a jejich snímků, nikoli úplná záloha aplikace.** Neexportuje definice rutin/plánů, jejich postup ani drafty. Staré template projekce/API byly odstraněny; UI exportu slouží výkonům. Obnova z exportu zatím není implementovaná.

🧭 Navigation Compose `2.8.0-alpha10` používá [malý adaptační krok](composeApp/src/commonMain/kotlin/app/navigation/RestorableGraph.kt) pro obnovu prázdných argumentů mimo Android. Test obnovuje skutečný NavHost; podrobnosti a odstranění workaroundu při upgradu jsou v architektuře.

## 🚀 Spuštění a ověření

```sh
./gradlew :composeApp:run
./gradlew :composeApp:installDebug

# Všechny moduly, nikoli jen aplikace:
./gradlew desktopTest testDebugUnitTest \
  :composeApp:assembleDebug :composeApp:compileKotlinIosSimulatorArm64

# Skutečné Root → ViewModel → Room → NavHost interakce přes Desktop Skia:
./gradlew :composeApp:desktopTest --tests '*LibrarySmokeTest' --tests '*TypedNavigationTest'
```

🧪 Testy zahrnují raw draft/reopen, konflikt a kopii, rollback při selhání smazání draftu, odmítnutí pozdního autosave, skutečnou v6 fixture, workout snapshoty a idempotentní postup plánu. Skia smoke vytváří obrázky v `composeApp/build/task5-visual`; pokrývá editor, Zpět, záložky, uložení, plánový start/dokončení a světlé/tmavé téma. Android APK a iOS compile samy nedokazují běh na zařízení.
