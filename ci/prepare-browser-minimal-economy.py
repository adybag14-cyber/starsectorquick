from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ECON_DIRS = [
    ROOT / "starsector" / "starsector" / "data" / "campaign" / "econ",
    ROOT / "data" / "campaign" / "econ",
]

MINIMAL_ECONOMY = '''{
    "version":2.0,
    "initialStepsToRun":20,
    "maxExoticUtilityAtRange":14000,
    "defaultTariff":0.3,
    "starSystems":[
        "corvus.json",
    ],
    "map":"../starmap.json",
}
'''

MINIMAL_CORVUS = '''{
    "starSystem":"corvus",
    "markets":[
        {
            "entities":["asharu"],
            "faction":"independent",
            "size":4,
            "startingConditions":[
                "population_4",
                "hot",
                "ore_moderate",
                "farmland_poor",
                "ruins_scattered",
                "habitable",
            ],
            "industries":[
                "population",
                "farming",
                "spaceport",
            ],
        },
    ],
}
'''


def require_stock_inputs(econ_text: str, corvus_text: str, directory: Path) -> None:
    required_econ = [
        '"askonia.json"',
        '"corvus.json"',
        '"isirah.json"',
        '"initialStepsToRun":20',
    ]
    required_corvus = [
        '"entities":["asharu"]',
        '"entities":["jangala", "corvus_hegemony_station"]',
        '"entities":["corvus_IIIa", "corvus_pirate_station"]',
    ]
    missing = [token for token in required_econ if token not in econ_text]
    missing += [token for token in required_corvus if token not in corvus_text]
    if missing:
        raise RuntimeError(
            f"refusing to patch unexpected economy data in {directory}: missing {missing}"
        )


def refresh_index(directory: Path) -> None:
    index = directory / "index.list"
    if not index.is_file():
        return
    lines = []
    seen = set()
    for raw in index.read_text(encoding="utf-8-sig").splitlines():
        if not raw.strip():
            continue
        name = raw.split("\t", 1)[0]
        file_path = directory / name
        if name in {"economy.json", "corvus.json"}:
            if not file_path.is_file():
                raise RuntimeError(f"indexed economy file missing: {file_path}")
            raw = f"{name}\t{file_path.stat().st_size}"
            seen.add(name)
        lines.append(raw)
    if seen != {"economy.json", "corvus.json"}:
        raise RuntimeError(f"economy index missing expected entries in {index}: seen={sorted(seen)}")
    index.write_text("\n".join(lines) + "\n", encoding="utf-8")


def patch(directory: Path) -> None:
    if not directory.is_dir():
        return
    economy = directory / "economy.json"
    corvus = directory / "corvus.json"
    if not economy.is_file() or not corvus.is_file():
        raise RuntimeError(f"economy directory incomplete: {directory}")
    econ_text = economy.read_text(encoding="utf-8-sig")
    corvus_text = corvus.read_text(encoding="utf-8-sig")
    require_stock_inputs(econ_text, corvus_text, directory)
    economy.write_text(MINIMAL_ECONOMY, encoding="utf-8")
    corvus.write_text(MINIMAL_CORVUS, encoding="utf-8")
    refresh_index(directory)
    print(
        "BrowserEconomyDiag: prepared stock Asharu-only economy "
        f"root={directory.relative_to(ROOT)} "
        f"economyBytes={economy.stat().st_size} corvusBytes={corvus.stat().st_size}"
    )


patched = 0
for directory in ECON_DIRS:
    if directory.is_dir():
        patch(directory)
        patched += 1
if patched == 0:
    raise RuntimeError("no campaign economy directories found")
print(f"BrowserEconomyDiag: patchedRoots={patched}")
