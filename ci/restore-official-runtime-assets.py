#!/usr/bin/env python3
"""Fill omitted browser runtime assets from the official Starsector archive.

The repository intentionally contains a curated asset tree, but the Java 17
0.98a-RC8 data files reference several hundred graphics that were not copied
into that tree. ResourceLoaderState treats a missing required texture as
fatal. This script copies only files that are absent locally, preserving all
existing browser-specific assets and edits.
"""

from __future__ import annotations

import argparse
import shutil
import tempfile
from collections import Counter
from pathlib import Path, PurePosixPath
from zipfile import ZipFile, ZipInfo


def find_section_prefix(zf: ZipFile, section: str) -> str:
    marker = f"/{section}/"
    prefixes: Counter[str] = Counter()
    for info in zf.infolist():
        name = info.filename.replace("\\", "/")
        if info.is_dir():
            continue
        lower = name.lower()
        index = lower.find(marker)
        if index >= 0:
            prefixes[name[: index + 1]] += 1
        elif lower.startswith(f"{section}/"):
            prefixes[""] += 1
    if not prefixes:
        raise RuntimeError(f"official archive contains no {section}/ files")
    return prefixes.most_common(1)[0][0]


def safe_relative(name: str, prefix: str, section: str) -> PurePosixPath | None:
    normalized = name.replace("\\", "/")
    if not normalized.startswith(prefix):
        return None
    relative = PurePosixPath(normalized[len(prefix) :])
    if not relative.parts or relative.parts[0].lower() != section.lower():
        return None
    if any(part in {"", ".", ".."} for part in relative.parts):
        return None
    return relative


def copy_missing_section(
    zf: ZipFile,
    target_root: Path,
    section: str,
    dry_run: bool,
) -> tuple[int, int, int]:
    prefix = find_section_prefix(zf, section)
    copied = 0
    copied_bytes = 0
    existing = 0
    print(f"official asset section={section} archivePrefix={prefix!r}")

    for info in zf.infolist():
        if info.is_dir():
            continue
        relative = safe_relative(info.filename, prefix, section)
        if relative is None:
            continue
        destination = target_root.joinpath(*relative.parts)
        if destination.is_file():
            existing += 1
            continue
        copied += 1
        copied_bytes += info.file_size
        print(f"  restore {relative.as_posix()} bytes={info.file_size}")
        if dry_run:
            continue
        destination.parent.mkdir(parents=True, exist_ok=True)
        with zf.open(info) as source, tempfile.NamedTemporaryFile(
            dir=destination.parent,
            prefix=f".{destination.name}.",
            delete=False,
        ) as temporary:
            shutil.copyfileobj(source, temporary)
            temporary_path = Path(temporary.name)
        temporary_path.replace(destination)

    return copied, copied_bytes, existing


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--archive", required=True, help="Official Linux release ZIP")
    parser.add_argument(
        "--root",
        default="starsector/starsector",
        help="Browser runtime root",
    )
    parser.add_argument(
        "--section",
        action="append",
        dest="sections",
        default=[],
        help="Top-level asset section to fill (repeatable; default: graphics)",
    )
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    archive = Path(args.archive)
    target_root = Path(args.root)
    sections = args.sections or ["graphics"]
    if not archive.is_file():
        raise SystemExit(f"official archive does not exist: {archive}")
    if not target_root.is_dir():
        raise SystemExit(f"runtime root does not exist: {target_root}")

    total_copied = 0
    total_bytes = 0
    total_existing = 0
    with ZipFile(archive) as zf:
        for section in sections:
            copied, copied_bytes, existing = copy_missing_section(
                zf,
                target_root,
                section,
                args.dry_run,
            )
            total_copied += copied
            total_bytes += copied_bytes
            total_existing += existing

    print(
        "official runtime asset restore: "
        f"sections={','.join(sections)} copied={total_copied} "
        f"copiedBytes={total_bytes} existing={total_existing} "
        f"dryRun={args.dry_run}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
