# 🏗️ ContentApp

🏋️ Kotlin Multiplatform POC pro samostatné tréninky, opakovatelné rutiny a cyklické plány. Room uchovává lokální data, MVI řídí obrazovky a jedna typovaná navigace spojuje Android a desktop. Sdílený kód se kompiluje také pro iOS; samostatný iOS host v repozitáři není.

## 🧭 Dokumentace

| 🔎 Co hledáš | Dokument |
| --- | --- |
| 🧱 moduly, DI, transakce a migrace | [Architektura](docs/architecture.md) |
| 🎨 atomic vrstvy, komponenty a přístupnost | [Atomic design](docs/atomic-design.md) |
| 🧠 State, Intent, Effect, lifecycle a durable drafty | [Praktický průvodce MVI](docs/mvi.md) |
| 📋🗓️ schválená obchodní pravidla | [Návrh rutin a plánů](docs/superpowers/specs/2026-10-28-routines-trainingplans-design.md) |
| 🛠️ verze, hosté, build konvence a CI | [Build a platformy](docs/build-toolchain.md) |
| ✅ opravy architektury a jejich ověření | [Akceptace hardeningu](docs/reviews/2026-10-29-hardening-acceptance.md) |
| 📋 historická akceptace rutin/plánů | [Původní výsledky](docs/reviews/2026-10-28-routines-trainingplans-acceptance.md) |
| 🌐 historický audit na druhém počítači | [Mesh příprava](docs/reviews/2026-10-28-mesh-preparation.md) |

## 📱 Průchod aplikací

- 🏋️ **Tréninky:** nový volný trénink, pokračování, historie, statistiky a export skutečných výkonů.
- 📋 **Rutiny:** seznam včetně archivovaných položek, detail a editor jednotlivých sérií. Cvik může být zařazen vícekrát; každé zařazení má vlastní pauzu, pořadí a identitu.
- 🗓️ **Plány:** cyklus pojmenovaných slotů odkazujících na rutiny. Dokončení plánového workoutu posune jeho původní plán právě jednou. Ostatní sloty lze spustit samostatně bez posunu plánu.
- 🧭 **Kontrola postupu:** po změně zdrojového plánu nebo ručním posunu během tréninku vyberte výslovně další slot. Do potvrzení je plánový start i přeskočení blokované.
- 💾 **Rozepsané návrhy:** tlačítko „Nový nebo rozepsaný…“ obnoví nový draft; „Upravit / pokračovat v návrhu“ obnoví draft konkrétní položky. Přepnutí záložky jej zachová. Zpět nabídne pokračovat nebo zahodit. Autosave nevytváří publikovanou rutinu/plán. Jeden návrh má vždy jeden živý editor; druhé otevření nabídne Zpět a opakování po zavření původního editoru, bez přepsání jeho obsahu.
- 🔄 **Aktuální nabídky:** vytvořte rutinu v jiné záložce a vraťte se do rozepsaného plánu. Nabídka se obnovuje živě; názvy, archivace i nové cviky se promítnou bez přepsání draftu. Selhání nabídky má vlastní opakování.
- 🔒 **Konflikt revize:** načíst aktuální uloženou verzi, nebo uložit vlastní nepublikovaný obsah jako novou kopii s novými ID.
- 📋 **Původní předpis:** odebrání skutečné série či celého cviku uchová původní cíle v historii a exportu jako vynechané. Předpis se nezapočítává do objemu, statistik ani převodu výkonu na rutinu.
- 📤 **Workout → rutina:** detail historie otevře návrh ze skutečných sérií, typů a pauz. Žádné průměrování; nová rutina vznikne až tlačítkem Uložit.

## 🗂️ Moduly

| 📦 Modul | Vlastnictví |
| --- | --- |
| `foundation` | Result/chyby, čas, ApplicationScope, MVI markery, platformní adaptéry |
| `core:storage` | CommandCoordinator, Room writer, outbox/sync a lokální DraftStore/OrderedDrafts |
| `core:presentation` | BaseViewModel, entry-scoped ViewModel, DurableEditorController, UiText a Back handler |
| `designsystem` | vlastní UI tokeny, interní Material adaptéry, atoms/molecules/organisms/templates |
| `feature:exercises:{api,impl}` | společný katalog, atomické uživatelské create/update/archive + outbox; oddělený seed/import |
| `feature:workout:{api,impl}` | session, snímek předpisu, skutečné série, historie, export |
| `feature:routines:{api,impl}` | předpisy, validace, drafty a obrazovky rutin |
| `feature:trainingplans:{api,impl}` | cyklus, kurzor/revize postupu, výběr aktuálního plánu a obrazovky |
| `composeApp` | sdílené složení DB/DI, integrační workflow, typovaný NavHost, desktop vstup a platformní adaptéry |
| `androidApp` | jediný Android host: Activity, Application, manifest a resources |
| `build-logic` | společné KMP/Compose konvence; doménové závislosti zůstávají v modulech |
| `verification:designsystem-consumer` | samostatný konzument veřejného UI API pro kompilační kontrolu |

🧱 Žádná feature implementace nezávisí na implementaci jiné feature. Workout API nezná rutiny ani plány; integrační akce skládá aplikace.

## 💾 Data a hranice POC

💾 Změna agregátu a jeho jednotlivé outbox mutace se commitnou atomicky. Přijaté příkazy vlastní aplikační fronta; procesní ukončení před commitem stále znamená rollback. Editor uchovává i neplatný číselný text a při publikaci atomicky odstraní draft. Starý autosave jej po publikaci neoživí.

♻️ **Room schéma 7** a migrace **6 → 7** zachovávají ID šablon jako rutin, historii i staré mutace. Neplatné historické hodnoty se nezkracují; UI nabízí opravu. Staré pořadí workoutu je označeno jako rekonstruované, chybějící původní předpis zůstává neznámý. Migrace 1–5 nejsou implementované; destruktivní fallback je zakázaný.

🔄 **Skutečný backend sync není implementovaný.** Bez uploaderu se outbox neodesílá ani nemaže. Kalendář, notifikace a automatická progrese nejsou součástí této etapy.

📤 **CSV/JSON export je export workoutů a jejich snímků, nikoli úplná záloha aplikace.** Neexportuje definice rutin/plánů, jejich postup ani drafty. JSON odděluje skutečné `sets` od úplných `prescriptions`; CSV označí odebrané cíle jako `OMITTED` s prázdnými sloupci skutečného výkonu. Staré template projekce/API byly odstraněny. Obnova z exportu zatím není implementovaná.

🧭 Stabilní Navigation `2.10.0` používá standardní typované routes a obnovu entry. Starý pomocný `_hostEntry` argument byl odstraněn po úspěšné kontrole skutečné rekreace NavHostu; aplikace neudržuje paralelní navigační stack.

## 🚀 Spuštění a ověření

```sh
./gradlew :composeApp:run
./gradlew :androidApp:installDebug

# Všechny moduly, nikoli jen aplikace:
./gradlew desktopTest testAndroidHostTest :androidApp:testDebugUnitTest \
  verifyArchitecture testArchitectureGuard lintAndroidMain :androidApp:lintDebug \
  :verification:designsystem-consumer:compileKotlinDesktop \
  :androidApp:assembleDebug :composeApp:compileKotlinIosArm64 :composeApp:compileKotlinIosSimulatorArm64

# Skutečné Root → ViewModel → Room → NavHost interakce přes Desktop Skia:
./gradlew :composeApp:desktopTest --tests '*LibrarySmokeTest' --tests '*TypedNavigationTest'
```

🧪 Aktuální revize, počty a výsledky jsou v [akceptaci hardeningu](docs/reviews/2026-10-29-hardening-acceptance.md). Sada pokrývá raw draft/reopen, lease, konflikt/kopii, rollback/outbox, cancellation, skutečnou v6 fixture, neměnný předpis, postup plánu a živé odběry. Skia smoke vykresluje skutečné Roots/Room/NavHost; komponentové kontroly přidávají kontrast, checked state a 320dp/fontScale2. Screenshoty jsou evidence scénářů, nikoli pixelové golden testy. Android APK a iOS compile samy nedokazují běh na zařízení.
