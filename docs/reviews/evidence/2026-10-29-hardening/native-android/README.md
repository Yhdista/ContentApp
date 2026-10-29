# 📱 Native Android smoke — 29.10.2026

✅ Host `androidApp`, produkční strom `c858837` (QA sestavení z `6122c02`, pozdější rozdíl pouze desktop testy). Stejné dependencies a resources jako běžné APK; jediný init-script override je applicationId `com.yhdista.contentapp.atomicqa`. AAPT ověřil min27/target35/compile37, VIBRATE a oddělený applicationId. Běžná aplikace ani její DB nebyly měněny.

💻 Existující Pixel6a emulator, API37 ARM64, lokální Mac; vlastní strojový gate po dobu smoke. Build na Air přes `gw -I /tmp/contentapp-native-smoke.init.gradle :androidApp:assembleDebug` PASS5s,219tasks (22executed/197up-to-date). Instalace/launch/layout/capture přes android-cli; ovládané prvky ověřené z aktuálního layoutu, každý screenshot vizuálně prohlédnutý.

## ✅ Provedený scénář

1. Cold launch nového Android hostu → historie; všechny tři tabs fungují.
2. Rutiny → nový návrh → název `AtomicQA` → přidat `Bench press`, výchozí série0kg×10.
3. Systémové Zpět zobrazí ochranu nepublikovaného návrhu; Pokračovat v úpravách zachová data.
4. Přepnout na Plány; force-stop pouze QA procesu a cold restart (2319ms).
5. Rutiny → rozepsaný návrh: název i cvik obnoveny ze storage. [Snímek](13-draft-restored.png), [layout](13-draft-restored.json).
6. Uložit → detail publikované rutiny; Spustit rutinu → aktivní workout se zdrojem/revizí1.
7. Označit sérii jako dokončenou → CHECKED semantics a rest timer. [Snímek](18-checked.png), [layout](18-checked.json).
8. Ukončit trénink → návrat ke zdrojové rutině ([layout](19-finished.json)); tab Tréninky → dokončený `AtomicQA`, Bench press, počet1. [Snímek](20-history.png), [layout](20-history.json).

🔎 QA process log po restartu bez FATAL/SQLiteException/SecurityException. Android CLI při instalaci zalogoval vlastní `DeviceInstaller` helper ClassNotFoundException a přešel na úspěšnou standardní instalaci; nejde o crash aplikace. Historický crash jiného package z27.10 nebyl započítán do výsledku.

📏 Dvě pomocné read-only kontroly layoutu se zpřesnily podle pozorovaného UI: timer posunul bounds checkboxu, weekly badge po dokončení zobrazuje streak namísto původního počtu. CHECKED se páruje přes bounds s konkrétním content-desc; historie přes název, dokončení a oddělený počet1. Nezměnily se žádné produktové assertions.

⚠️ Toto je ruční smoke konkrétní cesty, nikoli všechny Android instrumented testy. Plán byl ověřen otevřením tabu; plné vytváření/progress plánu pokrývají JVM/Skia/Room testy. iOS runtime se zde netestoval. QA aplikace zůstává odděleně nainstalovaná. Pokus o následný úklid přes strojový gate skončil GATE_BUSY před spuštěním uninstall; cizí práce nebyla přerušena.
