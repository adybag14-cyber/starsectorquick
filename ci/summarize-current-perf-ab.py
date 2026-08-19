#!/usr/bin/env python3
from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path("test_output/current-perf-ab")
ORDER = ["baseline-a", "fractal", "fast-id", "pathtracker", "baseline-b"]


def parse_variant(name: str) -> dict:
    directory = ROOT / name
    result_path = directory / "result.json"
    log_path = directory / "browser.log"
    row = {"name": name, "result": result_path.exists(), "log": log_path.exists()}
    if result_path.exists():
        data = json.loads(result_path.read_text(encoding="utf-8"))
        row.update(
            ok=data.get("ok"),
            title_ms=data.get("timeToTitleMs"),
            campaign_ms=data.get("timeToCampaignMs"),
            playable_ms=data.get("timeToFirstPlayableFrameMs"),
            fatal=data.get("fatalSeenAt"),
            errors=len(data.get("errors") or []),
        )
    if log_path.exists():
        text = log_path.read_text(encoding="utf-8", errors="ignore")
        starts = [float(x) for x in re.findall(r"\[boot-timing\] phase=direct-new-game-start ms=([0-9.]+)", text)]
        ready = [float(x) for x in re.findall(r"\[boot-timing\] phase=direct-new-game-ready ms=([0-9.]+)", text)]
        if starts and ready:
            row["direct_new_game_ms"] = round(ready[-1] - starts[-1], 1)
        worlds = re.findall(r"world-ready systems=(\d+) planets=(\d+) markets=(\d+) factions=(\d+)", text)
        if worlds:
            row["world"] = tuple(int(x) for x in worlds[-1])
        seeds = re.findall(r"seedString=(SEK\d+)", text)
        if seeds:
            row["seed"] = seeds[-1]
        row["fast_id_marker"] = "BrowserXStreamFastId: enabled" in text
        row["pathtracker_marker"] = "BrowserFastPathTracker: enabled exact indexed path cache" in text
    return row


def read_status() -> dict[str, dict]:
    status = {}
    path = ROOT / "status.tsv"
    if not path.exists():
        return status
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        name, ref, rc, verify_rc = line.split("\t")
        status[name] = {"ref": ref, "rc": int(rc), "verify_rc": int(verify_rc)}
    return status


def drift_expected(a: float | int | None, b: float | int | None, position: int) -> float | None:
    if a is None or b is None:
        return None
    return float(a) + (float(b) - float(a)) * (position / 4.0)


def main() -> int:
    status = read_status()
    rows = [parse_variant(name) for name in ORDER]
    for row in rows:
        row.update(status.get(row["name"], {}))

    by_name = {row["name"]: row for row in rows}
    a, b = by_name["baseline-a"], by_name["baseline-b"]
    metrics = ["title_ms", "campaign_ms", "direct_new_game_ms"]
    for position, name in enumerate(ORDER):
        row = by_name[name]
        if name.startswith("baseline"):
            continue
        row["drift_adjusted_delta_ms"] = {}
        for metric in metrics:
            expected = drift_expected(a.get(metric), b.get(metric), position)
            value = row.get(metric)
            row["drift_adjusted_delta_ms"][metric] = None if expected is None or value is None else round(float(value) - expected, 1)

    ROOT.mkdir(parents=True, exist_ok=True)
    (ROOT / "summary.json").write_text(json.dumps(rows, indent=2) + "\n", encoding="utf-8")

    lines = [
        "# Current same-runner performance A/B",
        "",
        "All variants use fixed seed `SEK968276040`; baseline is sampled before and after candidates.",
        "",
        "| variant | rc/verify | title ms | campaign ms | direct New Game ms | world | Δ direct vs drift baseline |",
        "|---|---:|---:|---:|---:|---|---:|",
    ]
    for row in rows:
        world = row.get("world")
        world_text = "-" if not world else "/".join(str(x) for x in world)
        delta = (row.get("drift_adjusted_delta_ms") or {}).get("direct_new_game_ms")
        delta_text = "-" if delta is None else f"{delta:+.1f}"
        lines.append(
            f"| {row['name']} | {row.get('rc', '-')} / {row.get('verify_rc', '-')} | "
            f"{row.get('title_ms', '-')} | {row.get('campaign_ms', '-')} | "
            f"{row.get('direct_new_game_ms', '-')} | {world_text} | {delta_text} |"
        )
    lines += ["", "Marker checks:"]
    for row in rows:
        lines.append(
            f"- {row['name']}: fast-id={row.get('fast_id_marker', False)}, "
            f"pathtracker={row.get('pathtracker_marker', False)}, seed={row.get('seed', '-')}, ok={row.get('ok', '-')}"
        )
    (ROOT / "summary.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print("\n".join(lines))

    # Baselines must succeed and be the exact intended world. Candidates may fail;
    # their failure is evidence and should not prevent the remaining variants from running.
    for name in ("baseline-a", "baseline-b"):
        row = by_name[name]
        if row.get("rc") != 0 or row.get("verify_rc") != 0 or row.get("ok") is not True:
            raise SystemExit(f"baseline failed: {name}: {row}")
        if row.get("seed") != "SEK968276040" or row.get("world") != (218, 917, 59, 21):
            raise SystemExit(f"baseline world mismatch: {name}: {row}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
