#!/usr/bin/env python3
"""Build a non-destructive overlay for the legacy gh-pages runtime tree.

The campaign preparation step intentionally mutates only the files required by
CheerpJ. The deployed gh-pages branch also contains large historical runtime
assets that are not present in the source branch, so publishing must overlay
candidate files instead of mirroring/deleting the branch.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import shutil
import subprocess
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GENERATED_OVERLAY_ROOT = (ROOT / "test_output" / "pages-overlays").resolve()
OVERLAY_MARKER = ".starsectorquick-pages-overlay"

REQUIRED_FILES = {
    ".gitattributes",
    "index.html",
    "launch.html",
    "starsectorquick-sw.js",
    "build/final/wasm-modules/lwjgl.js",
    "jars/Fixer.java",
    "jars/fixer_patch.jar",
    "jars/fs.common_obf.jar",
    "jars/bridge.jar",
    "jars/index.list",
    "jars/scripts-precompiled.jar",
    "jars/starfarer.api.jar",
    "jars/starfarer_obf.jar",
    "jars/txw2-2.3.1.jar",
    "data/campaign/econ/economy.json",
    "data/campaign/econ/corvus.json",
    "data/scripts/world/SectorGen.java",
    "starsector/starsector/data/campaign/econ/economy.json",
    "starsector/starsector/data/campaign/econ/corvus.json",
    "starsector/starsector/data/browser-spec-cache-v1.json",
    "starsector/starsector/data/scripts/world/SectorGen.java",
}

FULL_ECONOMY_SYSTEMS = (
    "askonia.json",
    "aztlan.json",
    "samarra.json",
    "corvus.json",
    "valhalla.json",
    "arcadia.json",
    "magec.json",
    "eos.json",
    "hybrasil.json",
    "yma.json",
    "tyle.json",
    "naraka.json",
    "canaan.json",
    "mayasura.json",
    "westernesse.json",
    "zagan.json",
    "thule.json",
    "algebbar.json",
    "kumarikandam.json",
    "isirah.json",
)


def git_paths(*args: str) -> set[str]:
    raw = subprocess.check_output(["git", *args], cwd=ROOT)
    return {p.decode("utf-8") for p in raw.split(b"\0") if p}


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def copy_file(rel: str, output: Path) -> dict[str, object]:
    if rel.startswith("/") or ".." in Path(rel).parts:
        raise RuntimeError(f"unsafe overlay path: {rel}")
    src = ROOT / rel
    if not src.is_file():
        raise RuntimeError(f"overlay source missing: {src}")
    if src.stat().st_size == 0:
        raise RuntimeError(f"refusing to publish empty runtime file: {rel}")
    dst = output / rel
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src, dst)
    return {"path": rel, "bytes": src.stat().st_size, "sha256": sha256(src)}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", default="test_output/pages-overlays/deploy")
    parser.add_argument("--profile", default="full-stock-campaign")
    parser.add_argument("--source-sha", default="")
    args = parser.parse_args()

    requested_output = Path(args.output)
    if requested_output.is_absolute():
        raise RuntimeError("overlay output must be repository-relative")
    output = (ROOT / requested_output).resolve()
    try:
        output.relative_to(GENERATED_OVERLAY_ROOT)
    except ValueError as exc:
        raise RuntimeError(
            f"overlay output must be under {GENERATED_OVERLAY_ROOT.relative_to(ROOT)}: {args.output}"
        ) from exc
    if output == GENERATED_OVERLAY_ROOT:
        raise RuntimeError("overlay output must be a child of the generated overlay root")
    if output.exists():
        if not output.is_dir():
            raise RuntimeError(f"overlay output exists and is not a directory: {output}")
        marker = output / OVERLAY_MARKER
        if not marker.is_file():
            raise RuntimeError(
                f"refusing to remove unmarked existing overlay directory: {output}"
            )
        shutil.rmtree(output)
    output.mkdir(parents=True)
    (output / OVERLAY_MARKER).write_text(
        "starsectorquick-pages-overlay-v1\n", encoding="utf-8"
    )

    source_sha = args.source_sha.strip() or subprocess.check_output(
        ["git", "rev-parse", "HEAD"], cwd=ROOT, text=True
    ).strip()

    # Keep accepting the old profile argument while workflows transition, but
    # record what is actually shipped. A Corvus/Asharu-only profile is no longer
    # a valid public runtime.
    profile_name = args.profile
    if profile_name == "corvus-asharu-compat":
        profile_name = "full-stock-campaign"

    # Preparation-time changes include restored official graphics and patched
    # loose Java fallbacks. Copy only runtime/data paths plus the complete stock
    # economy dataset so an old gh-pages tree cannot retain a minimal economy.
    changed = git_paths("diff", "--name-only", "-z", "HEAD", "--")
    untracked = git_paths("ls-files", "--others", "--exclude-standard", "-z")
    selected = set(REQUIRED_FILES)
    for rel in changed | untracked:
        if rel.startswith("data/") or rel.startswith("starsector/starsector/"):
            selected.add(rel)

    for econ_base in (
        ROOT / "data" / "campaign" / "econ",
        ROOT / "starsector" / "starsector" / "data" / "campaign" / "econ",
    ):
        if not econ_base.is_dir():
            raise RuntimeError(f"full economy directory missing: {econ_base}")
        for econ_file in econ_base.glob("*.json"):
            selected.add(econ_file.relative_to(ROOT).as_posix())

    # The source branch already stores sanitized manifests, so they may not
    # appear in git diff even though the old gh-pages branch still has BOMs.
    for base in (ROOT / "data", ROOT / "starsector" / "starsector"):
        if base.is_dir():
            for index in base.rglob("index.list"):
                selected.add(index.relative_to(ROOT).as_posix())

    launch = (ROOT / "launch.html").read_text(encoding="utf-8")
    if "20260810-campaign-render-v6" not in launch:
        raise RuntimeError("launch cache/reset version was not updated")
    if "window.__STARSECTOR_AUTO_CAMPAIGN_SECTOR_SIZE__ || 'normal'" not in launch:
        raise RuntimeError("public launcher is not using normal sector size")
    if "window.__STARSECTOR_AUTO_CAMPAIGN_STARTING_LOCATION__ || 'Galatia'" not in launch:
        raise RuntimeError("public launcher is not starting in Galatia")

    lwjgl_js = (
        ROOT / "build" / "final" / "wasm-modules" / "lwjgl.js"
    ).read_text(encoding="utf-8")
    if "preserveDrawingBuffer: false" not in lwjgl_js:
        raise RuntimeError("LWJGL candidate is missing production preserveDrawingBuffer=false")
    if 'powerPreference: "high-performance"' not in lwjgl_js:
        raise RuntimeError("LWJGL candidate is missing high-performance WebGL context preference")

    for econ_rel in (
        "data/campaign/econ/economy.json",
        "starsector/starsector/data/campaign/econ/economy.json",
    ):
        econ = (ROOT / econ_rel).read_text(encoding="utf-8-sig")
        missing = [name for name in FULL_ECONOMY_SYSTEMS if f'"{name}"' not in econ]
        if missing:
            raise RuntimeError(f"incomplete full-stock economy manifest {econ_rel}: missing={missing}")

    for corvus_rel in (
        "data/campaign/econ/corvus.json",
        "starsector/starsector/data/campaign/econ/corvus.json",
    ):
        corvus = (ROOT / corvus_rel).read_text(encoding="utf-8-sig")
        required_entities = ("asharu", "jangala", "corvus_hegemony_station", "corvus_IIIa", "corvus_pirate_station")
        missing = [entity for entity in required_entities if f'"{entity}"' not in corvus]
        if missing:
            raise RuntimeError(f"incomplete full-stock Corvus market data {corvus_rel}: missing={missing}")

    entries = [copy_file(rel, output) for rel in sorted(selected)]
    (output / ".nojekyll").touch()
    entries.insert(0, {"path": ".nojekyll", "bytes": 0, "sha256": sha256(output / ".nojekyll")})
    profile = {
        "profile": profile_name,
        "sourceSha": source_sha,
        "workflowRunId": os.environ.get("GITHUB_RUN_ID", "local"),
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "overlayFiles": len(entries),
    }
    (output / "web-runtime-profile.json").write_text(
        json.dumps(profile, indent=2) + "\n", encoding="utf-8"
    )
    (output / "web-runtime-manifest.json").write_text(
        json.dumps({"profile": profile, "files": entries}, indent=2) + "\n",
        encoding="utf-8",
    )
    print(
        f"Pages overlay prepared profile={profile_name} source={source_sha} "
        f"files={len(entries)} bytes={sum(int(e['bytes']) for e in entries)}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
