# 🛠️ Build a podporované platformy

## 📦 Jedna matice verzí

Verze vlastní [version catalog](../gradle/libs.versions.toml). Konvence v
[build-logic](../build-logic/build.gradle.kts) sdílejí cíle, SDK, JVM bytecode,
host testy a Compose setup. Konkrétní moduly dál deklarují vlastní `api` a
`implementation` dependencies.

| Součást | Verze |
|---|---|
| Kotlin a Compose compiler | 2.4.21 |
| Compose Multiplatform | 1.12.2 |
| Material3 | explicitní stabilní 1.9.0 |
| Navigation 2 / Lifecycle KMP | 2.10.0 / 2.11.0 |
| Room / SQLite bundled | 2.8.5 / 2.7.1 |
| Koin / KSP | 4.2.2 / 2.3.12 |
| AGP / Gradle wrapper | 9.3.1 / 9.5.0 |
| Coroutines / Serialization | 1.11.0 / 1.12.0 |
| Activity Compose / Android Core KTX | 1.13.0 / 1.19.1 |
| Android Views Material / Napier | 1.14.0 / 2.7.1 |

AGP9.3.1 a Gradle9.5.0 zůstávají v publikované Kotlin compatibility matici;
novější AGP není samo o sobě důvod překročit testovanou kombinaci.
Material3 je samostatně verzované: implicitní Compose alias by vybíral alpha
artefakt. POM Serialization1.12.0 používá Kotlin2.4.10 a Coroutines1.11.0
Kotlin2.2.20, oba pod zvoleným kompilátorem2.4.21. Všechny Compose dependency
souřadnice jsou v catalogu; preview anotace používá
`androidx.compose.ui.tooling.preview.Preview`.

🔎 Oficiální opora: [Kotlin compatibility](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html),
[Compose compatibility](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html),
[Android KMP plugin](https://developer.android.com/kotlin/multiplatform/plugin),
[AGP9.3](https://developer.android.com/build/releases/agp-9-3-0-release-notes).
Ověření konkrétní sestavy a jeho runtime meze uvádí acceptance report celé vlny.

## 📱 Hosté a source sets

Podporované cíle jsou Android, desktop JVM, iOS device `iosArm64` a Apple Silicon
simulátor `iosSimulatorArm64`. Intel iOS simulator `iosX64` byl odstraněn na
výslovnou volbu uživatele při modernizaci knihoven. iOS host aplikace zde není.

- `androidApp` je jediná Android application. Vlastní `MainActivity`,
  `ContentApplication`, manifest, launcher resources, Views téma a app context
  instrumentation test. Package/application ID zůstává `com.yhdista.contentapp`.
- `composeApp` je KMP knihovna s Android namespace `com.yhdista.shared`.
  Obsahuje společnou app navigaci, DB/DI composition a platformní DB adaptéry.
  Desktop entry point zůstává `composeApp/src/desktopMain/kotlin/main.kt`.
- Všech13produkčních KMP modulů používá nový
  `com.android.kotlin.multiplatform.library`. Android má jedinou variantu a
  `withHostTest {}` připojuje `commonTest` do `androidHostTest`.
- App-specific testy se přesunuly do `androidApp/src/test` a
  `androidApp/src/androidTest`. Sdílené Android unit testy používají nové
  `androidHostTest`, nikoli staré `androidUnitTest`.
- Room KSP je zapojen pro Android, desktop a oba iOS ARM cíle; historická
  schémata6/7 zůstávají v `composeApp/schemas`. Výstup compileru nesmí přepsat
  historii databáze bez skutečné nové verze a migrace.

## ▶️ Lokální příkazy

Minimum je funkční JDK17; CI jej explicitně instaluje, místní Gradle může běžet
na doporučeném JDK21. Kompilační
toolchain a JVM bytecode jsou17, bez osobních absolutních JDK cest v repozitáři.
Android potřebuje `compileSdk37`, `minSdk27`, `targetSdk35`; SDK cestu nastavte
lokálně do `local.properties` nebo `ANDROID_HOME`.
SDK Manager používá balíky `platforms;android-37.0` a `build-tools;37.0.0`.

```sh
./gradlew compileCommonMainKotlinMetadata compileKotlinDesktop compileTestKotlinDesktop \
  compileAndroidMain compileAndroidHostTest \
  :androidApp:compileDebugKotlin :androidApp:compileDebugUnitTestKotlin \
  :androidApp:compileDebugAndroidTestKotlin \
  :composeApp:compileKotlinIosArm64 :composeApp:compileKotlinIosSimulatorArm64
./gradlew desktopTest testAndroidHostTest :androidApp:testDebugUnitTest
./gradlew verifyArchitecture testArchitectureGuard
./gradlew :verification:designsystem-consumer:compileKotlinDesktop
./gradlew :androidApp:assembleDebug
./gradlew lintAndroidMain :androidApp:lintDebug
./gradlew :composeApp:run
```

V řízené agentní vlně nahrazuje přímý wrapper `yhdi-dev/scripts/gw`; dodržuje
jeden strojový gate a místní resource budget. Linux desktop UI testy potřebují
display, v CI se explicitně instalují Xvfb, OpenGL/EGL, font a X11 knihovny
a používá `xvfb-run`. Workflow je předpis prostředí; jeho skutečný GitHub běh
je samostatný důkaz. `allTests` není přenosná zkratka: může
vyžadovat nativní cíle nedostupné na daném stroji.

## 🧱 Senzory a CI

[GitHub workflow](../.github/workflows/verify.yml) odděluje Linux desktop/Android
testy, public design system consumer, architekturu, APK a Android lint od macOS
iOS device a Apple Silicon simulator compile. Nevyžaduje secrets ani deployment.

Architektonická pravidla a pozitivní/negativní fixtures popisuje
[guard README](../tools/architecture/README.md). Root `check` obsahuje scanner
a jeho fixtures; module `check` také závisí na scanneru. Plnou testovou a
platformní matici definují výslovné CI/acceptance příkazy.

Nav restore helper byl odstraněn po průchodu přesné regrese
`TypedNavigationTest.entryOwnershipRestorationAndWorkoutReturnsToSource` na
Navigation2.10.0 bez dekorace grafu. Explicitní Room invalidace zůstává:
její odstranění nebylo samostatně prokázáno. Kompilace obou ARM cílů
neprokazuje iOS runtime, link frameworku ani existenci iOS host aplikace a instrumented source compile nenahrazuje device test.
