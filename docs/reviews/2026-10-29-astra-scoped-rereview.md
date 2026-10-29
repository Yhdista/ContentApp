# ✅ Astra scoped rereview: statistiky opravené

**Verdikt: RESOLVED.** Jediný P2 nález z [původního Astra reportu](2026-10-29-astra-hardening-review.md) je vyřešen. V ohraničené opravě statistik jsem nenašel nový defect ani otevřený požadavek.

📅 29. 10. 2026 · reviewer **GPT-6 Astra** · oprava `a460474`, zkontrolovaná ve zmrazeném produkčním stavu **`88a5974`** a testovém stavu **`8b21c1e`**. Původní report zůstává historickým záznamem; toto je pouze navazující kontrola jeho statistického nálezu.

🔎 [ExerciseStatsCalculator.kt:13](../../feature/workout/api/src/commonMain/kotlin/features/workout/domain/ExerciseStatsCalculator.kt#L13) vytváří mapu dokončených sessions a před všemi výpočty filtruje skutečné série podle dokončení, cviku a členství session v této mapě. Stejná množina tedy vstupuje do počtů, objemu, osobních maxim, 1RM i historie. Aktivní nebo zatím nedoručená session už nemůže přispět falešným historickým bodem. Na řádku 33 se metadata získají přes `getValue`; název a čas na řádcích 47–48 pocházejí ze skutečné session. Fallback na epochu `0` je odstraněný. Oprava zachovává zvolenou politiku historie dokončených workoutů a nezasahuje do DAO, schématu ani předpisů.

🧪 Regrese pokrývají příčinu a výsledek:

- [ExerciseStatsCalculatorTest.kt:96](../../feature/workout/api/src/commonTest/kotlin/features/workout/ExerciseStatsCalculatorTest.kt#L96) kombinuje dokončenou session s aktivní, chybějícími metadaty, nezaškrtnutou sérií a jiným cvikem. Ověřuje členství, součty, maxima a skutečná metadata.
- Test na řádku 117 ověřuje prázdný historický výsledek při chybějící/aktivní session a přijetí stejné série po dodání dokončených metadat.
- [TrainingWorkflowTest.kt:55](../../composeApp/src/desktopTest/kotlin/app/workflow/TrainingWorkflowTest.kt#L55) používá skutečný Room a produkční `getExerciseStats`: starší dokončený workout plus nová aktivní zaškrtnutá série. Před Finish zůstává pouze starší výkon; po app `TrainingWorkflow.complete` přibude nový výkon se správnými součty, názvem, časem, pořadím a ID série. Test čte nové odběry před/po Finish; netvrdí zvláštní důkaz nepřetržitého UI collectoru.

📎 [RED evidence](evidence/2026-10-29-hardening/review-runtime-red.json) zachycuje všechny tři statistické regrese selhávající před opravou (`40→1000`, `0→80`, počet `1→2`); [GREEN evidence](evidence/2026-10-29-hardening/review-runtime-green.json) jejich úspěch na `a460474`. [Finální evidence](evidence/2026-10-29-hardening/review-final.json) uvádí agregaci **231 desktop +138 Android host/unit testů**, bez failures/errors/skips; jde o dotčené celé sady doplněné zachovanou G evidencí ostatních modulů, nikoli jeden nový běh všech testů.

⚖️ **Rozsah a limity:** CodeGraph byl použit před čtením zdrojů. Prověřen relevantní diff, aktuální kalkulátor, tři regrese a uložené výsledky. Reviewer nespouštěl buildy/testy, síť ani nativní UI a neměnil produkční zdroje. Ostatní veřejné API, terminal/UI/guard opravy nejsou předmětem tohoto rereview. Výsledek uzavírá konkrétní P2; nerozšiřuje původní POC záruky na obecnou produkční připravenost.
