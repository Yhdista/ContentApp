#!/usr/bin/env python3
"""Read-only inventory and contrast measurements for the fb6078d architecture audit."""
from difflib import SequenceMatcher
from pathlib import Path
import json
import re
import sys

root = Path(sys.argv[1]).resolve()
production = {}
for path in root.rglob("*.kt"):
    relative = path.relative_to(root).as_posix()
    if re.search(r"/src/(commonMain|androidMain|desktopMain|iosMain)/", relative):
        if not any(part in path.parts for part in ("build", ".gradle", ".codegraph")):
            production[relative] = path.read_text()
material = [name for name, text in production.items()
            if re.search(r"^import androidx\.compose\.material3", text, re.M)]
colors = production["designsystem/src/commonMain/kotlin/ui/designsystem/nucleus/AppColors.kt"]
palette = {name: int(value, 16) for name, value in
           re.findall(r"val (\w+) = Color\(0xFF([0-9A-Fa-f]{6})\)", colors)}

def luminance(value):
    channels = [((value >> shift) & 255) / 255 for shift in (16, 8, 0)]
    linear = [c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4 for c in channels]
    return sum(c * weight for c, weight in zip(linear, (0.2126, 0.7152, 0.0722)))

pairs = [("LightStatusPending", "LightOnStatusPending"),
         ("LightStatusSynced", "LightOnStatusSynced"),
         ("LightStatusError", "LightOnStatusError"),
         ("LightPrimary", "LightOnPrimary"),
         ("LightOnSurfaceVariant", "LightSurfaceVariant"),
         ("DarkOnSurfaceVariant", "DarkSurfaceVariant")]
ratios = []
for foreground, background in pairs:
    low, high = sorted([luminance(palette[foreground]), luminance(palette[background])])
    ratios.append({"foreground": foreground, "background": background,
                   "ratio": round((high + 0.05) / (low + 0.05), 3)})
routine = production["feature/routines/impl/src/commonMain/kotlin/features/routines/presentation/RoutineEditorViewModel.kt"].splitlines()
plan = production["feature/trainingplans/impl/src/commonMain/kotlin/features/trainingplans/presentation/PlanEditorViewModel.kt"].splitlines()
matching = sum(block.size for block in SequenceMatcher(None, routine, plan, autojunk=False).get_matching_blocks())
print(json.dumps({
    "production_kt_files": len(production),
    "designsystem_files": len([p for p in production if p.startswith("designsystem/")]),
    "material_import_files_outside_nucleus": sorted(p for p in material if "/designsystem/nucleus/" not in p),
    "editor_vm_exact_matching_lines_including_blank_imports": matching,
    "editor_vm_total_lines": [len(routine), len(plan)],
    "contrasts": ratios,
}, ensure_ascii=False, indent=2))
