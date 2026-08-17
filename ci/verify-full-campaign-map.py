#!/usr/bin/env python3
"""Fail CI unless the browser produced a real, populated full Starsector campaign."""

from __future__ import annotations

import argparse
import re
from pathlib import Path

CORE_SYSTEMS = (
    "Galatia", "Askonia", "Eos", "Valhalla", "Arcadia", "Magec", "Corvus",
    "Aztlan", "Samarra", "Penelope", "Yma", "Hybrasil", "Duzahk", "TiaTaxet",
    "Canaan", "AlGebbar", "Isirah", "KumariKandam", "Naraka", "Thule",
    "Mayasura", "Zagan", "Westernesse", "Tyle",
)

WORLD_READY_RE = re.compile(
    r"Fixer: auto campaign world-ready systems=(\d+) planets=(\d+) "
    r"markets=(\d+) factions=(\d+) playerFleet=(true|false) playerLocation=(true|false)"
)
DATA_RE = re.compile(
    r"Fixer: auto campaign data prepared .*?startLocation=([^\s]+) "
    r"sectorSize=([^\s]+)"
)


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(f"full-campaign-map verification failed: {message}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("log", type=Path)
    parser.add_argument("--expected-sector-size", required=True)
    parser.add_argument("--expected-start-location", required=True)
    parser.add_argument("--require-tutorial", action="store_true")
    parser.add_argument("--min-systems", type=int, default=100)
    parser.add_argument("--min-planets", type=int, default=300)
    parser.add_argument("--min-markets", type=int, default=40)
    parser.add_argument("--min-factions", type=int, default=15)
    args = parser.parse_args()

    text = args.log.read_text(encoding="utf-8", errors="replace")

    missing = [
        system for system in CORE_SYSTEMS
        if f"BrowserSectorGenStep: end {system} " not in text
    ]
    require(not missing, "missing completed core systems: " + ", ".join(missing))
    require(
        "Fixer: full campaign map enabled; delegating to stock outer-sector procedural generation."
        in text,
        "stock outer-sector procedural generation was not started",
    )
    require(
        "Fixer: full campaign outer-sector procedural generation complete." in text,
        "stock outer-sector procedural generation did not complete",
    )
    require(
        "Fixer: explicit diagnostic override is skipping outer-sector procedural generation."
        not in text,
        "partial-map diagnostic override was active",
    )

    data_matches = DATA_RE.findall(text)
    require(data_matches, "campaign preparation settings were not logged")
    start_location, sector_size = data_matches[-1]
    require(
        sector_size.lower() == args.expected_sector_size.lower(),
        f"sector size {sector_size!r} != {args.expected_sector_size!r}",
    )
    require(
        start_location.lower() == args.expected_start_location.lower(),
        f"start location {start_location!r} != {args.expected_start_location!r}",
    )

    world_matches = WORLD_READY_RE.findall(text)
    require(world_matches, "no completed world-ready population record")
    systems, planets, markets, factions, player_fleet, player_location = world_matches[-1]
    systems_i, planets_i, markets_i, factions_i = map(
        int, (systems, planets, markets, factions)
    )
    require(systems_i >= args.min_systems, f"systems={systems_i} < {args.min_systems}")
    require(planets_i >= args.min_planets, f"planets={planets_i} < {args.min_planets}")
    require(markets_i >= args.min_markets, f"markets={markets_i} < {args.min_markets}")
    require(factions_i >= args.min_factions, f"factions={factions_i} < {args.min_factions}")
    require(player_fleet == "true", "player fleet is missing")
    require(player_location == "true", "player location is missing")

    if args.require_tutorial:
        require(
            "BrowserTutorialCompat: started stock Galatia tutorial" in text,
            "stock Galatia tutorial did not start",
        )
        require(
            "Fixer: browser tutorial active; preserving tutorial-managed ability slots." in text,
            "mature quick-start ability repair overwrote tutorial progression",
        )

    print(
        "Full campaign map verified "
        f"coreSystems={len(CORE_SYSTEMS)} systems={systems_i} planets={planets_i} "
        f"markets={markets_i} factions={factions_i} sectorSize={sector_size} "
        f"startLocation={start_location} tutorial={args.require_tutorial}"
    )


if __name__ == "__main__":
    main()
