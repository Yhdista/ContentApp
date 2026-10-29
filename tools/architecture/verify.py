#!/usr/bin/env python3
"""Small, build-neutral boundary sensor; compiler and API review remain necessary.

Production Kotlin imports are mapped to their physical Gradle owners, even if an
implementation file declares an `api` package. Comments and literals are ignored.
Gradle supplies its actual declared dependencies via --inventory; the source
dependency reader also supports isolated negative fixtures without Gradle.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path
import re
import sys


ATOMIC_LAYERS = {name: rank for rank, name in enumerate(('nucleus', 'atoms', 'molecules', 'organisms', 'templates'))}


def atomic_layer(path: Path) -> str | None:
    """Physical Kotlin directory owns the layer, not the declared package."""
    parts = path.parts
    if 'kotlin' not in parts:
        return None
    return next((part for part in parts[parts.index('kotlin') + 1:] if part in ATOMIC_LAYERS), None)


def clean(text: str, literals: bool = True) -> str:
    """Mask Kotlin/Groovy comments (including nested blocks) and string literals."""
    out, i, n = [], 0, len(text)
    while i < n:
        start = i
        if text.startswith('//', i):
            i = text.find('\n', i)
            if i < 0:
                i = n
        elif text.startswith('/*', i):
            depth, i = 1, i + 2
            while i < n and depth:
                if text.startswith('/*', i):
                    depth, i = depth + 1, i + 2
                elif text.startswith('*/', i):
                    depth, i = depth - 1, i + 2
                else:
                    i += 1
        elif text[i] in ('"', "'"):
            quote = text[i] * (3 if text.startswith(text[i] * 3, i) else 1)
            i += len(quote)
            while i < n:
                if len(quote) == 1 and text[i] == '\\':
                    i += 2
                elif text.startswith(quote, i):
                    i += len(quote)
                    break
                else:
                    i += 1
            if not literals:
                out.append(text[start:i])
                continue
        else:
            out.append(text[i])
            i += 1
            continue
        out.append(''.join('\n' if c == '\n' else ' ' for c in text[start:i]))
    return ''.join(out)


def feature(module: str) -> str | None:
    match = re.fullmatch(r':feature:([^:]+):(api|impl)', module)
    return match[1] if match else None


def production(path: Path) -> bool:
    return 'src' in path.parts and any(p.endswith('Main') or p == 'main' for p in path.parts) and not any('Test' in p or p in ('test', 'androidTest') for p in path.parts)


def scan(root: Path, inventory: list[dict] | None = None) -> list[str]:
    builds = {p.parent: ':' + ':'.join(p.parent.relative_to(root).parts)
              for p in root.rglob('build.gradle.kts')
              if not any(x in ('build', '.gradle', 'build-logic', '.codegraph') for x in p.relative_to(root).parts)}
    builds[root] = ':'
    violations: list[str] = []

    def owner(path: Path) -> str:
        return next((builds[p] for p in path.parents if p in builds), ':')

    def reject(path: Path | str, rule: str, detail: str):
        label = path.relative_to(root).as_posix() if isinstance(path, Path) else path
        violations.append(f'{label}: {rule}: {detail}')

    def dependency(module: str, target: str, label: Path | str):
        if feature(module) and target.endswith(':impl') and feature(target) != feature(module):
            reject(label, 'foreign-feature-implementation', target)
        if module == ':foundation' and (target.startswith((':feature:', ':designsystem', ':core:')) or re.match(r'(androidx\.|org\.jetbrains\.compose|org\.koin|io\.insert-koin|io\.ktor)', target)):
            reject(label, 'foundation-purity', target)
        if module == ':designsystem' and (target.startswith((':feature:', ':foundation', ':core:', ':composeApp', ':androidApp'))):
            reject(label, 'designsystem-purity', target)
        if 'material3:material3' in target and module != ':designsystem':
            reject(label, 'material-ownership', target)

    for directory, module in builds.items():
        p = directory / 'build.gradle.kts'
        if not p.exists():
            continue
        text = clean(p.read_text(), literals=False)
        # Tokenize literals as units: text describing a project dependency in a
        # Gradle string is not itself a project dependency.
        tokens = re.findall(r'"""[\s\S]*?"""|\'\'\'[\s\S]*?\'\'\'|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|[A-Za-z_]\w*|[^\s]', text)
        for index in range(len(tokens) - 3):
            if tokens[index:index + 2] == ['project', '('] and tokens[index + 2].startswith(('"', "'")):
                target = tokens[index + 2][1:-1]
                if target.startswith(':'):
                    dependency(module, target, p)
        # Catalog aliases are checked in the real Gradle inventory. Direct
        # coordinates are also understood in standalone fixtures.
        for token in tokens:
            target = token[1:-1] if token.startswith(('"', "'")) else ''
            if re.fullmatch(r'(?:androidx\.|org\.jetbrains\.compose|io\.insert-koin|org\.koin|io\.ktor)[\w.\-]+:[\w.\-]+(?::[\w.\-]+)?', target):
                dependency(module, target, p)
    for edge in inventory or []:
        dependency(edge['module'], edge['target'], f"{edge['module']} [{edge['configuration']}]")

    sources = [(p, clean(p.read_text()), owner(p)) for p in root.rglob('*.kt')
               if production(p) and not any(x in ('build', '.gradle', '.codegraph') for x in p.relative_to(root).parts)]
    symbols: dict[str, set[str]] = {}
    packages: dict[str, set[str]] = {}
    atomic_symbols: dict[str, set[str]] = {}
    atomic_packages: dict[str, set[str]] = {}
    for path, text, module in sources:
        package = re.search(r'^\s*package\s+([\w.]+)', text, re.M)
        if not package:
            continue
        pkg = package[1]
        packages.setdefault(pkg, set()).add(module)
        layer = atomic_layer(path) if module == ':designsystem' else None
        if layer:
            atomic_packages.setdefault(pkg, set()).add(layer)
        for symbol in re.findall(r'\b(?:class|interface|object|typealias|fun|val)\s+([A-Za-z_]\w*)', text):
            symbols.setdefault(pkg + '.' + symbol, set()).add(module)
            if layer:
                atomic_symbols.setdefault(pkg + '.' + symbol, set()).add(layer)

    def referenced_layers(reference: str) -> set[str]:
        candidate = reference.removesuffix('.*')
        while candidate:
            if candidate in atomic_symbols:
                return atomic_symbols[candidate]
            if candidate in atomic_packages:
                return atomic_packages[candidate]
            candidate = candidate.rpartition('.')[0]
        # Also reject unresolved higher-layer imports; compilation checks their existence.
        match = re.match(r'ui\.designsystem\.(nucleus|atoms|molecules|organisms|templates)(?:\.|$)', reference)
        return {match[1]} if match else set()

    for path, text, module in sources:
        imports = re.findall(r'^\s*import\s+([\w.*]+)(?:\s+as\s+(\w+))?', text, re.M)
        # Expand imported package aliases before checking qualified references.
        expanded = text
        for imported, alias in imports:
            if alias:
                expanded = re.sub(r'\b' + re.escape(alias) + r'\.', imported + '.', expanded)
            for target in (packages.get(imported[:-2], set()) if imported.endswith('.*') else symbols.get(imported, set())):
                if feature(module) and feature(target) and feature(module) != feature(target) and target.endswith(':impl'):
                    reject(path, 'foreign-feature-implementation-import', imported + ' owned by ' + target)
                if module == ':foundation' and target != ':foundation':
                    reject(path, 'foundation-purity', imported + ' owned by ' + target)
            if module == ':foundation' and re.match(r'(androidx\.|org\.jetbrains\.compose\.|org\.koin\.|io\.ktor\.|features\.|ui\.|app\.|core\.(storage|viewmodel|presentation)\.)', imported):
                reject(path, 'foundation-purity', imported)
            if module == ':designsystem' and re.match(r'(features\.|core\.|app\.|androidx\.room\.|io\.ktor\.)', imported):
                reject(path, 'designsystem-purity', imported)
        # Match a platform root, never a later segment in legitimate shared
        # namespaces such as androidx.compose.ui.platform or core.platform.
        if 'commonMain' in path.parts and re.search(r'(?<![\w.])(?:android\.|java\.|javax\.|UIKit\.|Foundation\.|platform\.|kotlinx\.cinterop\.)', expanded):
            reject(path, 'common-platform-import', 'platform reference in commonMain')
        nucleus = module == ':designsystem' and 'nucleus' in path.relative_to(root).parts
        if not nucleus and re.search(r'\b(?:androidx|org\.jetbrains)\.compose\.material3\b', expanded):
            reject(path, 'material-ownership', 'Material3 belongs to designsystem/nucleus')
        layer = atomic_layer(path) if module == ':designsystem' else None
        if layer:
            references = re.sub(r'^\s*package\s+[\w.]+', '', expanded, flags=re.M)
            for reference in set(re.findall(r'(?<![\w.])[A-Za-z_]\w*(?:\.(?:[A-Za-z_]\w*|\*))+', references)):
                for target in referenced_layers(reference):
                    if ATOMIC_LAYERS[target] > ATOMIC_LAYERS[layer]:
                        reject(path, 'atomic-layer-direction', f'{layer} -> {target} via {reference}')
        ui = path.name.endswith(('Screen.kt', 'Root.kt', 'ViewModel.kt'))
        if ui:
            for imported, _ in imports:
                if re.match(r'(androidx\.room\.|io\.ktor\.|okhttp3\.|retrofit2\.|core\.network\.)', imported) or re.search(r'(?:\.dao\.|\.\w*(?:Dao|DAO)$)', imported):
                    reject(path, 'ui-storage-ownership', imported)
            if re.search(r'\b(?:WorkoutStore|ExerciseCatalogImport)\b', expanded):
                reject(path, 'ui-workflow-ownership', 'workflow-only integration participant')
            if path.name.endswith('ViewModel.kt') and re.search(r'\b(?:DraftStore|CommandCoordinator)\b', expanded):
                reject(path, 'viewmodel-publish-ownership', 'publish sequencing belongs to a controller/factory')
    return sorted(set(violations))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument('--inventory', type=Path)
    args = parser.parse_args()
    violations = scan(args.root.resolve(), json.loads(args.inventory.read_text()) if args.inventory else None)
    if violations:
        print('\n'.join(violations), file=sys.stderr)
        return 1
    print('Architecture boundaries: PASS')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
