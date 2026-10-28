# 🌐📋 Paralelní příprava přes Yhdi Mesh

> 🕰️ Historický přípravný audit základny `b9a1b70` z 28. 10. 2026. Popisuje podklady pro implementaci obrazovek; není finálním review hotové větve ani potvrzením dnešního stavu jednotlivých souborů.

## 💻 Jak se práce rozdělila

🔀 Na hlavním stroji vznikalo propojení rutin a plánů s workouty, jejich neměnné snímky a migrace databáze 6 → 7. Současně MacBook Air přes `yhdi-mesh` kontroloval připravenost navigace a editorů podle [schváleného návrhu](../superpowers/specs/2026-10-28-routines-trainingplans-design.md) a [implementačního plánu](../superpowers/plans/2026-10-28-routines-trainingplans-implementation.md).

🌿 Vzdálený worker pracoval v izolovaném git worktree nad přesným commitem z přeneseného git bundle. Neprováděl změny zdrojového kódu, instalace, buildy ani push. Úloha `job--Users-yhdista-.yhdi-mesh-repos-ContentApp-b9a1b70-20261028-114203-90eccc` skončila s návratovým kódem **0**. Prostředí workeru dovolovalo pouze čtení, proto koordinátor převzal report ze závěrečného výstupu.

## 🔍 Co audit předal implementaci

- 🧭 Nahradit vlastní zásobník jedním typovaným `NavHost`; navigační controller zůstává v app vrstvě, feature Roots dostávají callbacky.
- 🧠 Vázat ViewModel na konkrétní navigační položku, zachovat jej při dočasném překrytí a vyčistit až při skutečném odebrání položky.
- ↩️ Vést Android systémové Zpět i tlačítko editoru přes stejnou kontrolu nepublikovaných změn.
- 💾 Uchovávat v draftech i neplatné textové vstupy, původní očekávanou revizi a stabilní ID položek.
- 🚦 Řadit autosave před publikováním nebo zahozením. Uložení aggregate a smazání draftu musí být jedna transakce; starší autosave nesmí draft později obnovit.
- 📑 Při konfliktu vytvářet kopii z aktuálního formuláře s novými ID, nikoli z poslední uložené verze.
- 🧩 Doplnit doménově nezávislé UI obálky, čitelné stavy načítání/chyby/prázdného seznamu a ovládací plochy alespoň 48 dp.
- 🔒 Nevypisovat obsah intentů, poznámky, názvy, exporty ani draft JSON do logů.
- 🏋️ Používat finální workflow startu a dokončení; převod workoutu na novou rutinu musí zachovat jednotlivé skutečné série bez průměrování.

## 🧪 Hranice ověření

📖 Šlo o statickou přípravu nad uvedeným commitem. Worker nespouštěl testy ani vizuální kontrolu. Checklist byl předán implementátorovi obrazovek; ověření hotových změn patří do samostatných testů a nezávislého review.
