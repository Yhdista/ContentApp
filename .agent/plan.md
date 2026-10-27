# 🏋️📱 Project Plan: OmniSync Workout Tracker (Hevy Styl) 🚀✨

> **Stav projektu:** ✅ **DOKONČENO & OTESTOVÁNO (Tasky 1–25)**  
> **Typ projektu:** Referenční Kotlin Multiplatform (KMP) & Compose Multiplatform tréninkový tracker  
> **Cílové platformy:** 📱 Android, 💻 Desktop (macOS / Windows / Linux JVM), 🍏 iOS  
> **Testy:** 🧪 **63/63 Unit testů prochází (100% Pass Rate)**

---

## 🎯 Architektonické Principy (Core Principles) 🏛️

1. 🔄 **Jednosměrný tok dat (Unidirectional Data Flow - MVI):**
   - Striktní MVI vzor: `@Stable State`, `Intent`, `Channel.BUFFERED Effect`.
   - Všechny ViewModely dědí z [`BaseViewModel`](file:///Users/yhdista/AndroidApps/ContentApp/composeApp/src/commonMain/kotlin/core/viewmodel/BaseViewModel.kt) s atomickou aktualizací `_uiState.update { copy(...) }`.
2. 🛡️ **Offline-First & Transakční Outbox:**
   - Lokální SQLite databáze (Room KMP v6) je **jediným zdrojem pravdy (SSOT)**.
   - Každá mutace se okamžitě a bezpečně zapisuje do Outboxu (`sync_mutations`).
   - Správce synchronizace [`SyncManager`](file:///Users/yhdista/AndroidApps/ContentApp/composeApp/src/commonMain/kotlin/core/sync/SyncManager.kt) garantuje ochranu před reaktivními smyčkami (`MAX_RETRIES = 5`, `syncJob?.isActive == true`).
3. 💾 **Záchrana dat v `ApplicationScope`:**
   - Dlouhotrvající databázové zápisy a dokončení tréninku běží v [`ApplicationScope`](file:///Users/yhdista/AndroidApps/ContentApp/composeApp/src/commonMain/kotlin/core/common/ApplicationScope.kt), takže přežijí opuštění obrazovky i zánik ViewModelu.
4. 🔌 **Bezpečný životní cyklus ViewModelů:**
   - Funkce `rememberScreenViewModel()` s `ViewModelStore` v [`ViewModelLifecycle.kt`](file:///Users/yhdista/AndroidApps/ContentApp/composeApp/src/commonMain/kotlin/core/viewmodel/ViewModelLifecycle.kt) garantuje zavolání `onCleared()` a zrušení `viewModelScope` při opuštění obrazovky.
5. 🎨 **Atomic Design & Duální téma (Light ☀️ / Dark 🌙):**
   - Striktní oddělení: `Nucleus` (tokeny), `Atoms` (odznaky), `Templates` (obrazovky).
   - Žádné ad-hoc `CompositionLocal` – veškeré barvy jsou svázány s `MaterialTheme.colorScheme`.
   - Dynamický přepínač témat v hlavičce obrazovky s respektováním systémového nastavení.
6. 👆 **Ergonomie v posilovně (Gym-First Ergonomics):**
   - Všechny interaktivní prvky garantují minimální terč **48 dp** (`minimumInteractiveComponentSize()`).
   - Ergonomická palcová akční zóna (Thumb Action Zone) na spodní hraně obrazovky.
7. 🛡️ **Čistá doménová logika bez závislostí:**
   - Kalkulátory ([`PersonalRecordCalculator`](file:///Users/yhdista/AndroidApps/ContentApp/composeApp/src/commonMain/kotlin/features/workout/domain/PersonalRecordCalculator.kt), [`PlateCalculator`](file:///Users/yhdista/AndroidApps/ContentApp/composeApp/src/commonMain/kotlin/features/workout/domain/PlateCalculator.kt), [`WeeklyActivityCalculator`](file:///Users/yhdista/AndroidApps/ContentApp/composeApp/src/commonMain/kotlin/features/workout/domain/WeeklyActivityCalculator.kt), [`ExerciseStatsCalculator`](file:///Users/yhdista/AndroidApps/ContentApp/composeApp/src/commonMain/kotlin/features/workout/domain/ExerciseStatsCalculator.kt)) jsou čisté Kotlin funkce bez jakýchkoliv závislostí na Androidu, Compose či Roomu.

---

## 🛠️ Přehled Realizovaných Úkolů (Completed Tasks) 📋

| Fáze / Úkol | Název & Rozsah | Stav |
| :--- | :--- | :---: |
| **Task 1** | 💾 Datový model a Room KMP entity pro tréninky | ✅ DOKONČENO |
| **Task 2** | 🔄 Workout repozitář & Seed výchozích cviků | ✅ DOKONČENO |
| **Task 3** | 🎨 Klíčové komponenty (SetRow, RestTimer, ExerciseCard, ThumbBar) | ✅ DOKONČENO |
| **Task 4** | 🧠 Obrazovka aktivního tréninku (`ActiveWorkoutScreen` & MVI) | ✅ DOKONČENO |
| **Task 5** | 📅 Obrazovka historie tréninků (`WorkoutHistoryScreen` & Navigace) | ✅ DOKONČENO |
| **Task 6** | 📱 Živé ověření na emulátoru Androidu & Desktopu | ✅ DOKONČENO |
| **Task 7** | 🗄️ Obrazovka detailu dokončeného tréninku (`WorkoutDetailScreen`) | ✅ DOKONČENO |
| **Task 8** | 🏋️ Kalkulátor kotoučů činky & Tvorba vlastních cviků | ✅ DOKONČENO |
| **Task 9** | 📋 Šablony tréninků (Push, Pull, Leg Day) | ✅ DOKONČENO |
| **Task 10** | 🏆 Automatická detekce osobních rekordů (Epley 1RM) | ✅ DOKONČENO |
| **Task 11** | 🔔 Zvukové a haptické notifikace při 0 s rest timeru (`PlatformFeedback`) | ✅ DOKONČENO |
| **Task 12** | 💾 Uložení odcvičeného tréninku jako vlastní šablony | ✅ DOKONČENO |
| **Task 13** | 📈 Statistiky cviků a historická progrese | ✅ DOKONČENO |
| **Task 14** | 📉 Křivka progrese v Compose Canvas (`ProgressionChart`) | ✅ DOKONČENO |
| **Task 15** | 📤 Export dat do CSV (Hevy styl) a JSON strukturované zálohy | ✅ DOKONČENO |
| **Task 16** | 📋 Kopírování exportu do systémové schránky (`PlatformClipboard`) | ✅ DOKONČENO |
| **Task 17** | 🏷️ Typy sérií (Warmup, Drop, Failure) s vyloučením warmup u PR | ✅ DOKONČENO |
| **Task 18** | 📅 Týdenní kalendář aktivity & Počítadlo nepřerušených dnů (Streak) | ✅ DOKONČENO |
| **Task 19** | 🔍 Fulltextové vyhledávání v historii podle názvu tréninku i cviků | ✅ DOKONČENO |
| **Task 20** | 🧪 Rozšíření testovací sady na 55 testů a živé ověření | ✅ DOKONČENO |
| **Task 21** | 🧹 Architektonické pročištění, časová pásma, Outbox a Room v6 | ✅ DOKONČENO |
| **Task 22** | 🏗️ Hloubkový refaktoring: Deep DataSource, Mappers, Result & @Stable | ✅ DOKONČENO |
| **Task 23** | 🛡️ Vytvrzení jádra (`core/`), ochrana Outboxu, iOS Native Build | ✅ DOKONČENO |
| **Task 24** | 🔍 Dvouosý `/code-review` (Standards & Spec) – 63 testů | ✅ DOKONČENO |
| **Task 25** | ☀️ Prvotřídní Light Mode grafika, dynamické přepínání témat 🌙 | ✅ DOKONČENO |

---

## 🧪 Verifikace a Příkazy pro Spuštění 🚀

```bash
# 🧪 Spuštění všech 63 unit testů a multiplatformní kompilace:
./gradlew testDebugUnitTest compileKotlinDesktop compileKotlinIosSimulatorArm64 --console=plain

# 📱 Sestavení a instalace na Android emulátor:
./gradlew installDebug && adb shell am start -n com.yhdista.contentapp/.MainActivity

# 💻 Spuštění na Desktopu:
./gradlew :composeApp:run
```
