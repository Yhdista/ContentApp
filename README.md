# 🏗️ ContentApp

🏋️ Kotlin Multiplatform POC tréninkové aplikace a výchozí základ pro další aplikace. Obsahuje lokální ukládání přes Room, MVI obrazovky, šablony, historii, statistiky a export. Sdílený kód se kompiluje pro Android, desktop JVM a iOS; samostatný iOS host v repozitáři není.

## 🧭 Dokumentace pro vývoj

| 🔎 Co hledáš | Dokument |
| --- | --- |
| 💾 moduly, adresáře, odpovědnosti vrstev, DI a transakce | [Architektura projektu](docs/architecture.md) |
| ⚡ přesné použití State, Intent, Effect, ViewModel, Root a Screen | [Praktický průvodce MVI](docs/mvi.md) |
| 📱 průchod změnou váhy, lifecycle a návod na další obrazovku | [MVI příklady a postup](docs/mvi.md) |
| 📋🗓️ návrh samostatných rutin a tréninkových plánů — zatím neimplementováno | [Návrh další etapy](docs/superpowers/specs/2026-10-28-routines-trainingplans-design.md) |

## 🗂️ Struktura

| 📦 Gradle modul | Obsah | Závislosti na jiných modulech |
| --- | --- | --- |
| 🧱 `foundation` | Result/chyby, čas, aplikační scope, MVI kontrakty, platformní adaptéry a obecné modely | žádné |
| 🎨 `designsystem` | téma, typografie, rozestupy, základní prvky a šablona obrazovky | `foundation` |
| 📱 `composeApp` | platformní vstupy, aplikační DI, databáze, navigace a workout feature | `foundation`, `designsystem` |

🗂️ Sdílené moduly nemají přístup k workout feature. Aplikaci skládá `composeApp/src/commonMain/kotlin/app`; konkrétní funkce žijí v `features/workout`. Podrobnosti, pravidla přidávání dalších funkcí a omezení jsou v [architektuře](docs/architecture.md).

## 💾 Zápis a životní cyklus

💾 UI posílá intent do ViewModelu. Repository předá celý příkaz do `WorkoutWriteCoordinator`, jehož aplikační implementace příkazy řadí a provádí ve společné Room transakci. Změna dat i její outbox záznam se uloží společně. Přijatý příkaz může doběhnout po zrušení čekající obrazovky; dokončení UI zůstává ve scope ViewModelu.

💾 Editace podle ID pracují s aktuální uloženou hodnotou. Odebrání cviku i smazání a přečíslování sérií jsou celé transakční příkazy. Dokončený trénink další editace odmítá.

## 🔄 Synchronizace a migrace

⚠️ **Backend synchronizace není implementovaný.** Bez explicitního uploaderu se outbox neodesílá ani nemaže. Testy používají injektované deterministické uploadery. Připojení skutečného backendu vyžaduje vlastní protokol včetně idempotence, pořadí mutací a řešení konfliktů.

🔄 Room schéma verze 6 je uložené v `composeApp/schemas`. Destruktivní migrace jsou vypnuté. Při chybějící migraci otevření starší databáze selže a data zůstanou zachovaná. Historické migrace z verzí 1–5 nejsou součástí tohoto POC; další změny schématu musí mít migraci a test zachování dat.

## 🚀 Spuštění

```sh
./gradlew :composeApp:run
./gradlew :composeApp:installDebug
```

## 🧪 Ověření

```sh
./gradlew :composeApp:desktopTest :foundation:desktopTest \
  :composeApp:testDebugUnitTest :foundation:testDebugUnitTest \
  :composeApp:assembleDebug :composeApp:compileKotlinIosSimulatorArm64
```

🧪 Common testy ověřují výpočty, ViewModely a orchestrace. Desktop integrační testy používají skutečný Room a Bundled SQLite pro rollback, souběh, zrušení volajícího, dávkové změny a ochranu před smazáním dat při chybějící migraci. Kompilace iOS sama nedokazuje běh aplikace na zařízení.

🗃️ Původní [architektonický rozbor](docs/reviews/2026-10-28-architecture-review.md) zachycuje stav před těmito úpravami.
