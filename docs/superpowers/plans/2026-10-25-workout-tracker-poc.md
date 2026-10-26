# Workout Tracker (Hevy Styl) PoC Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Vytvořit produkčně odladěný 2-obrazovkový Proof of Concept (PoC) pokročilého tréninkového trackeru (Hevy styl) v KMP s důrazem na vyladění klíčových interaktivních komponent (řádek série, rest timer, karta cviku, palcová zóna) a striktní offline-first architekturu (Room KMP Outbox) na Androidu i Desktopu (macOS).

**Architecture:** Striktní Atomic Design System (izolace M3 v Nucleus, 48dp dotykové zóny), MVI prezentační vrstva s jednosměrným tokem dat, Room KMP s odděleným Outboxem (`sync_mutations`), `ApplicationScope` pro záchranu zápisů při navigaci, duální běh Android + Desktop JVM.

**Tech Stack:** Kotlin 2.2.10, Compose Multiplatform 1.7.0, Koin 4.0.2, Room KMP 2.7.0-alpha11, BundledSQLiteDriver 2.5.0-alpha11, Ktor 3.5.2, AndroidX Navigation Compose.

**Spec:** Návrh z diskuse – 2 obrazovky:
1. `WorkoutHistoryScreen`: Historie tréninků, statistika zvednutého objemu, startovací tlačítko.
2. `ActiveWorkoutScreen`: Živé tréninkové sezení, cviky, řádky sérií (váha, opakování, RPE), automatický rest timer a palcová lišta.
3. Výběr cviku jako `BottomSheet` uvnitř aktivního tréninku.

## Global Constraints
- Čistota `commonMain`: žádné importy `android.*` nebo `java.awt.*` mimo platformní moduly.
- Všechny interaktivní prvky musí dodržovat minimální dotykovou plochu **48 dp** (`minimumInteractiveComponentSize()`).
- Insets chaining na obou obrazovkách: `padding(innerPadding).consumeWindowInsets(innerPadding).imePadding()`.
- Zápisy do Roomu a mutace běží v `ApplicationScope`, aby přežily zničení `viewModelScope`.
- Deterministické unit testy korutin: žádný nekonečný `advanceUntilIdle()` s `MutableStateFlow`.

## Review Focus
1. **Rest Timer auto-trigger:** Odškrtnutí série musí automaticky spustit odpočet pauzy (např. 90 s) s možností přidat +30 s nebo přeskočit.
2. **Přesnost číselných vstupů:** Změna váhy nebo opakování nesmí vést k pádu při zadávání desetin (např. 82.5 kg).
3. **Přežití nedokončeného tréninku:** Nechtěné opuštění obrazovky nebo pád aplikace nesmí smazat rozpracované série (uloženy v Room jako aktivní sezení).
4. **Idempotentní synchronizace:** Záznam série do outboxu nese unikátní klientské UUID, aby nedocházelo k duplikaci.
5. **Konzistentní zobrazení na Androidu i Desktopu:** Všechny komponenty se renderují identicky v okně macOS i na emulátoru Android API 36.

---

## Task 1: Datový model a Room KMP entity pro tréninky

**Files:**
- Create: `composeApp/src/commonMain/kotlin/features/workout/domain/Exercise.kt`
- Create: `composeApp/src/commonMain/kotlin/features/workout/domain/WorkoutSession.kt`
- Create: `composeApp/src/commonMain/kotlin/features/workout/domain/WorkoutSet.kt`
- Create: `composeApp/src/commonMain/kotlin/features/workout/data/entity/WorkoutEntities.kt`
- Create: `composeApp/src/commonMain/kotlin/features/workout/data/dao/WorkoutDao.kt`
- Modify: `composeApp/src/commonMain/kotlin/core/database/AppDatabase.kt`
- Modify: `composeApp/src/androidMain/kotlin/core/di/PlatformModule.kt`
- Modify: `composeApp/src/desktopMain/kotlin/core/di/PlatformModule.kt`
- Modify: `composeApp/src/iosMain/kotlin/core/di/PlatformModule.kt`

**Steps:**
- [ ] Vytvořit doménové modely `Exercise`, `WorkoutSession` a `WorkoutSet` v `features/workout/domain/`.
- [ ] Vytvořit Room entity `ExerciseEntity`, `WorkoutSessionEntity`, `WorkoutSetEntity` v `features/workout/data/entity/`.
- [ ] Vytvořit `WorkoutDao` s reaktivními `Flow` pro aktivní sezení, historii tréninků, katalog cviků a transakční metody zápisu.
- [ ] Zaregistrovat entity a DAO do `AppDatabase` (verze 3) s `fallbackToDestructiveMigration(true)`.
- [ ] Poskytnout `WorkoutDao` v `platformModule` pro Android, Desktop i iOS.
- [ ] Ověřit kompilaci `./gradlew compileDebugKotlinAndroid compileKotlinDesktop`.
- [ ] Commit: `feat(workout): add Room KMP entities, DAO and database schema v3`.

---

## Task 2: Workout Repozitář & Seed výchozích cviků

**Files:**
- Create: `composeApp/src/commonMain/kotlin/features/workout/domain/WorkoutRepository.kt`
- Create: `composeApp/src/commonMain/kotlin/features/workout/data/WorkoutRepositoryImpl.kt`
- Modify: `composeApp/src/commonMain/kotlin/core/di/Koin.kt`
- Create: `composeApp/src/commonTest/kotlin/features/workout/WorkoutRepositoryTest.kt`

**Steps:**
- [ ] Definovat rozhraní `WorkoutRepository` (sledování historie, aktivního sezení, start nového tréninku, přidání/úprava série, dokončení tréninku s outboxem).
- [ ] Implementovat `WorkoutRepositoryImpl` s výchozím seedem populárních cviků (Bench press, Dřep, Mrtvý tah, Tlaky na ramena, Přítahy).
- [ ] Provázat ukládání sérií a dokončení tréninku do tabulky `sync_mutations` v témže okamžiku pro asynchronní sync.
- [ ] Zaregistrovat repozitář do `commonModule` v Koinu.
- [ ] Napsat unit test ověřující výchozí seed cviků a transakční zápis série.
- [ ] Spustit `./gradlew testDebugUnitTest` a ověřit průchod.
- [ ] Commit: `feat(workout): implement WorkoutRepository with default exercise catalog and outbox sync`.

---

## Task 3: Vyladění klíčových komponent – Řádek série, Rest Timer a Karta cviku (Atomic Design)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/features/workout/ui/components/SetRowItem.kt`
- Create: `composeApp/src/commonMain/kotlin/features/workout/ui/components/RestTimerBar.kt`
- Create: `composeApp/src/commonMain/kotlin/features/workout/ui/components/ExerciseCard.kt`
- Create: `composeApp/src/commonMain/kotlin/features/workout/ui/components/ExercisePickerSheet.kt`
- Create: `composeApp/src/commonMain/kotlin/features/workout/ui/components/WorkoutThumbBar.kt`

**Steps:**
- [ ] Implementovat `SetRowItem`:
  - Sloupec čísla série (1, 2, 3...)
  - Předchozí výkon šedě (např. "80 kg × 8")
  - Vstup pro váhu (kg) a opakování (reps) s rychlými tlačítky
  - Volič RPE (7.0 - 10.0)
  - Velké zelené odškrtávací tlačítko (striktně min 48 dp dotykový terč).
- [ ] Implementovat `RestTimerBar`:
  - Živý odpočet zbývajícího času pauzy s grafickým indikátorem.
  - Tlačítka `+30s`, `-15s` a `Přeskočit`.
  - Hladká animace zobrazení/skrytí při aktivaci.
- [ ] Implementovat `ExerciseCard`:
  - Hlavička cviku (název, partie, rest timer default).
  - Seznam sérií v kartě.
  - Tlačítko `+ Přidat sérii`.
- [ ] Implementovat `ExercisePickerSheet`:
  - Spodní panel (BottomSheet) pro vyhledání a přidání cviku do tréninku.
- [ ] Implementovat `WorkoutThumbBar`:
  - Pevná palcová lišta pro přidání cviku a ukončení tréninku.
- [ ] Ověřit kompilaci na Androidu i Desktopu.
- [ ] Commit: `feat(workout): create core atomic workout components (SetRow, RestTimer, ExerciseCard, ThumbBar)`.

---

## Task 4: Obrazovka 2 – Aktivní trénink (`ActiveWorkoutScreen` & MVI)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/features/workout/active/ActiveWorkoutContract.kt`
- Create: `composeApp/src/commonMain/kotlin/features/workout/active/ActiveWorkoutViewModel.kt`
- Create: `composeApp/src/commonMain/kotlin/features/workout/active/ActiveWorkoutScreen.kt`
- Create: `composeApp/src/commonMain/kotlin/features/workout/active/ActiveWorkoutRoot.kt`
- Modify: `composeApp/src/commonMain/kotlin/core/di/Koin.kt`
- Create: `composeApp/src/commonTest/kotlin/features/workout/ActiveWorkoutViewModelTest.kt`

**Steps:**
- [ ] Vytvořit MVI kontrakt (`ActiveWorkoutContract`) se stavy pro aktivní trénink, cviky, rest timer, běžící čas sezení a bottom sheet.
- [ ] Implementovat `ActiveWorkoutViewModel`:
  - Čítač celkového času tréninku.
  - Automatické spuštění rest timeru po označení série jako hotové (`ToggleSetCompleted`).
  - Ochrana ukládání tréninku v `ApplicationScope`.
- [ ] Implementovat `ActiveWorkoutScreen` s využitím `UniversalScreenTemplate` a vyladěných komponent.
- [ ] Napsat unit testy:
  - Spuštění rest timeru po odškrtnutí série.
  - Úprava váhy a opakování.
  - Dokončení tréninku v `ApplicationScope`.
- [ ] Spustit `./gradlew testDebugUnitTest`.
- [ ] Commit: `feat(workout): implement ActiveWorkoutScreen with MVI, auto rest-timer and unit tests`.

---

## Task 5: Obrazovka 1 – Historie tréninků (`WorkoutHistoryScreen` & Navigace)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/features/workout/history/WorkoutHistoryContract.kt`
- Create: `composeApp/src/commonMain/kotlin/features/workout/history/WorkoutHistoryViewModel.kt`
- Create: `composeApp/src/commonMain/kotlin/features/workout/history/WorkoutHistoryScreen.kt`
- Create: `composeApp/src/commonMain/kotlin/features/workout/history/WorkoutHistoryRoot.kt`
- Create: `composeApp/src/commonMain/kotlin/features/workout/navigation/WorkoutNavigation.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/yhdista/contentapp/MainActivity.kt`
- Modify: `composeApp/src/desktopMain/kotlin/main.kt`

**Steps:**
- [ ] Vytvořit MVI kontrakt a ViewModel pro historii tréninků (souhrn nazvedaného objemu, seznam proběhlých tréninků).
- [ ] Implementovat `WorkoutHistoryScreen` s přehlednými kartami a velkým startovacím tlačítkem v palcové zóně.
- [ ] Vytvořit `WorkoutNavigation.kt` spojující historii a aktivní trénink typově bezpečnou navigací.
- [ ] Přepojit `MainActivity.kt` (Android) i `main.kt` (Desktop) na nový `WorkoutNavigation` tok.
- [ ] Ověřit `./gradlew compileDebugKotlinAndroid compileKotlinDesktop`.
- [ ] Commit: `feat(workout): implement WorkoutHistoryScreen and type-safe navigation flow`.

---

## Task 6: Živé ověření na emulátoru Androidu & Desktopu a publikace na GitHub

**Files:**
- Test live: Android emulator (`emulator-5554`)
- Test live: Desktop JVM (`./gradlew :composeApp:run`)
- Update: `universal_poc_report.md`

**Steps:**
- [ ] Sestavit a nainstalovat APK na emulátor: `./gradlew :composeApp:installDebug`.
- [ ] Spustit aplikaci na Androidu, zahájit trénink, přidat cvik, vyplnit váhy, odškrtnout sérii a ověřit naskočení rest timeru.
- [ ] Spustit Desktop verzi `./gradlew :composeApp:run` a ověřit identické fungování a zápis do SQLite.
- [ ] Pořídit screenshoty a aktualizovat artefakt zprávy.
- [ ] Pushnout všechny commity do repozitáře `https://github.com/Yhdista/ContentApp`.
- [ ] Commit & Push: `docs: update PoC report with live workout tracker results`.
