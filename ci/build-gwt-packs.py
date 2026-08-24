#!/usr/bin/env python3
"""Build deterministic Starsector GWT asset packs from a prepared Pages tree."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path


ALIASES = {
    "graphics/arial20bold.fnt": "graphics/fonts/arial20bold.fnt",
    "graphics/particlealpha32sq.png": "graphics/fx/particlealpha32sq.png",
    "graphics/particleline32ln.png": "graphics/fx/particleline32ln.png",
    "graphics/particleline32sq.png": "graphics/fx/particleline32ln.png",
    "graphics/test/res/arial20bold.fnt": "graphics/fonts/arial20bold.fnt",
}


def source_file(root: Path, runtime_path: str) -> Path:
    resolved_path = ALIASES.get(runtime_path, runtime_path)
    candidates = (
        root / "starsector" / "starsector" / resolved_path,
        root / "starsector" / resolved_path,
        root / resolved_path,
    )
    for candidate in candidates:
        if candidate.is_file():
            return candidate
    raise FileNotFoundError(
        f"GWT asset is missing: {runtime_path} (resolved as {resolved_path})"
    )


def source_directory(root: Path, runtime_path: str) -> Path:
    candidates = (
        root / "starsector" / "starsector" / runtime_path,
        root / "starsector" / runtime_path,
        root / runtime_path,
    )
    for candidate in candidates:
        if candidate.is_dir():
            return candidate
    raise FileNotFoundError(f"GWT asset directory is missing: {runtime_path}")


def payload_for(root: Path, runtime_path: str) -> bytes:
    try:
        return source_file(root, runtime_path).read_bytes()
    except FileNotFoundError:
        if not runtime_path.endswith("/index.list") and runtime_path != "graphics/index.list":
            raise
        parent = runtime_path.rsplit("/", 1)[0]
        directory = source_directory(root, parent)
        rows: list[str] = []
        children = sorted(directory.iterdir(), key=lambda path: path.name.lower())
        for child in children:
            if child.name == "index.list":
                continue
            if child.is_dir():
                rows.append(f"{child.name}/\t-1")
            elif child.is_file():
                rows.append(f"{child.name}\t{child.stat().st_size}")
        return (("\n".join(rows) + "\n") if rows else "").encode("utf-8")


def build_pack(root: Path, allow_manifest: Path, output_dir: Path, output_stem: str) -> dict:
    allow = json.loads(allow_manifest.read_text(encoding="utf-8"))
    paths = list((allow.get("files") or {}).keys())
    if not paths:
        raise RuntimeError(f"No files listed in {allow_manifest}")

    output_dir.mkdir(parents=True, exist_ok=True)
    pack_path = output_dir / f"{output_stem}.bin"
    manifest_path = output_dir / f"{output_stem}.json"
    temp_pack = pack_path.with_suffix(pack_path.suffix + ".tmp")
    digest = hashlib.sha256()
    entries: dict[str, dict[str, int]] = {}
    offset = 0

    with temp_pack.open("wb") as packed:
        for runtime_path in paths:
            payload = payload_for(root, runtime_path)
            packed.write(payload)
            digest.update(payload)
            entries[runtime_path] = {"offset": offset, "length": len(payload)}
            offset += len(payload)

    os.replace(temp_pack, pack_path)
    if pack_path.stat().st_size >= 100 * 1024 * 1024:
        raise RuntimeError(
            f"{pack_path} is {pack_path.stat().st_size} bytes and exceeds GitHub's 100 MiB file limit"
        )

    manifest = {
        "version": 1,
        "pack": pack_path.name,
        "bytes": offset,
        "fileCount": len(entries),
        "sha256": digest.hexdigest(),
        "files": entries,
    }
    manifest_path.write_text(
        json.dumps(manifest, separators=(",", ":"), ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
    print(
        f"{output_stem}: files={len(entries)} bytes={offset} sha256={manifest['sha256']}"
    )
    return manifest


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", required=True, type=Path, help="Prepared Pages root")
    parser.add_argument(
        "--runtime-source",
        default=Path("gwt-runtime"),
        type=Path,
        help="Directory containing the validated GWT file allow-lists",
    )
    args = parser.parse_args()

    root = args.root.resolve()
    runtime_source = args.runtime_source.resolve()
    output_dir = root / "gwt"
    data = build_pack(
        root,
        runtime_source / "gwt-data-files-v4.json",
        output_dir,
        "gwt-data-pack-v4",
    )
    graphics = build_pack(
        root,
        runtime_source / "gwt-graphics-files-v4.json",
        output_dir,
        "gwt-graphics-pack-v4",
    )
    if data["fileCount"] != 1400:
        raise RuntimeError(f"Unexpected GWT data file count: {data['fileCount']}")
    if graphics["fileCount"] != 3383:
        raise RuntimeError(f"Unexpected GWT graphics file count: {graphics['fileCount']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
