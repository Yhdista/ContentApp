# 🧭 Sdílená pravidla hardening vlny

📅 29. 10. 2026. Specifikace: [audit](../reviews/2026-10-29-atomic-architecture-audit.md). Balíky a globální invarianty: [plán](../superpowers/plans/2026-10-29-atomic-architecture-hardening.md). Integrátor root. Řízení **yhdi-dev** podle výslovného pokynu uživatele; předchozí SDD review po jednotlivých balících se nepoužívá.

## 🔐 Hranice a výjimky

- Room je SSOT; app FIFO writer vlastní commit agregátu i outboxu. Cancellation přijatého příkazu nesmí ztratit zápis. Vnořené chyby mají rollback, nikoli částečný úspěch.
- Zachovat existující data a identitu schémat6/7, immutable prescriptions, actual-only statistiky, CAS, cyklické plány a raw JSON draft version1. Změnu schématu lze udělat jen s novou verzí a skutečnou migrací/testem; upgrade kompilátoru neopravňuje přepsat historii.
- Žádný cizí feature impl dependency; app je legitimní composition root. Foundation bez fitness/UI/framework závislostí. Material3 pouze nucleus po balíku3; před ním jsou stávající consumers dočasná migrační výjimka, nové přímé vazby nepřidávat.
- Obecné UI musí mít root Modifier, malé potřebné sloty, čitelné labels/semantics a48dp cíle; běžný text4.5:1;320dp,fontScale2 light/dark. Theme provider bez emitovaného layoutu nepotřebuje Modifier.
- commonMain bez platformních importů; platform adapters patří platform source setu. App/test composition smí importovat konkrétní impl pro sestavení a integrační ověření; produkční feature ne.
- Staré API lze odstranit jen po přepojení všech callers/fakes/fixtures; smazaný test jen s vysvětleným náhradním behavior pokrytím. Neoslabovat assertions kvůli zelené. Historické audit probes se nemění a nespouští jako nové očekávání CI.
- Žádný cloud/auth/deploy, push nebo merge do main v této vlně. Jen lokální commity a integrace do `refactor/atomic-foundation`.

## 🧰 Nástroje a prostředí

- Použít `codegraph explore` / MCP explore před hledáním/čtením kódu. Každý brief obsahuje ověřené `initialized=true` pro konkrétní kopii a známé přesné cesty/symboly. Chybějící index je STOP pro nový worker, opravuje jej integrátor.
- Konfiguraci/docs lze číst přímo. Nové osobní/citlivé soubory se nekopírují. SDK a resource budget nastavit podle cílového stroje; nové build worktrees mají vlastní local.properties jen s SDK cestou.
- Všechen Gradle přes absolutní cestu `yhdi-dev/.../scripts/gw`, ostatní heavy kroky přes jeho `gate_lock.sh`. Integrátor drží `.yhdista/gw.env`; jeden strojový gate, žádné cizí procesy nezavírat.
- Výchozí lokální rozpočet v00:38:1worker,2gGradle/1gKotlin,1testfork. Mac64GiB měl tlak warn,10–12GiB dostupných; stav před gate zkontrolovat. Stabilní session flags nepřepisovat při malém výkyvu.
- Před task4 nebyly nalezeny konfigurace detekt/ktlint/Konsist/API dumpů ani automatické architecture checks; nejsou tedy vymyšlené thresholds či skryté statické gate. Nové pravidlo přidat se senzorem v balíku4 a zapsat na hranici balíku.
- Preview umístit s komponentou nebo do určeného preview source setu; nevystavovat interní Compose Res mezi moduly. Nepřekládat Kotlin operator imports pomocí prostého textového výskytu; při migraci zkontrolovat lambdas a positional args.

## 🧪 K / L1 / G

- **K každého balíku:** konkrétní Gradle compile tasky dotčených modulů i consumers, commonMain metadata, desktop production/test a Android debug production/unitTest. `commonTest` se kompiluje přes desktop; žádný test task jako zkratka pro kompilaci. Platform target změněný přímo přidat do K podle dopadu.
- **L1 pouze jmenovaná výjimka:** hlášená chyba nebo sdílený persistence/concurrency kontrakt pro navazující balíky. Brief/report uvádí invariant, návaznost, přesné metody `--tests`. Žádný celý modul testů ani statika.
- **G jednou po všech balících:** kompletní desktop+Android unit/UI testy, architecture guard + negative probes, design consumer compile, APK a iOS simulator compile; isolated native smoke a screenshots podle dostupnosti, schema/diff/link kontroly. Přesné tasky integrátor upraví podle finální matice, potřebné cíle nesmí tiše vynechat.
- **Milník po balíku3:** integrátor předá konkrétní debug APK / desktop run příkaz s revizí „před finální G“. Build přes stejný gate s `YHDISTA_FULL=1`; nezdržovat další práci čekáním na uživatele.
- Po červené G opravit kód i testy a ověřit selhavší/dotčené tasky. Plnou G opakovat pouze při změně build/test prostředí, rozsahu platforem nebo sdíleného kontraktu s nevymezeným dopadem; uvést důvod a revizi.

## 📦 Handoff a review

- Každý další implementační balík má vlastní worktree/branch; žádný souběžný zápis do téhož souboru. Sdílené inventories/version catalog vlastní integrátor; implementer připraví přesnou patch doporučení a integrátor je aplikuje při integraci, případně explicitně přidělí úzký soubor v briefu.
- Report na disk: commit, změny, K příkazy+exit, L1 filtry/výsledky, odložené testy, start/konec, K/test/gate-wait časy. Chybějící rozpad označit jako odhad, ne tvrdit čistý testový čas. Přes20min a40% v Gradle prověřit na hranici balíku.
- Až po G levná triáž uloženého diffu; spouštěče eskalace zahrnují veřejné API, testy, concurrency, persistence, security a build. Eskalační kontrola jiným výrobcem pokud dostupná; její absenci přiznat. Uživatel výslovně požaduje Astra whole-architecture review, zachovat je.
- Finální report mapuje A1–A13 na konkrétní opravy a důkazy, uvádí nepokryté platformní/runtime limity. Bez „hotovo“ jen podle kompilace či historické cached sady.
