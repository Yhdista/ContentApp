# 🧪 Opravy nálezů závěrečné G

✅ Celá aplikační desktopová sada na `9cbdace`:85testů/11suites,0fail/error/skip,7s. Spolu s nedotčenými moduly jde o220desktop +132Android host/unit testů. Testové assertions navigace, skutečné Room operace a řízené coroutine/clock scénáře zůstaly zachované. Konečná oprava atomic směru `45e2133` má následné cílené ověření, uvedené v hlavním akceptačním reportu.

## 🔍 Tři skutečné příčiny UI selhání

1. **Nekonečné čekání při cleanup.** První G čekala na `runOnIdle` ještě před zrušením ownerů; Skiko default timeout byl nekonečný. Thread dumps doložily čekání. Testy mají30s limit a společný failure-safe helper; externí ViewModelStore/ApplicationScope/Koin/DB se zavírají až po skutečném disposal UI runneru. Původní exception se zachovává, cleanup chyby jsou suppressed. Blokované zápisy se vždy uvolní. Žádné potlačení selhání ani změna produkční navigace.
2. **Koin context mezi testy.** Izolovaný navigační test prošel, ale v celé sadě byl první test zelený a další selhaly už za16–29ms při první kompozici. Skutečný source Koin4.2.2 potvrdil cachovanou první globální instanci v default Compose local. Fixture nyní explicitně poskytuje vlastní `KoinIsolatedContext` a ověřuje identitu instance. Dřívější hypotéza, že všech7ClosedScope failures způsobilo až pořadí cleanup, nebyla správná; samotná změna pořadí je nevyřešila.
3. **Chybějící katalog v smoke fixture.** Screenshoty i semantics ukázaly plně viditelné, ale disabled tlačítko Přidat cvik. Fixture obcházela `initKoin` bootstrap. Nyní volá stejný produkční `ExerciseCatalogImport.ensureDefaults()` proti skutečné Room, ověřuje uložený aktivní Benchpress a čeká na skutečně enabled picker. Android stejnou cestou prošel. Produkční dropdown se kvůli této chybě neměnil.

| Revize | Výsledek |
| --- | --- |
| caea2ad | První celáG RED; vlastněný zaseklý UI executor ukončen po thread-dumps, ostatní --continue tasks dokončeny |
| 01e75cb | Izolovaný delayed-plan scénář PASS7s; všechny původní assertions dokončeny |
| 6122c02 /05a588e | Celý appmodul RED8s/6s;7ClosedScope při první kompozici, nedostatečná izolace |
| 347d9f5 | Context opraven;83/85app testů PASS,2smoke chyby prázdného katalogu |
| e8992f6 | Přesné2RED7s; screenshot+semantics prokázaly disabled picker bez dat |
| 9cbdace | Celý appmodul85/85PASS7s po opravě bootstrapu |

## 🛠️ Scanner a Android permission

`c858837` rozlišuje root `platform.*` od segmentu v `androidx.compose.ui.platform`/`core.platform`. Přidaná pozitivní fixture zachovává10negativních platformimport/FQN/alias případů. Žádná výjimka pro konkrétní produkční soubor. Foundation manifest vlastní normální oprávnění VIBRATE pro svůj feedback adapter; host nepoužívá odstraněný INTERNET. Cílené verifyArchitecture/testArchitectureGuard/foundationlint/hostlint/APK PASS29s,417tasks;13fixtures v této fázi.

## 🧱 Atomic směr a veřejné API

`45e2133` přesouvá neutrální ActionVariant a48dp policy do nucleus. NucleusChoice skládá přímo interní Material OutlinedButton/Text se stejnou bodyMedium/48dp konfigurací; žádný odkaz z nucleus doatoms. Všech14hlášených ModifierParameter signatur přeuspořádáno a callsites pojmenovány. Fyzický rankguard `nucleus < atoms < molecules < organisms < templates` odmítá vyšší reference/importy včetně wildcard, aliases a známých symbolů s klamavým package. Comments/literals ignoruje. Zdrojový scanner není Kotlin compiler ani úplná dataflow analýza; samostatný public consumer a platformní K zůstávají nutné.

Před předáním prošlo17Pythonfixtures1.207s i standaloneguard aktuálního stromu. Následující skutečný Gradleinventory/K/lint/testy/APK vlastní root naAir; přesné výsledky jsou v [akceptaci](../../2026-10-29-hardening-acceptance.md).
