#!/usr/bin/env python3
"""Normalize Starsector web runtime asset manifests before CheerpJ starts.

The source archive contains UTF-8 BOM bytes at the beginning of a number of
index.list entries. The game treats U+FEFF as part of the path and requests
URLs such as ``%EF%BB%BFproj/index.list`` or
``%EF%BB%BFlasher_Assault.variant``. Those requests 404, leaving weapon and
projectile specs partially initialized and causing ResourceLoaderState to
abort before the render loop can run.

A few cosmetic aliases remain as a fallback for deliberately curated trees.
They are only created when the exact destination is absent, so assets restored
from the official release are never overwritten.
"""

from __future__ import annotations

import argparse
import re
import shutil
from pathlib import Path


GRAPHICS_PATH_RE = re.compile(
    r"graphics/[A-Za-z0-9_./@+-]+\.(?:png|jpg|jpeg)",
    re.IGNORECASE,
)
TEXT_ASSET_EXTENSIONS = {
    ".csv",
    ".json",
    ".proj",
    ".ship",
    ".skin",
    ".system",
    ".variant",
    ".wpn",
}


def sanitize_index(path: Path) -> tuple[bool, int]:
    raw = path.read_bytes()
    # utf-8-sig removes a file-leading BOM. lstrip below also removes BOMs
    # embedded at the beginning of individual lines after concatenation.
    text = raw.decode("utf-8-sig")
    normalized = text.replace("\r\n", "\n").replace("\r", "\n")
    fixed_entries = 0
    clean_lines: list[str] = []
    for line in normalized.split("\n"):
        clean = line.lstrip("\ufeff")
        if clean != line:
            fixed_entries += 1
        clean_lines.append(clean)

    clean_text = "\n".join(clean_lines)
    clean_bytes = clean_text.encode("utf-8")
    if clean_bytes == raw:
        return False, fixed_entries
    path.write_bytes(clean_bytes)
    return True, fixed_entries


def copy_alias(root: Path, source: str, destination: str) -> bool:
    src = root / source
    dst = root / destination
    if not src.is_file():
        raise FileNotFoundError(f"required alias source is missing: {src}")
    if dst.is_file():
        return False
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(src, dst)
    return True


def discover_missing_static_graphics(root: Path) -> dict[str, list[str]]:
    """Return missing literal graphics paths and a few files referencing each.

    This is diagnostic rather than a hard gate: a handful of scripts use
    generated or optional paths. Printing the complete set lets CI expose the
    next curated-asset omission in one run instead of one fatal startup at a
    time.
    """

    missing: dict[str, list[str]] = {}
    for source in root.rglob("*"):
        if not source.is_file() or source.suffix.lower() not in TEXT_ASSET_EXTENSIONS:
            continue
        try:
            text = source.read_text(encoding="utf-8-sig", errors="ignore")
        except OSError:
            continue
        for match in GRAPHICS_PATH_RE.finditer(text):
            asset = match.group(0).replace("\\", "/")
            if (root / asset).is_file():
                continue
            refs = missing.setdefault(asset, [])
            if len(refs) < 3:
                refs.append(source.relative_to(root).as_posix())
    return dict(sorted(missing.items()))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--root",
        default="starsector/starsector",
        help="Starsector data root",
    )
    parser.add_argument(
        "--check",
        action="store_true",
        help="Fail instead of modifying files",
    )
    args = parser.parse_args()

    root = Path(args.root)
    if not root.is_dir():
        raise SystemExit(f"runtime root does not exist: {root}")

    changed: list[Path] = []
    embedded_bom_entries = 0
    index_files = sorted(root.rglob("index.list"))
    for index_path in index_files:
        raw = index_path.read_bytes()
        text = raw.decode("utf-8-sig")
        normalized = text.replace("\r\n", "\n").replace("\r", "\n")
        count = sum(1 for line in normalized.split("\n") if line.startswith("\ufeff"))
        would_change = raw != "\n".join(
            line.lstrip("\ufeff") for line in normalized.split("\n")
        ).encode("utf-8")
        embedded_bom_entries += count
        if would_change:
            changed.append(index_path)
            if not args.check:
                sanitize_index(index_path)

    aliases = [
        ("graphics/fx/particlealpha32sq.png", "graphics/particlealpha32sq.png"),
        # Cosmetic weapon-skin fallbacks for reduced asset trees. Exact official
        # variants win whenever they are present.
        (
            "graphics/weapons/blaster2_turret_base.png",
            "graphics/weapons/blaster2ht_turret_base.png",
        ),
        (
            "graphics/weapons/mining_laser_hardpoint_base.png",
            "graphics/weapons/mining_laser_hightech_hardpoint_base.png",
        ),
        (
            "graphics/weapons/mining_laser_hardpoint_glow.png",
            "graphics/weapons/mining_laser_hightech_hardpoint_glow.png",
        ),
        (
            "graphics/weapons/mining_laser_turret_base.png",
            "graphics/weapons/mining_laser_hightech_turret_base.png",
        ),
        (
            "graphics/weapons/mining_laser_turret_glow.png",
            "graphics/weapons/mining_laser_hightech_turret_glow.png",
        ),
    ]
    alias_changes: list[str] = []
    for source, destination in aliases:
        dst = root / destination
        needs_copy = not dst.is_file()
        if needs_copy:
            alias_changes.append(destination)
            if not args.check:
                copy_alias(root, source, destination)

    missing_graphics = discover_missing_static_graphics(root)
    print(
        "runtime asset sanitation: "
        f"indexes={len(index_files)} changed={len(changed)} "
        f"embeddedBomEntries={embedded_bom_entries} aliases={len(alias_changes)} "
        f"missingStaticGraphics={len(missing_graphics)}"
    )
    for path in changed[:80]:
        print(f"  normalized {path.as_posix()}")
    if len(changed) > 80:
        print(f"  ... and {len(changed) - 80} more index files")
    for destination in alias_changes:
        print(f"  aliased {destination}")
    for asset, refs in list(missing_graphics.items())[:200]:
        print(f"  missing-static {asset} <- {', '.join(refs)}")
    if len(missing_graphics) > 200:
        print(f"  ... and {len(missing_graphics) - 200} more missing static graphics paths")

    if args.check and (changed or alias_changes):
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
