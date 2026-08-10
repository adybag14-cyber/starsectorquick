#!/usr/bin/env python3
"""Pack the deployed Starsector data tree into one immutable Pages payload."""
from __future__ import annotations

import argparse
import hashlib
import json
import mimetypes
from pathlib import Path

DATA_ROOT = Path("starsector/starsector/data")
PACK_NAME = "starsector-data-pack-v1.bin"
INDEX_NAME = "starsector-data-pack-v1.json"

def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


EXTRA_MIME = {
    ".csv": "text/csv; charset=utf-8",
    ".faction": "text/plain; charset=utf-8",
    ".java": "text/plain; charset=utf-8",
    ".list": "text/plain; charset=utf-8",
    ".proj": "text/plain; charset=utf-8",
    ".sample": "application/octet-stream",
    ".ship": "text/plain; charset=utf-8",
    ".skill": "text/plain; charset=utf-8",
    ".skin": "text/plain; charset=utf-8",
    ".system": "text/plain; charset=utf-8",
    ".variant": "text/plain; charset=utf-8",
    ".wpn": "text/plain; charset=utf-8",
}


def mime_for(path: Path) -> str:
    suffix = path.suffix.lower()
    if suffix in EXTRA_MIME:
        return EXTRA_MIME[suffix]
    guessed, _ = mimetypes.guess_type(path.name)
    return guessed or "application/octet-stream"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", default=".", help="final Pages tree to pack")
    args = parser.parse_args()

    root = Path(args.root).resolve()
    data_root = root / DATA_ROOT
    if not data_root.is_dir():
        raise RuntimeError(f"missing Starsector data tree: {data_root}")

    files = sorted((p for p in data_root.rglob("*") if p.is_file()), key=lambda p: p.relative_to(data_root).as_posix())
    if not files:
        raise RuntimeError(f"refusing to create empty data pack from {data_root}")

    pack_path = root / PACK_NAME
    index_path = root / INDEX_NAME
    entries: dict[str, dict[str, object]] = {}
    offset = 0
    pack_hash = hashlib.sha256()

    with pack_path.open("wb") as out:
        for path in files:
            rel = path.relative_to(data_root).as_posix()
            payload = path.read_bytes()
            if rel in entries:
                raise RuntimeError(f"duplicate data-pack path: {rel}")
            entries[rel] = {
                "offset": offset,
                "length": len(payload),
                "type": mime_for(path),
            }
            out.write(payload)
            pack_hash.update(payload)
            offset += len(payload)

    manifest = {
        "version": 1,
        "dataRoot": DATA_ROOT.as_posix() + "/",
        "pack": PACK_NAME,
        "bytes": offset,
        "sha256": pack_hash.hexdigest(),
        "fileCount": len(entries),
        "files": entries,
    }
    index_path.write_bytes((json.dumps(manifest, separators=(",", ":")) + "\n").encode("utf-8"))

    if pack_path.stat().st_size != offset:
        raise RuntimeError("data-pack size validation failed")
    parsed = json.loads(index_path.read_text(encoding="utf-8"))
    if parsed["fileCount"] != len(files) or parsed["bytes"] != offset:
        raise RuntimeError("data-pack manifest validation failed")

    profile_path = root / "web-runtime-profile.json"
    runtime_manifest_path = root / "web-runtime-manifest.json"
    if profile_path.is_file() and runtime_manifest_path.is_file():
        profile = json.loads(profile_path.read_text(encoding="utf-8"))
        runtime_manifest = json.loads(runtime_manifest_path.read_text(encoding="utf-8"))
        runtime_files = [
            entry for entry in runtime_manifest.get("files", [])
            if entry.get("path") not in {PACK_NAME, INDEX_NAME}
        ]
        runtime_files.extend([
            {"path": PACK_NAME, "bytes": pack_path.stat().st_size, "sha256": sha256_file(pack_path)},
            {"path": INDEX_NAME, "bytes": index_path.stat().st_size, "sha256": sha256_file(index_path)},
        ])
        runtime_files.sort(key=lambda entry: str(entry.get("path", "")))
        profile["dataPack"] = {
            "version": 1,
            "fileCount": len(files),
            "bytes": offset,
            "sha256": pack_hash.hexdigest(),
        }
        profile["overlayFiles"] = len(runtime_files)
        runtime_manifest["profile"] = profile
        runtime_manifest["files"] = runtime_files
        profile_path.write_bytes((json.dumps(profile, indent=2) + "\n").encode("utf-8"))
        runtime_manifest_path.write_bytes(
            (json.dumps(runtime_manifest, indent=2) + "\n").encode("utf-8")
        )

    print(
        f"Pages data pack prepared files={len(files)} bytes={offset} "
        f"indexBytes={index_path.stat().st_size} sha256={pack_hash.hexdigest()}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
