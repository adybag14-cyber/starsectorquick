#!/usr/bin/env python3
"""Replace only explicitly reviewed stock-image deviations, guarded by both hashes.

The allowlist contains hashes, not bundled game assets. An unrecognised local
edit or a different official image fails before any image is replaced. Missing
files are still handled by restore-official-runtime-assets.py. This operation
never rewrites arbitrary browser adaptations or aliases.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import tempfile
from zipfile import ZipFile


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def relative_graphic(value: str) -> str:
    if not isinstance(value, str): raise ValueError('Image path must be a string')
    p = PurePosixPath(value)
    if (not value.startswith('graphics/') or p.is_absolute() or '..' in p.parts
            or '\\' in value or ':' in value or p.as_posix() != value
            or p.suffix.lower() not in {'.png', '.jpg', '.jpeg'}):
        raise ValueError(f'Unsafe/non-image repair path: {value}')
    return value


def repair(root: Path, archive_path: Path, manifest_path: Path, dry_run: bool = False) -> dict:
    manifest = json.loads(manifest_path.read_text(encoding='utf-8'))
    if manifest.get('schemaVersion') != 1 or not isinstance(manifest.get('images'), list):
        raise ValueError('Unsupported repair manifest')
    report = {'schemaVersion': 1, 'release': manifest.get('release'), 'dryRun': dry_run, 'images': []}
    plan = []
    seen = set()
    with ZipFile(archive_path) as archive:
        for entry in manifest['images']:
            name = relative_graphic(entry['path'])
            if name in seen: raise ValueError(f'Duplicate repair: {name}')
            seen.add(name)
            for field in ['expectedLocalSha256', 'officialSha256']:
                value = entry.get(field, '')
                if len(value) != 64 or any(c not in '0123456789abcdef' for c in value):
                    raise ValueError(f'Invalid {field}: {name}')
            candidates = [item for item in archive.infolist() if not item.is_dir()
                and (item.filename == name or item.filename.endswith('/'+name))]
            if len(candidates) != 1: raise ValueError(f'Ambiguous/missing official image: {name}')
            official = archive.read(candidates[0])
            if sha256(official) != entry['officialSha256']:
                raise ValueError(f'Official image hash mismatch: {name}')
            destination = root / name
            if not destination.resolve().is_relative_to(root.resolve()):
                raise ValueError(f'Repair would escape runtime root: {name}')
            if destination.is_symlink(): raise ValueError(f'Repair target is a symlink: {name}')
            before = destination.read_bytes() if destination.is_file() else None
            before_hash = sha256(before) if before is not None else None
            if before_hash not in {None, entry['expectedLocalSha256'], entry['officialSha256']}:
                raise ValueError(f'Unreviewed local image change: {name}; refusing replacement')
            action = 'already-official' if before_hash == entry['officialSha256'] else 'missing' if before is None else 'repair'
            row = {'path': name, 'action': action, 'beforeSha256': before_hash,
                'officialSha256': entry['officialSha256'], 'officialBytes': len(official), 'reason': entry.get('reason')}
            report['images'].append(row)
            if action == 'repair': plan.append((destination, before_hash, official))
        # Validate the complete manifest and all local/official hashes first.
        # Pre-existing omissions are reported, not created by this repair phase.
        if not dry_run:
            for destination, before_hash, official in plan:
                if sha256(destination.read_bytes()) != before_hash:
                    raise ValueError(f'Local image changed after validation: {destination}')
                temporary = None
                try:
                    with tempfile.NamedTemporaryFile(dir=destination.parent, prefix='.'+destination.name+'.', delete=False) as file:
                        temporary = Path(file.name); file.write(official); file.flush(); os.fsync(file.fileno())
                    temporary.replace(destination)
                finally:
                    if temporary is not None and temporary.exists(): temporary.unlink()
    report['plannedRepairs'] = sum(row['action'] == 'repair' for row in report['images'])
    report['repaired'] = 0 if dry_run else report['plannedRepairs']
    report['alreadyOfficial'] = sum(row['action'] == 'already-official' for row in report['images'])
    report['missing'] = sum(row['action'] == 'missing' for row in report['images'])
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path('starsector/starsector'))
    parser.add_argument('--archive', type=Path, required=True)
    parser.add_argument('--manifest', type=Path, default=Path('ci/official-graphics-repairs.json'))
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--dry-run', action='store_true')
    args = parser.parse_args()
    report = repair(args.root, args.archive, args.manifest, args.dry_run)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2)+'\n', encoding='utf-8')
    print('Official graphics repair:', json.dumps({k: report[k] for k in ['dryRun','repaired','alreadyOfficial','missing']}))
    return 0


if __name__ == '__main__': raise SystemExit(main())
