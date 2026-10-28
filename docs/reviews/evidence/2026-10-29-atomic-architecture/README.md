# 🔬 Důkazy auditu atomic struktury a architektury

📅 29. 10. 2026 · produkční základ `fb6078d` · [společný audit](../../2026-10-29-atomic-architecture-audit.md) · [nezávislý Astra report](../../2026-10-29-astra-architecture-audit.md).

| Soubor | Co dokazuje |
| --- | --- |
| [baseline-verification.log](baseline-verification.log) | Gradle desktop/Android test gate, APK a iOS kompilace: SUCCESS, 413 úloh, 5 executed + 408 up-to-date. Testovací úlohy byly up-to-date; XML výsledky 198 desktop + 124 Android, bez chyb. |
| [ContentAppArchitectureProbe.kt](ContentAppArchitectureProbe.kt) | Čtyři check-based reprodukce nad produkčními třídami: bypass dokončení, rozdělené přidání, odemčení pending příkazu, chybné pořadí předchozího výkonu. |
| [astra-runtime-probes.log](astra-runtime-probes.log) | Všechny čtyři chyby byly runtime reprodukované; obsahuje také nesouvisející JDK native-access upozornění. |
| [contentapp-run-architecture-probes.py](contentapp-run-architecture-probes.py) | Spustitelný postup získání classpath, kompilace probe a běhu. Archivovaná varianta pro classpath používá `--offline`. |
| [designsystem-consumer-red.log](designsystem-consumer-red.log) | Nový consumer závislý pouze na designsystem nevidí veřejný typ ItemSyncStatus. Očekávané selhání testovaného kontraktu, nikoli selhání běžného buildu aplikace. |
| [designsystem-consumer-green.log](designsystem-consumer-green.log) | Kontrolní běh téhož consumeru s explicitní foundation dependency kompiluje. Produkční dependency nebyla opravována. |
| [designsystem-consumer-probe.py](designsystem-consumer-probe.py) | Znovuvytvoří izolovaný consumer mimo source tree a provede oba kompilační pokusy. |
| [atomic-inventory.json](atomic-inventory.json) | Mechanický inventář souborů/importů/preview a kandidátů bez Modifier. Obsahuje i AppTheme, který sám UI layout neemituje a není součástí nálezu o chybějícím Modifier. |
| [contrast.json](contrast.json) | Přesné vypočtené kontrasty vybraných skutečných barevných párů. |
| [verify-static.py](verify-static.py) | Reprodukuje počty zdrojových souborů, importů, shodných řádků editorů a kontrasty bez úprav zdrojů. |

## 🧪 Reprodukce runtime chyb

Spouštět nad uvedeným commitem. Po opravě kódu mají dnešní probes přestat potvrzovat vadný stav; jejich exit 0 není známkou správného produktu. Pro trvalé testy je nutné změnit očekávání na požadované správné chování.

Předpoklady: funkční lokální Android/KMP toolchain, již zkompilované desktop artefakty a cached Kotlin compiler 2.2.10. Audit neotevírá uživatelskou databázi: používá in-memory Room. `-Xfriend-paths` zpřístupňuje interní ViewModel pouze testovacímu probe.

Z kořene repozitáře:

```sh
audit_tmp="$(mktemp -d /tmp/contentapp-architecture-repro.XXXXXX)"
cp docs/reviews/evidence/2026-10-29-atomic-architecture/ContentAppArchitectureProbe.kt "$audit_tmp/"
cp docs/reviews/evidence/2026-10-29-atomic-architecture/contentapp-run-architecture-probes.py "$audit_tmp/"
python3 "$audit_tmp/contentapp-run-architecture-probes.py" "$PWD"
```

Runner zapisuje classpath, pomocný Gradle init script, kompilované probe třídy a logy vedle své kopie v dočasném adresáři. Nemění produkční zdroje. Používá již sestavené desktop artefakty; při chybějící cache/artefaktech nejdříve provést běžnou přípravu projektu.

Očekávaný výstup na `fb6078d`:

```text
PROVEN completion bypass: completed=true, workflow retry firstCompletion=false, nextSlot=A instead of B, revision unchanged
PROVEN split add command: second outbox fails, persisted entries=1 sets=0; first transaction cannot roll back
PROVEN terminal lock leak: pending finish unlocked by catalogue Flow error; second Finish accepted, completion calls=2
PROVEN latest-performance chronology: old session end=200 chosen over newer end=400 because zz-old sorts after aa-new
```

## 📦 Reprodukce veřejného design-system kontraktu

```sh
python3 docs/reviews/evidence/2026-10-29-atomic-architecture/designsystem-consumer-probe.py "$PWD"
```

Skript přes dočasný init script přidá Gradle projekt mimo repozitář. První běh očekává chybu chybějícího veřejného typu; druhý přidá foundation pouze do dočasného konzumenta a očekává úspěch. Všechny vstupy a logy ponechá v novém systémovém dočasném adresáři. Používá `--offline`.

## 🎨 Reprodukce statického měření

```sh
python3 docs/reviews/evidence/2026-10-29-atomic-architecture/verify-static.py "$PWD"
```

Kontrast se počítá pro neprůhledné sRGB barvy linearizací jednotlivých kanálů, relativní luminancí `0.2126 R + 0.7152 G + 0.0722 B` a poměrem `(Lmax + 0.05) / (Lmin + 0.05)`. Nejde o screenshotové měření ani certifikaci přístupnosti celé aplikace.
