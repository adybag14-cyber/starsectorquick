#!/usr/bin/env python3
"""Verify every published graphics range and compare all official image bytes."""
from __future__ import annotations
import argparse
from collections import defaultdict
import hashlib
import importlib.util
import json
from pathlib import Path, PurePosixPath
from zipfile import ZipFile

spec = importlib.util.spec_from_file_location('stock_graphics_audit', Path(__file__).with_name('audit-runtime-graphics.py'))
stock = importlib.util.module_from_spec(spec)
spec.loader.exec_module(stock)


def safe_relative(name: str) -> str:
    p = PurePosixPath(name)
    if not name or p.is_absolute() or '..' in p.parts or '\\' in name or ':' in name or p.as_posix() != name:
        raise ValueError(f'Unsafe pack path: {name}')
    return name


def audit(manifest_path: Path, pack_dir: Path, archive_path: Path) -> dict:
    manifest = json.loads(manifest_path.read_text(encoding='utf-8'))
    if manifest.get('version') != 1: raise ValueError('Unsupported graphics pack version')
    if manifest['fileCount'] != len(manifest['files']) or manifest['packCount'] != len(manifest['packs']):
        raise ValueError('Graphics manifest counts disagree')
    packs = {}
    for name, entry in manifest['packs'].items():
        safe_relative(name)
        source = pack_dir / name
        if not source.resolve().is_relative_to(pack_dir.resolve()): raise ValueError('Pack escapes directory')
        data = source.read_bytes()
        if len(data) != entry['bytes'] or hashlib.sha256(data).hexdigest() != entry['sha256']:
            raise ValueError(f'Pack hash/size mismatch: {name}')
        packs[name] = data
    if manifest['bytes'] != sum(map(len, packs.values())): raise ValueError('Total pack bytes disagree')
    ranges = defaultdict(list)
    for name, entry in manifest['files'].items():
        safe_relative(name)
        pack = entry['pack']
        if pack not in packs: raise ValueError(f'Missing referenced pack: {pack}')
        offset, length = entry['offset'], entry['length']
        if type(offset) is not int or type(length) is not int or offset < 0 or length < 0 or offset + length > len(packs[pack]):
            raise ValueError(f'Invalid image range: {name}')
        ranges[pack].append((offset, length, name))
    for name, data in packs.items():
        if len(ranges[name]) != manifest['packs'][name]['fileCount']: raise ValueError(f'Pack file count mismatch: {name}')
        cursor = 0
        for offset, length, image in sorted(ranges[name]):
            if offset != cursor: raise ValueError(f'Overlapping/gapped pack range: {image}')
            cursor += length
        if cursor != len(data): raise ValueError(f'Unreferenced trailing pack bytes: {name}')
    report = {'verifiedPacks': len(packs), 'verifiedPackBytes': manifest['bytes'], 'indexedFiles': len(manifest['files']),
        'officialImages': 0, 'exactOfficialImages': 0, 'missing': [], 'different': []}
    with ZipFile(archive_path) as archive:
        images = stock.archive_images(archive)
        report['officialImages'] = len(images)
        for name, item in sorted(images.items()):
            entry = manifest['files'].get(name.removeprefix('graphics/'))
            if entry is None:
                report['missing'].append(name)
                continue
            data = packs[entry['pack']][entry['offset']:entry['offset']+entry['length']]
            actual = hashlib.sha256(data).hexdigest()
            expected = hashlib.sha256(archive.read(item)).hexdigest()
            if actual == expected: report['exactOfficialImages'] += 1
            else: report['different'].append({'path':name,'packedSha256':actual,'officialSha256':expected})
    report['allOfficialImagesByteExact'] = not report['missing'] and not report['different']
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--manifest',type=Path,required=True)
    parser.add_argument('--pack-dir',type=Path,required=True)
    parser.add_argument('--archive',type=Path,required=True)
    parser.add_argument('--output',type=Path,required=True)
    parser.add_argument('--require-byte-exact',action='store_true')
    args = parser.parse_args()
    report = audit(args.manifest,args.pack_dir,args.archive)
    args.output.parent.mkdir(parents=True,exist_ok=True)
    args.output.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print('Graphics pack audit:',json.dumps({k:v for k,v in report.items() if k not in ['different','missing']}))
    return int(args.require_byte_exact and not report['allOfficialImagesByteExact'])


if __name__ == '__main__': raise SystemExit(main())
