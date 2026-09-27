#!/usr/bin/env python3
"""Read-only, case-sensitive coverage audit against an official release archive.

Coverage is not inferred from a successful boot. Literal source references are
reported separately because comments, optional content and generated names can
refer to files absent from the official release itself. No placeholder creation
or automatic replacement occurs here.
"""
from __future__ import annotations
import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path, PurePosixPath
import re
from zipfile import ZipFile

IMAGE_SUFFIXES = {'.png', '.jpg', '.jpeg', '.gif', '.bmp', '.tga'}
TEXT_SUFFIXES = {'.java', '.csv', '.json', '.ship', '.skin', '.wpn', '.proj', '.fnt', '.variant', '.system'}
REFERENCE = re.compile(r'graphics/[A-Za-z0-9_./@+ -]+?\.(?:png|jpg|jpeg|gif|bmp|tga)', re.I)
LFS_POINTER = b'version https://git-lfs.github.com/spec/v1'


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def image_signature_ok(name: str, data: bytes) -> bool:
    suffix = PurePosixPath(name).suffix.lower()
    if suffix == '.png': return data.startswith(b'\x89PNG\r\n\x1a\n')
    if suffix in {'.jpg', '.jpeg'}: return data.startswith(b'\xff\xd8\xff')
    if suffix == '.gif': return data.startswith((b'GIF87a', b'GIF89a'))
    if suffix == '.bmp': return data.startswith(b'BM')
    return bool(data)  # TGA has no mandatory leading magic; decoder tests cover it.


def archive_images(archive: ZipFile) -> dict:
    prefixes = Counter()
    for item in archive.infolist():
        name = item.filename.replace('\\', '/')
        if '/graphics/' in name and not item.is_dir():
            prefixes[name.split('/graphics/', 1)[0] + '/'] += 1
        elif name.startswith('graphics/') and not item.is_dir():
            prefixes[''] += 1
    if not prefixes: raise ValueError('Official archive contains no graphics directory')
    prefix = prefixes.most_common(1)[0][0]
    result = {}
    for item in archive.infolist():
        name = item.filename.replace('\\', '/')
        if not name.startswith(prefix + 'graphics/') or item.is_dir(): continue
        relative = name[len(prefix):]
        parts = PurePosixPath(relative).parts
        if '..' in parts or PurePosixPath(relative).is_absolute(): raise ValueError('Unsafe archive path')
        if PurePosixPath(relative).suffix.lower() not in IMAGE_SUFFIXES: continue
        if relative in result: raise ValueError(f'Duplicate official image {relative}')
        result[relative] = item
    return result


def audit(root: Path, archive_path: Path) -> dict:
    # A string set, not Path.exists(), makes Windows CI catch Linux case errors.
    local = {p.relative_to(root).as_posix(): p for p in (root/'graphics').rglob('*') if p.is_file()}
    casefold = {}
    for name in local: casefold.setdefault(name.casefold(), []).append(name)
    report = {'root': str(root), 'archive': str(archive_path), 'officialImages': 0,
              'exactImages': 0, 'missing': [], 'different': [], 'invalidImages': [],
              'caseMismatches': [], 'extraImages': [], 'referenceMisses': [], 'groups': {}}
    with ZipFile(archive_path) as archive:
        official = archive_images(archive)
        report['officialImages'] = len(official)
        groups = {}
        for name, entry in sorted(official.items()):
            group = '/'.join(PurePosixPath(name).parts[:2])
            counts = groups.setdefault(group, {'official': 0, 'exact': 0, 'missing': 0, 'different': 0})
            counts['official'] += 1
            if name not in local:
                report['missing'].append(name); counts['missing'] += 1
                if name.casefold() in casefold:
                    report['caseMismatches'].append({'official': name, 'local': casefold[name.casefold()]})
                continue
            raw = local[name].read_bytes()
            if raw.startswith(LFS_POINTER) or not image_signature_ok(name, raw):
                report['invalidImages'].append(name)
            expected = archive.read(entry)
            if digest(raw) == digest(expected):
                report['exactImages'] += 1; counts['exact'] += 1
            else:
                counts['different'] += 1
                report['different'].append({'path': name, 'localBytes': len(raw), 'officialBytes': len(expected),
                    'localSha256': digest(raw), 'officialSha256': digest(expected)})
        report['groups'] = groups
        report['extraImages'] = sorted(name for name in local
            if PurePosixPath(name).suffix.lower() in IMAGE_SUFFIXES and name not in official)
        references = {}
        data_root = root/'data'
        for source in sorted(data_root.rglob('*')):
            if not source.is_file() or source.suffix.lower() not in TEXT_SUFFIXES: continue
            text = source.read_text(encoding='utf-8-sig', errors='replace')
            for match in REFERENCE.finditer(text):
                name = match.group(0)
                if name in local: continue
                row = references.setdefault(name, {'path': name, 'existsInOfficialRelease': name in official,
                    'caseVariants': casefold.get(name.casefold(), []), 'references': []})
                if len(row['references']) < 5: row['references'].append(source.relative_to(root).as_posix())
        report['referenceMisses'] = [references[name] for name in sorted(references)]
    report['completeOfficialImageSet'] = not report['missing'] and not report['invalidImages']
    report['allOfficialImagesByteExact'] = report['completeOfficialImageSet'] and not report['different']
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path('starsector/starsector'))
    parser.add_argument('--archive', required=True, type=Path)
    parser.add_argument('--output', required=True, type=Path)
    parser.add_argument('--require-complete', action='store_true')
    args = parser.parse_args()
    if not (args.root/'graphics').is_dir(): parser.error('Runtime graphics directory does not exist')
    report = audit(args.root, args.archive)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2)+'\n', encoding='utf-8')
    print(json.dumps({key: report[key] for key in ['officialImages', 'exactImages', 'completeOfficialImageSet', 'allOfficialImagesByteExact']}))
    print('missing=%d different=%d invalid=%d literal-reference-misses=%d' % tuple(len(report[key])
        for key in ['missing', 'different', 'invalidImages', 'referenceMisses']))
    return 1 if args.require_complete and not report['completeOfficialImageSet'] else 0


if __name__ == '__main__': raise SystemExit(main())
