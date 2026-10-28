# 🔎 Šetření návratu na přejmenovaný detail

📅 28. 10. 2026 · navazuje na Task5 `90905a9` · závěrečná akceptace na `feat/routines-trainingplans`.

✅ **Přesný dříve visící průchod nyní prochází.** Šetření odhalilo skutečnou produkční chybu: chybějící upozornění Room observerů po vnějším commitu. Po opravě dlouhodobé Flow odběry dostávají nový název i počet dotčených plánů. Rozšířený test zůstal natrvalo v [LibrarySmokeTest](../../composeApp/src/desktopTest/kotlin/app/navigation/LibrarySmokeTest.kt), nikoli jako ignorovaný nebo vypnutý test. Neprovedla se změna rendereru ani verze knihoven.

## 🧭 Přesná reprodukce před opravou

1. 📋 Ve skutečném AppNavHost vytvořit rutinu s jedním cvikem a uložit ji; přes Plány vytvořit jednoslotový plán odkazující na tuto rutinu a uložit.
2. ↩️ Přepnout na Rutiny → Upravit / pokračovat v návrhu. Změnit název na `Retained owner content`.
3. 🗓️ Přepnout na Plány → Otevřít rutinu → Upravit / pokračovat v návrhu.
4. 🔒 Druhý editor musí zobrazit vysvětlení, že návrh je otevřen jinde; ani Retry nevytvoří vstupní pole.
5. ↩️ Zpět dvakrát na detail plánu, potom Rutiny. Původní editor má stále změněný text.
6. 💾 Scroll na Uložit, uložit; počkat na Spustit rutinu a **nový název v detailu**.

🧪 Původní testové rozšíření vložené za `screenshot("plan-detail")` je uchováno [jako přesný Kotlin fragment](evidence/2026-10-28-routines-trainingplans/original-render-repro.kt.txt). Aktuální smoke navíc ověřuje `Dotčené plány: 1.` a ukládá `renamed-routine-detail.png`. Příkaz:

```sh
./gradlew :composeApp:desktopTest --tests '*LibrarySmokeTest'
```

## ❌ Pozorované příznaky

🖥️ Task5 test okamžitě nenašel nový název. Následné čekání se neukončovalo; dump zachytil AWT vlákno s vysokým CPU v `CanvasKt._nDrawPicture` a test worker v `SwingUtilities.invokeAndWait`. Task6 tento symptom znovu zachytil i po lifecycle opravě při rozšíření delayed-publication testu. [Relevantní stacky](evidence/2026-10-28-routines-trainingplans/render-before-threads.txt) jsou zachované ve zdrojovém stromu; původní dočasné soubory byly `/tmp/task5-fix1-nav-threads.txt`, `/tmp/task5-fix1-runtime-threads.txt` a `/tmp/task6-render-threads.txt`.

🧪 Přesný plný průchod po sjednocení RESUMED collectorů stále čekal na nové jméno. `/tmp/task6-render-exact.log` nedokončil test; dump `/tmp/task6-render-exact-threads.txt` zachytil tentokrát test worker ve `hasText`/`waitUntil`, nikoli důkaz stejného nativního callstacku. Ukončena byla jen vlastní Gradle test session pomocí Ctrl-C. To samo o sobě neopravňovalo závadu označit za harness-only.

📱 Na Android API37, emulator-5554, izolovaná aplikace `com.yhdista.contentapp.task6` zůstala responzivní, ale detail opakovaně ukazoval `NativeRoutine` a `Dotčené plány: 0`. Read-only kopie **jen testovací DB** ukázala:

```text
routines: name=RetainedOwnerNative, revision=2
editor_drafts: []
training_plan_slots: jedna reference na stejnou rutinu
```

🖼️ [Screenshot před opravou](evidence/2026-10-28-routines-trainingplans/android-before.png) byl vizuálně prohlédnut. Rozpor storage/UI potvrdil produkční stale-observation chybu; nešlo pouze o pomalý screenshot. Původní uživatelská aplikace ani DB nebyly upraveny.

## 🔬 Příčina a nejmenší oprava

💾 Zdroje **připnuté** `androidx.room:room-runtime-jvm:2.7.0-alpha11` byly přečteny z lokálního sources JAR (`commonMain/androidx/room/RoomDatabase.kt` a `util/DBUtil.kt`): `useWriterConnection` jen získá spojení. Automatická invalidace v DAO `internalPerform` běží pouze tehdy, pokud už transactor není uvnitř transakce. Naše DAO operace jsou vnořené do vnějšího writeru, takže tento refresh přeskočí. [Oficiální dokumentace refreshAsync](https://developer.android.com/reference/androidx/room/InvalidationTracker#refreshAsync()) rovněž uvádí nutnost ručního refresh pro některé přímé connection zápisy.

✅ [RoomCommandCoordinator](../../core/storage/src/commonMain/kotlin/core/storage/RoomCommandCoordinator.kt) nyní zavolá `database.invalidationTracker.refreshAsync()` až po dokončené vnější transakci, před úspěšným acknowledgement. Nested savepoint ani rollback nevystavují necommitnutý obsah. Obrazovky zůstávají na stejných Flow odběrech; nepřidává se polling ani ruční reload po návratu.

## 📱 Finální nativní Android průchod

✅ Stejný postup byl opakován na finálním produkčním kódu (API37, emulator-5554) v izolované instalaci. Po čistém restartu pouze testovacích dat: vytvořit NativeRoutine + jednoslotový NativePlan → editovat název na NativeFinalRename → přes plán otevřít druhý editor → explicitní lease zpráva, Retry stále bez polí → Zpět dvakrát → Rutiny s uchovaným NativeFinalRename → scroll/Uložit. Detail zobrazil **NativeFinalRename**, **Dotčené plány: 1** a zůstal responzivní.

🖼️ [Screenshot po opravě](evidence/2026-10-28-routines-trainingplans/android-after.png), [skutečný layout](evidence/2026-10-28-routines-trainingplans/android-after-layout.json) a [SQL readback](evidence/2026-10-28-routines-trainingplans/android-after-storage.json) jsou trvalé důkazy. Oba screenshoty byly vizuálně zkontrolovány. SQL potvrzuje revizi 2, žádný draft a jeden plan slot. Log jednotlivých akcí/layoutů je `/tmp/task6-android-final-journey.log`; build `/tmp/task6-android-fixed-build.log`. První příliš časný tap po startu nenašel UI a byl zopakován po načtení; žádná chyba aplikačního průchodu se při finálním opakování neignorovala.

🛡️ Izolace vznikla init skriptem, který mění pouze `android.defaultConfig.applicationId` na `com.yhdista.contentapp.task6`. Pouze tato testovací data byla při opakování vyčištěna; po smoke byl testovací balíček odinstalován. Normální package/DB nebyly měněny. Finální `composeApp-debug.apk` má znovu normální applicationId díky následnému plnému gate.

## 🧪 Ověření před → po

| Kontrola | Před | Po |
| --- | --- | --- |
| Dlouhodobý Room Flow v `RoutinePersistenceTest.liveObserverReceivesOuterCommitWithoutReopeningAndNeverSeesRollback` | ❌ Timeout čekání na změnu po commitu (`/tmp/task6-observer-red.log`) | ✅ Rename/archive bez resubscribe, rollback mezistav se neobjeví (`/tmp/task6-observer-green.log`) |
| Přesný rozšířený plný LibrarySmokeTest | ❌ Nekončící čekání na název | ✅ Dokončeno za 3 s (`/tmp/task6-render-fixed.log`), potom zahrnuto v celé zelené sadě |
| Živý počet dotčených plánů a nový název | ❌ Native detail zůstává na 0/starém názvu | ✅ Finální desktop smoke i izolovaný native Android zobrazí 1/nový název při návratu do stejného detailu |
| Navigace při opožděném commitu | ❌ Oba editory přesměrovaly cizí záložku | ✅ Samostatná oprava RESUMED collectorů a tři skutečné Root/NavHost/Room testy; buffering ověřen návratem do vlastní entry |

⚠️ Závěr platí pro tento konkrétní reprodukovaný průchod. Historický native Skia callstack sám neprokazuje nezávislou chybu Skia; po opravě chybějícího datového signálu se stejný průchod dokončuje. Nativní desktopové klikání nebylo ověřeno, původní launch pouze naběhl a CUA se k Java oknu nepřipojilo. iOS je compile-only, bez host aplikace. Žádný obecný závěr o všech rendererech ani všech zařízení z toho nevyvozujeme.
