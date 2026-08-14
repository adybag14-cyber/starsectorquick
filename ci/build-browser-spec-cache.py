#!/usr/bin/env python3
"""Build the stock-browser bulk cache for small JSON-like gameplay spec files."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

EXTENSIONS = {".variant", ".ship", ".skin", ".wpn", ".proj", ".system", ".skill"}
VERSION = 1


def sha256_text(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", default="starsector/starsector")
    parser.add_argument("--output", default="data/browser-spec-cache-v1.json")
    args = parser.parse_args()

    root = Path(args.root).resolve()
    output = (root / args.output).resolve()
    data_root = (root / "data").resolve()
    if not data_root.is_dir():
        raise RuntimeError(f"data root missing: {data_root}")
    try:
        output.relative_to(root)
    except ValueError as exc:
        raise RuntimeError(f"output must stay under {root}: {output}") from exc

    files: dict[str, str] = {}
    source_bytes = 0
    source_digest = hashlib.sha256()
    by_extension: dict[str, int] = {}

    for path in sorted(p for p in data_root.rglob("*") if p.is_file() and p.suffix.lower() in EXTENSIONS):
        rel = path.relative_to(root).as_posix()
        raw = path.read_text(encoding="utf-8-sig")
        # LoadingUtils' normal InputStream path removes CR characters before parsing.
        raw = raw.replace("\r", "")
        if rel in files:
            raise RuntimeError(f"duplicate spec cache path: {rel}")
        files[rel] = raw
        encoded = raw.encode("utf-8")
        source_bytes += len(encoded)
        source_digest.update(rel.encode("utf-8"))
        source_digest.update(b"\0")
        source_digest.update(encoded)
        source_digest.update(b"\0")
        by_extension[path.suffix.lower()] = by_extension.get(path.suffix.lower(), 0) + 1

    if len(files) < 1000:
        raise RuntimeError(f"unexpectedly small spec cache: {len(files)} files")

    payload = {
        "version": VERSION,
        "fileCount": len(files),
        "sourceBytes": source_bytes,
        "sourceSha256": source_digest.hexdigest(),
        "extensions": {key: by_extension[key] for key in sorted(by_extension)},
        "files": files,
    }
    output.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":")) + "\n"
    output.write_text(text, encoding="utf-8", newline="\n")

    # Re-open and prove the output reconstructs every source string exactly.
    check = json.loads(output.read_text(encoding="utf-8"))
    if check.get("version") != VERSION or check.get("fileCount") != len(files):
        raise RuntimeError("spec cache metadata mismatch")
    if check.get("files") != files:
        raise RuntimeError("spec cache reconstruction mismatch")

    print(
        "Browser spec cache prepared "
        f"files={len(files)} sourceBytes={source_bytes} outputBytes={output.stat().st_size} "
        f"sha256={sha256_text(text)} output={output}"
    )
    print("Browser spec cache extensions " + " ".join(f"{k}={by_extension[k]}" for k in sorted(by_extension)))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())