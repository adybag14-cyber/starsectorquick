#!/usr/bin/env python3
"""Pack the deployed Starsector graphics tree into lazy top-level Pages payloads."""
from __future__ import annotations

import argparse
import hashlib
import json
import mimetypes
import re
import shutil
from collections import defaultdict
from pathlib import Path

GRAPHICS_ROOT = Path("starsector/starsector/graphics")
PACK_DIR = Path("starsector-graphics-pack-v1")
INDEX_NAME = "starsector-graphics-pack-v1.json"

EXTRA_MIME = {
    ".fnt": "text/plain; charset=utf-8",
    ".list": "text/plain; charset=utf-8",
}


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def mime_for(path: Path) -> str:
    suffix = path.suffix.lower()
    if suffix in EXTRA_MIME:
        return EXTRA_MIME[suffix]
    guessed, _ = mimetypes.guess_type(path.name)
    return guessed or "application/octet-stream"


def pack_key(path: Path, graphics_root: Path) -> str:
    rel = path.relative_to(graphics_root)
    raw = rel.parts[0] if len(rel.parts) > 1 else "_root"
    key = re.sub(r"[^A-Za-z0-9._-]+", "_", raw)
    if not key:
        raise RuntimeError(f"unsafe graphics pack key for {rel}")
    return key


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", default=".", help="final Pages tree to pack")
    args = parser.parse_args()

    root = Path(args.root).resolve()
    graphics_root = root / GRAPHICS_ROOT
    if not graphics_root.is_dir():
        raise RuntimeError(f"missing Starsector graphics tree: {graphics_root}")

    files = sorted(
        (p for p in graphics_root.rglob("*") if p.is_file()),
        key=lambda p: p.relative_to(graphics_root).as_posix(),
    )
    if not files:
        raise RuntimeError(f"refusing to create empty graphics packs from {graphics_root}")

    groups: dict[str, list[Path]] = defaultdict(list)
    for path in files:
        groups[pack_key(path, graphics_root)].append(path)

    pack_dir = root / PACK_DIR
    if pack_dir.exists():
        if not pack_dir.is_dir():
            raise RuntimeError(f"graphics pack path is not a directory: {pack_dir}")
        shutil.rmtree(pack_dir)
    pack_dir.mkdir(parents=True)

    file_entries: dict[str, dict[str, object]] = {}
    pack_entries: dict[str, dict[str, object]] = {}
    total_bytes = 0

    for key in sorted(groups):
        pack_name = f"{key}.bin"
        pack_path = pack_dir / pack_name
        offset = 0
        digest = hashlib.sha256()
        with pack_path.open("wb") as out:
            for path in groups[key]:
                rel = path.relative_to(graphics_root).as_posix()
                payload = path.read_bytes()
                file_entries[rel] = {
                    "pack": pack_name,
                    "offset": offset,
                    "length": len(payload),
                    "type": mime_for(path),
                }
                out.write(payload)
                digest.update(payload)
                offset += len(payload)
        if pack_path.stat().st_size != offset:
            raise RuntimeError(f"graphics pack size validation failed: {pack_name}")
        pack_entries[pack_name] = {
            "bytes": offset,
            "sha256": digest.hexdigest(),
            "fileCount": len(groups[key]),
        }
        total_bytes += offset

    manifest = {
        "version": 1,
        "graphicsRoot": GRAPHICS_ROOT.as_posix() + "/",
        "packDir": PACK_DIR.as_posix() + "/",
        "fileCount": len(file_entries),
        "packCount": len(pack_entries),
        "bytes": total_bytes,
        "packs": pack_entries,
        "files": file_entries,
    }
    index_path = root / INDEX_NAME
    index_path.write_bytes((json.dumps(manifest, separators=(",", ":")) + "\n").encode("utf-8"))

    parsed = json.loads(index_path.read_text(encoding="utf-8"))
    if parsed["fileCount"] != len(files) or parsed["packCount"] != len(groups):
        raise RuntimeError("graphics pack manifest validation failed")

    profile_path = root / "web-runtime-profile.json"
    runtime_manifest_path = root / "web-runtime-manifest.json"
    if profile_path.is_file() and runtime_manifest_path.is_file():
        profile = json.loads(profile_path.read_text(encoding="utf-8"))
        runtime_manifest = json.loads(runtime_manifest_path.read_text(encoding="utf-8"))
        runtime_files = [
            entry for entry in runtime_manifest.get("files", [])
            if entry.get("path") != INDEX_NAME and not str(entry.get("path", "")).startswith(PACK_DIR.as_posix() + "/")
        ]
        generated = [index_path, *sorted(pack_dir.glob("*.bin"))]
        for path in generated:
            rel = path.relative_to(root).as_posix()
            runtime_files.append({"path": rel, "bytes": path.stat().st_size, "sha256": sha256_file(path)})
        runtime_files.sort(key=lambda entry: str(entry.get("path", "")))
        profile["graphicsPack"] = {
            "version": 1,
            "fileCount": len(files),
            "packCount": len(pack_entries),
            "bytes": total_bytes,
        }
        profile["overlayFiles"] = len(runtime_files)
        runtime_manifest["profile"] = profile
        runtime_manifest["files"] = runtime_files
        profile_path.write_bytes((json.dumps(profile, indent=2) + "\n").encode("utf-8"))
        runtime_manifest_path.write_bytes((json.dumps(runtime_manifest, indent=2) + "\n").encode("utf-8"))

    print(
        f"Pages graphics packs prepared files={len(files)} packs={len(pack_entries)} "
        f"bytes={total_bytes} indexBytes={index_path.stat().st_size}"
    )
    for name, entry in sorted(pack_entries.items(), key=lambda item: -int(item[1]["bytes"])):
        print(f"  {name}: files={entry['fileCount']} bytes={entry['bytes']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
