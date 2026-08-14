#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
index = ROOT / 'jars' / 'index.list'
tracked = {
    'fixer_patch.jar',
    'fs.common_obf.jar',
    'starfarer.api.jar',
    'starfarer_obf.jar',
    'scripts-precompiled.jar',
}

lines = []
seen = set()
for raw in index.read_text(encoding='utf-8-sig').splitlines():
    raw = raw.strip()
    if not raw:
        continue
    name = raw.split()[0]
    if name in tracked:
        path = ROOT / 'jars' / name
        if not path.is_file():
            raise RuntimeError(f'missing transformed runtime JAR: {name}')
        lines.append(f'{name}\t{path.stat().st_size}')
        seen.add(name)
    else:
        lines.append(raw)

if 'fixer_patch.jar' not in seen:
    path = ROOT / 'jars' / 'fixer_patch.jar'
    if not path.is_file():
        raise RuntimeError('missing fixer_patch.jar')
    lines.insert(0, f'fixer_patch.jar\t{path.stat().st_size}')
    seen.add('fixer_patch.jar')

missing = sorted((tracked - {'fixer_patch.jar'}) - seen)
if missing:
    raise RuntimeError('runtime JARs missing from jars/index.list: ' + ', '.join(missing))

index.write_text('\n'.join(lines) + '\n', encoding='utf-8', newline='\n')
print('Refreshed runtime JAR index sizes: ' + ', '.join(sorted(tracked)))
