#!/usr/bin/env python3
"""Pack runtime JAR byte streams into one immutable Pages payload.

The service worker reconstructs the original individual JAR URLs and byte-range
responses from this pack, preserving CheerpJ classpath semantics while avoiding
latency-bound network range requests for every class/resource probe.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

PACK_NAME = "starsector-jar-pack-v1.bin"
INDEX_NAME = "starsector-jar-pack-v1.json"


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def parse_index(index_path: Path) -> list[str]:
    jars: list[str] = []
    seen: set[str] = set()
    for raw in index_path.read_text(encoding="utf-8-sig").splitlines():
        line = raw.strip()
        if not line:
            continue
        name = line.split()[0]
        rel = Path(name)
        if rel.is_absolute() or ".." in rel.parts or rel.suffix.lower() != ".jar":
            raise RuntimeError(f"unsafe JAR index entry: {name!r}")
        normalized = rel.as_posix()
        if normalized in seen:
            raise RuntimeError(f"duplicate JAR index entry: {normalized}")
        seen.add(normalized)
        jars.append(normalized)
    if not jars:
        raise RuntimeError(f"refusing to create empty JAR pack from {index_path}")
    return jars


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", default=".")
    parser.add_argument("--output-dir", default="")
    args = parser.parse_args()

    root = Path(args.root).resolve()
    jars_root = root / "jars"
    index_path = jars_root / "index.list"
    if not index_path.is_file():
        raise RuntimeError(f"missing runtime JAR index: {index_path}")

    output_dir = Path(args.output_dir).resolve() if args.output_dir else root
    output_dir.mkdir(parents=True, exist_ok=True)
    pack_path = output_dir / PACK_NAME
    manifest_path = output_dir / INDEX_NAME

    ordered = parse_index(index_path)
    entries: dict[str, dict[str, object]] = {}
    offset = 0
    with pack_path.open("wb") as out:
        for name in ordered:
            jar_path = jars_root / name
            if not jar_path.is_file():
                raise RuntimeError(f"runtime JAR listed but missing: {jar_path}")
            data = jar_path.read_bytes()
            if not data:
                raise RuntimeError(f"runtime JAR is empty: {jar_path}")
            if not data.startswith(b"PK"):
                raise RuntimeError(f"runtime JAR is not a ZIP/JAR payload: {jar_path}")
            out.write(data)
            entries[name] = {
                "offset": offset,
                "length": len(data),
                "sha256": sha256_bytes(data),
                "type": "application/java-archive",
            }
            offset += len(data)

    manifest = {"version": 1, "bytes": offset, "order": ordered, "jars": entries}
    manifest_path.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8", newline="\n")

    actual_size = pack_path.stat().st_size
    if actual_size != offset:
        raise RuntimeError(f"JAR pack size mismatch expected={offset} actual={actual_size}")
    with pack_path.open("rb") as body:
        for name in ordered:
            entry = entries[name]
            body.seek(int(entry["offset"]))
            remaining = int(entry["length"])
            entry_digest = hashlib.sha256()
            while remaining:
                chunk = body.read(min(remaining, 1 << 20))
                if not chunk:
                    raise RuntimeError(f"JAR pack truncated: {name}")
                entry_digest.update(chunk)
                remaining -= len(chunk)
            if entry_digest.hexdigest() != entry["sha256"]:
                raise RuntimeError(f"JAR pack reconstruction mismatch: {name}")

        body.seek(0)
        pack_digest = hashlib.sha256()
        for chunk in iter(lambda: body.read(1 << 20), b""):
            pack_digest.update(chunk)

    print(f"Pages JAR pack prepared jars={len(ordered)} bytes={offset} sha256={pack_digest.hexdigest()} output={pack_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
